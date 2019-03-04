package com.github.sftwnd.crayfish.examples.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.Map;

public class MdcLoggedMarker extends ClassicConverter {

    private static final String paymentKey = "PAY";
    private static final String messageKey = "MSG";
  //private static final Marker paymentMarker =  MarkerFactory.getMarker(paymentKey);
  //private static final Marker messageMarker =  MarkerFactory.getMarker(messageKey);

    @Override
    public String convert(ILoggingEvent event) {
        Map<String,String> mdc = event.getMDCPropertyMap();
        if (mdc != null) {
            if (mdc.containsKey(paymentKey)) {
                //event.getMarker().add(paymentMarker);
                return paymentKey;
            } else if (mdc.containsKey(messageKey)) {
                //event.getMarker().add(messageMarker);
                return messageKey;
            }
        }
        return "";
    }

}