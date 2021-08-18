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

import com.google.common.base.Strings;
import de.rwth.idsg.steve.NotificationFeature;
import de.rwth.idsg.steve.SteveException;
import de.rwth.idsg.steve.repository.UserRepository;
import de.rwth.idsg.steve.repository.TransactionRepository;
import de.rwth.idsg.steve.repository.dto.Transaction;
import de.rwth.idsg.steve.repository.dto.User;
import de.rwth.idsg.steve.repository.dto.InsertTransactionParams;
import de.rwth.idsg.steve.repository.dto.MailSettings;
import de.rwth.idsg.steve.repository.dto.SmsSettings;
import de.rwth.idsg.steve.repository.dto.UpdateTransactionParams;
import lombok.extern.slf4j.Slf4j;
import ocpp.cs._2015._10.RegistrationStatus;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static de.rwth.idsg.steve.NotificationFeature.OcppStationBooted;
import static de.rwth.idsg.steve.NotificationFeature.OcppStationStatusFailure;
import static de.rwth.idsg.steve.NotificationFeature.OcppStationWebSocketConnected;
import static de.rwth.idsg.steve.NotificationFeature.OcppStationWebSocketDisconnected;
import static de.rwth.idsg.steve.NotificationFeature.OcppTransactionStarted;
import static de.rwth.idsg.steve.NotificationFeature.OcppTransactionEnded;
import static java.lang.String.format;

/**
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @since 22.01.2016
 */
@Slf4j
@Service
public class NotificationService {

    @Autowired private MailService mailService;
    @Autowired private SmsService smsService;
    @Autowired private UserRepository userRepository;
    @Autowired private TransactionRepository transactionRepository;

    public void ocppStationBooted(String chargeBoxId, Optional<RegistrationStatus> status) {
        if (isDisabled(OcppStationBooted)) {
            return;
        }

        String subject = format("Received boot notification from '%s'", chargeBoxId);
        String body;
        if (status.isPresent()) {
            body = format("Charging station '%s' is in database and has registration status '%s'.", chargeBoxId, status.get().value());
        } else {
            body = format("Charging station '%s' is NOT in database", chargeBoxId);
        }

        mailService.sendAsync(subject, addTimestamp(body));
    }

    public void ocppStationWebSocketConnected(String chargeBoxId) {
        if (isDisabled(OcppStationWebSocketConnected)) {
            return;
        }

        String subject = format("Connected to JSON charging station '%s'", chargeBoxId);

        mailService.sendAsync(subject, addTimestamp(""));
    }

    public void ocppStationWebSocketDisconnected(String chargeBoxId) {
        if (isDisabled(OcppStationWebSocketDisconnected)) {
            return;
        }

        String subject = format("Disconnected from JSON charging station '%s'", chargeBoxId);

        mailService.sendAsync(subject, addTimestamp(""));
    }

    public void ocppStationStatusFailure(String chargeBoxId, int connectorId, String errorCode) {
        if (isDisabled(OcppStationStatusFailure)) {
            return;
        }

        String subject = format("Connector '%s' of charging station '%s' is FAULTED", connectorId, chargeBoxId);
        String body = format("Status Error Code: '%s'", errorCode);

        mailService.sendAsync(subject, addTimestamp(body));
    }

    public void ocppTransactionStarted(int transactionId, InsertTransactionParams params) {

        String subject = format("Ladevorgang '%s' wurde an Ladesaeule '%s' am Ladepunkt '%s' gestartet", transactionId, params.getChargeBoxId(), params.getConnectorId());

        try{

            //User zu idTag holen
            
            List<User.Overview> userlist = userRepository.getUserToIdTag(params.getIdTag());
            User.Overview user = userlist.get(0);

            //Mail senden, falls Mail bei Benutzer hinterlegt ist
            if(!Strings.isNullOrEmpty(user.getEmail())){
                String mailaddress = user.getEmail();
                mailService.sendAsync(subject, addTimestamp(createContent(params)), mailaddress);
            }

            //SMS senden, falls Telefonnummer bei Benutzer hinterlegt ist
            if(!Strings.isNullOrEmpty(user.getPhone())){
                String phonenumber = user.getPhone();
                smsService.sendAsync(subject, phonenumber);
            }


        }catch (Exception e){
            log.error("Beim Abrufen des Benutzers zum idTag ist ein Fehler aufgetreten.", e);
        }

        
        if (isDisabled(OcppTransactionStarted)) {
            return;
        }
        
        mailService.sendAsync(subject, addTimestamp(createContent(params)));
    }

