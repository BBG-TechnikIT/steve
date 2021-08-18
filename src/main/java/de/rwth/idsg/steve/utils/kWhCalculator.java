package de.rwth.idsg.steve.utils;

/**
 * @author Daniel Christen
 * @since 22.07.2021
 */

 public class kWhCalculator{
     public kWhCalculator(){}

     public String convertWhToKwh (String value){
        if(value != null){
            if(value != "Charging"){
                Double newValue = Double.parseDouble(value);
                return Double.toString(newValue/1000);
            }else{
                return "Charging";
            }
            
        }else{
            return "";
        }
    }

    public String subStrings (String startValue, String stopValue){
        if(stopValue != null){
            Double start = Double.parseDouble(startValue);
            Double stop = Double.parseDouble(stopValue);
            return Double.toString(stop-start);
        }else{
            return "Charging";
        }
        
    }

    public String addStrings (String startValue, String stopValue){
        if(stopValue != null){
            Double start = Double.parseDouble(startValue);
            Double stop = Double.parseDouble(stopValue);
            return Double.toString(stop+start);
        }else{
            return "Charging";
        }
        
    }
 }