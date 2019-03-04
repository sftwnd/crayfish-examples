package com.github.sftwnd.crayfish.examples.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public class MySampleConverter extends ClassicConverter {

    private static final Marker useropsMarker =  MarkerFactory.getMarker("USEROPS");
    private static final Marker adminopsMarker =  MarkerFactory.getMarker("ADMINOPS");

    @Override
    public String convert(ILoggingEvent event) {
        Marker marker = event.getMarker();
        if (useropsMarker.equals(marker)) {
            return new StringBuilder("[USR:").append(event.getMDCPropertyMap().get("user")).append("]").toString();
        } else if (adminopsMarker.equals(marker)) {
            return new StringBuilder("[ADM:").append(event.getMDCPropertyMap().get("admin")).append("]").toString();
        }
        else return "";
    }
}