    public void ocppTransactionEnded(UpdateTransactionParams params) {
       if (isDisabled(OcppTransactionEnded)) {
            return;
        }

        String subject = format("Transaction '%s' has ended on charging station '%s'", params.getTransactionId(), params.getChargeBoxId());

        mailService.sendAsync(subject, addTimestamp(createContent(params)));
    }

    public void ocppStationStatusSuspendedEV (String chargeBoxId, int connectorId, String errorCode){
        //Abfrage der Transaktion (TOP 1), bei der chargeBoxId, connectorId übereinstimmen UND stop_timestamp = NULL ist.
        List<Transaction> matchingTransactionList = transactionRepository.getDetailsSpecific(chargeBoxId, connectorId);
        
        if(!matchingTransactionList.isEmpty()){ //Wenn eine Transaktion gefunden wurde
            Transaction matchingTransaction = matchingTransactionList.get(0);
            //User-Infos für idTag aus Transaktion finden
            try{
                //User zu idTag holen
                List<User.Overview> userlist = userRepository.getUserToIdTag(matchingTransaction.getOcppIdTag());
                if(!userlist.isEmpty()){
                    User.Overview user = userlist.get(0);

                    String subject = format("Der Ladevorgang '%s' an Ladesaeule '%s' (Ladepunkt '%s') wurde durch das Auto pausiert", matchingTransaction.getId(), matchingTransaction.getChargeBoxId(), matchingTransaction.getConnectorId());
                    String body = "";

                    //Mail senden, falls Mail bei Benutzer hinterlegt ist
                    if(!Strings.isNullOrEmpty(user.getEmail())){
                        mailService.sendAsync(subject, addTimestamp(body), user.getEmail());

                    }

                    //SMS senden
                    if(!Strings.isNullOrEmpty(user.getPhone())){
                        smsService.sendAsync(subject, user.getPhone());
                    }
                }else{
                    log.info("SuspendedEV: Dem ID-Tag ist kein Benutzer zugeordnet.");
                }

            }catch (Exception e){
                log.error("SuspendedEV: Beim Abrufen des Benutzers zum idTag ist ein Fehler aufgetreten.",e);
            }
        }else{
            log.info("SuspendedEV: Es wurde keine gülitge Transaktion zum Status 'SuspendedEV' gefunden.");
        }
        
        
        
        
    }
    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------


    private static String createContent(InsertTransactionParams params) {
        StringBuilder sb = new StringBuilder("Details:").append(System.lineSeparator())
            .append("- chargeBoxId: ").append(params.getChargeBoxId()).append(System.lineSeparator())
            .append("- connectorId: ").append(params.getConnectorId()).append(System.lineSeparator())
            .append("- idTag: ").append(params.getIdTag()).append(System.lineSeparator())
            .append("- startTimestamp: ").append(params.getStartTimestamp()).append(System.lineSeparator())
            .append("- startMeterValue: ").append(params.getStartMeterValue());

        if (params.isSetReservationId()) {
            sb.append(System.lineSeparator()).append("- reservationId: ").append(params.getReservationId());
        }

        return sb.toString();
    }

    private static String createContent(UpdateTransactionParams params) {
        return new StringBuilder("Details:").append(System.lineSeparator())
            .append("- chargeBoxId: ").append(params.getChargeBoxId()).append(System.lineSeparator())
            .append("- transactionId: ").append(params.getTransactionId()).append(System.lineSeparator())
            .append("- stopTimestamp: ").append(params.getStopTimestamp()).append(System.lineSeparator())
            .append("- stopMeterValue: ").append(params.getStopMeterValue()).append(System.lineSeparator())
            .append("- stopReason: ").append(params.getStopReason())
            .toString();
    }


    private boolean isDisabled(NotificationFeature f) {
        MailSettings settings = mailService.getSettings();

        boolean isEnabled = settings.isEnabled()
                && settings.getEnabledFeatures().contains(f)
                && !settings.getRecipients().isEmpty();

        return !isEnabled;
    }

    private static String addTimestamp(String body) {
        String eventTs = "Timestamp of the event: " + DateTime.now().toString();
        String newLine = System.lineSeparator() + System.lineSeparator();

        if (Strings.isNullOrEmpty(body)) {
            return eventTs;
        } else {
            return body + newLine + "--" + newLine + eventTs;
        }
    }

}
