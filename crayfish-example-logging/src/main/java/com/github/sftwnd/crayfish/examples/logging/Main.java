package com.github.sftwnd.crayfish.examples.logging;

import ch.qos.logback.classic.LoggerContext;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.io.IOException;

public class Main {

    public static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws InterruptedException, IOException {


        Marker userMarker = MarkerFactory.getMarker("USEROPS");
        for (int i=0; i<10; i++) {
            try {
                MDC.put("user", String.valueOf(12345000+i));
                logger.info(userMarker, "User operation "+i);
            } finally {
                MDC.clear();
            }
        }
        Marker adminMarker = MarkerFactory.getMarker("ADMINOPS");
        try {
            MDC.put("admin", "root");
            logger.info(adminMarker, "Admin operation");
        } finally {
            MDC.clear();
        }
        logger.info("Simple log message");
        try {
            MDC.put("user", String.valueOf(9998004));
            logger.info(userMarker, "User operation N");
        } finally {
            MDC.clear();
        }

        ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
        if (loggerFactory instanceof LoggerContext) {
            LoggerContext context = (LoggerContext) loggerFactory;
            context.stop();
        }


    }

}
