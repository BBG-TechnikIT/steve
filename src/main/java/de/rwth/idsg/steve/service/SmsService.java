/*
 * SteVe - SteckdosenVerwaltung - https://github.com/RWTH-i5-IDSG/steve
 * Copyright (C) 2013-2021 RWTH Aachen University - Information Systems - Intelligent Distributed Systems Group (IDSG).
 * All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package de.rwth.idsg.steve.service;


import org.springframework.stereotype.Service;
import de.rwth.idsg.steve.repository.dto.SmsSettings;
import de.rwth.idsg.steve.repository.SettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.ScheduledExecutorService;
import java.io.InputStreamReader;


/**
 * @author Daniel Christen
 * @since 23.07.2021
 */
@Slf4j
@Service
public class SmsService {

    @Autowired private SettingsRepository settingsRepository;
    @Autowired private ScheduledExecutorService executorService;


    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();

    private SmsSettings settings;
    
    @PostConstruct
    public void loadSettingsFromDB() {
        writeLock.lock();
        try {
            settings = settingsRepository.getSmsSettings();
        } finally {
            writeLock.unlock();
        }
    }

    public SmsSettings getSettings(){
        readLock.lock();
        try {
            return this.settings;
        } finally {
            readLock.unlock();
        }
    }

    public void sendAsync(String body, String recipient){
        SmsSettings settings = getSettings(); //Einstellungen holen
        if(isEnabledSms()){
            String message = PrepareTextForUrl(body); //Textvorverarbeitung
            executorService.execute(() -> {
                try {
                        send(message, recipient);
                } catch (Exception e) {
                    log.error("Failed to send SMS", e);
                }
            });
        }
    }

    public void send(String body, String recipient) throws Exception{
        SmsSettings settings = getSettings(); //Settings holen

        //Variablen
        //String protocol = settings.getProtocol();
        String urlHost = settings.getProtocol() + "://" + settings.getHost(); 
        String urlStandbyHost = settings.getProtocol() + "://" + settings.getStandbyHost();
        String messageUri = "/api.php?text=" + body +"&to=" + recipient + "&username=" + settings.getUsername() + "&password=" + settings.getPassword() + "&mode=number";
        String gwCheckUri = "/check.php";
        
        //SMS Versand
        if(settings.getProtocol().equals(new String("http"))){
            //kein check.php über http möglich => kein checkGWStatus(), sondern nur Check auf erfolgreichen SMS-Versand
            if(checkSMS(getHttp(urlHost + messageUri))){
                log.info("SMS erfolgreich mit " + settings.getHost() + " versendet (http)");
            }else if(checkSMS(getHttp(urlStandbyHost + messageUri))){
                log.info("SMS erfolgreich mit " + settings.getStandbyHost() + " versendet (http)");
            }else{
                //Error-Log
                log.error("SMS konnte nicht versendet werden. --- HTTP --- " + recipient + " --- " + body);
            }
        }else if(settings.getProtocol().equals(new String("https"))){
            if(checkGWStatus(getHttps(urlHost + gwCheckUri))){
                checkSMS(getHttps(urlHost + messageUri));
                log.info("SMS erfolgreich mit " + settings.getHost() + " versendet (https)");
            }else if(checkGWStatus(getHttps(urlStandbyHost + gwCheckUri))){
                checkSMS(getHttps(urlStandbyHost + messageUri));
                log.info("SMS erfolgreich mit " + settings.getStandbyHost() + " versendet (https)");
            }else{
                //Error-Log 
                log.error("SMS konnte nicht versendet werden. --- HTTPS --- " + recipient + " --- " + body);
            }
        }else{
            //Error-Log
            log.error("ERROR: SMS-Versand - Protokoll-Fehler");
        }
       
    }

    //Http-Adresse abfragen
    private String getHttp(String urlToRead) throws Exception {
        StringBuilder result = new StringBuilder();
        URL url = new URL(urlToRead);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        
        //Daten der Webseite verarbeiten
        try (var reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
            for (String line; (line = reader.readLine()) != null; ) {
                result.append(line);
            }
        }
        return result.toString();
    }

    //Http-Adresse abfragen
    private String getHttps(String urlToRead) throws Exception {
        
        /* Start of Fix Certificate-Error*/ //https://stackoverflow.com/questions/13626965/how-to-ignore-pkix-path-building-failed-sun-security-provider-certpath-suncertp
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
            public void checkClientTrusted(X509Certificate[] certs, String authType) { }
            public void checkServerTrusted(X509Certificate[] certs, String authType) { }

        } };
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        // Create all-trusting host name verifier
        HostnameVerifier allHostsValid = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) { return true; }
        };
        // Install the all-trusting host verifier
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        /* End of the fix*/
        

        StringBuilder result = new StringBuilder();
        URL url = new URL(urlToRead);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("GET");


        //Daten der Webseite verarbeiten
        try (var reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
            for (String line; (line = reader.readLine()) != null; ) {
                result.append(line);
            }
        }
        return result.toString();
    }

    //Prüfen, ob Gateway-Status OK ist
    private boolean checkGWStatus(String stateString){
        int checkState = stateString.indexOf("SIGNAL_OK");
            if(checkState == -1){
                return false;
            }else{
                return true;
            }
    }

    //Prüfen, ob SMS erfolgreich versendet wurde
    private boolean checkSMS(String resultString){
        int checkState = resultString.indexOf("OK: ");
        if(checkState == -1){
            return false;
        }else{
            return true;
        }
    }
    
    //Leerzeichen / Sonderzeichen ersetzen
    private String PrepareTextForUrl(String body){
        //Ersetzen von Zeichen für URL
        body = body.replace("%", "%25");
        body = body.replace(" ", "%20");
        body = body.replace("=", "%3D");
        body = body.replace("(", "%28");
        body = body.replace(")", "%29");
        body = body.replace("<", "%3C");
        body = body.replace(">", "%3E");
        if (body.length() >= 160){
            body = body.substring(0, 159); //auf 160 Zeichen kürzen
        }
        return body;
    }

    //Check, ob SMS gesendet werden sollen
    private boolean isEnabledSms(){
        SmsSettings settings = getSettings(); //Settings holen
        boolean isEnabled = settings.isEnabled();
        return isEnabled;
    }
}
