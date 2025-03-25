package com.example.transit.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for configuring logging settings.
 * Used to control log output during application execution.
 */
public class LoggingUtil {

    /**
     * Sets the logging level for root logger and specific loggers.
     * Configures both SLF4J/Logback loggers and Java Util Logging (JUL) loggers used by Ignite.
     *
     * @param level String representation of the log level ("OFF", "ERROR", "WARN", "INFO", "DEBUG", "TRACE")
     * @throws IllegalArgumentException if an invalid log level is provided
     */
    public static void setLogs(String level) {
        // Convert string to Level enum
        Level logLevel;
        try {
            logLevel = Level.valueOf(level);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid log level: " + level +
                    ". Valid options are: OFF, ERROR, WARN, INFO, DEBUG, TRACE");
        }

        // Get the Logback root logger and set it to the specified level
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(logLevel);

        // Specifically set Netty logger (used by Ignite for networking)
        Logger nettyLogger = (Logger) LoggerFactory.getLogger("io.netty");
        nettyLogger.setLevel(logLevel);

        // Ignite logger
        Logger igniteLogger = (Logger) LoggerFactory.getLogger("org.apache.ignite");
        igniteLogger.setLevel(logLevel);

        // Set specific Ignite internal logger
        Logger igniteInternalLogger = (Logger) LoggerFactory.getLogger("org.apache.ignite.internal");
        igniteInternalLogger.setLevel(logLevel);

        // Set specific Ignite logger impl
        Logger igniteLoggerImpl = (Logger) LoggerFactory.getLogger("org.apache.ignite.internal.logger");
        igniteLoggerImpl.setLevel(logLevel);

        // Configure Java Util Logging (JUL) loggers used by some Ignite components
        java.util.logging.Level julLevel = convertToJulLevel(level);

        // Set JUL root logger
        java.util.logging.Logger.getLogger("").setLevel(julLevel);

        // Set specific JUL loggers for Ignite
        java.util.logging.Logger.getLogger("org.apache.ignite").setLevel(julLevel);
        java.util.logging.Logger.getLogger("org.apache.ignite.internal").setLevel(julLevel);
        java.util.logging.Logger.getLogger("org.apache.ignite.internal.logger").setLevel(julLevel);
    }

    /**
     * Converts a Logback log level string to Java Util Logging Level.
     *
     * @param level String representation of the log level
     * @return Corresponding JUL Level
     */
    private static java.util.logging.Level convertToJulLevel(String level) {
        switch (level.toUpperCase()) {
            case "OFF": return java.util.logging.Level.OFF;
            case "ERROR": return java.util.logging.Level.SEVERE;
            case "WARN": return java.util.logging.Level.WARNING;
            case "INFO": return java.util.logging.Level.INFO;
            case "DEBUG": return java.util.logging.Level.FINE;
            case "TRACE": return java.util.logging.Level.FINEST;
            default:
                return java.util.logging.Level.INFO; // Default fallback
        }
    }
}