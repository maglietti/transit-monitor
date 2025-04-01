package com.example.transit.examples;

import com.example.transit.config.ConfigService;
import com.example.transit.config.IgniteConnectionManager;
import com.example.transit.config.VehiclePositionTableManager;
import com.example.transit.model.VehiclePosition;
import com.example.transit.service.GtfsService;
import com.example.transit.service.ReportingService;
import org.apache.ignite.client.IgniteClient;

import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Example application for fetching and analyzing transit data.
 */
public class GtfsFeedExample {

    private static final Logger logger = LogManager.getLogger(GtfsFeedExample.class);

    public static void main(String[] args) {

        // Handle JUL logging
        java.util.logging.LogManager.getLogManager().reset();
        org.apache.logging.log4j.jul.LogManager.getLogManager();
        java.util.logging.Logger.getLogger("").setLevel(java.util.logging.Level.WARNING);

        System.out.println("=== GTFS Feed Example ===");

        // Load configuration
        ConfigService config = ConfigService.getInstance();
        if (config.validateConfiguration()) {
            return;
        }

        System.out.println("+++ Using GTFS feed URL: " + config.getRedactedFeedUrl());

        // Connect to Ignite cluster
        try (IgniteConnectionManager connectionManager = new IgniteConnectionManager()) {
            IgniteClient client = connectionManager.getClient();
            ReportingService reportingService = new ReportingService(client);

            // Set up the schema
            VehiclePositionTableManager tableManager = new VehiclePositionTableManager(connectionManager);
            if (!tableManager.createSchema()) {
                System.err.println("Failed to set up database schema. Exiting.");
                return;
            }

            // Create the feed service
            GtfsService feedService = new GtfsService(config.getFeedUrl());

            try {
                // Fetch vehicle positions
                System.out.println("=== Fetching vehicle positions...");
                List<VehiclePosition> positions = feedService.getVehiclePositions();

                System.out.println(">>> Fetched " + positions.size() + " vehicle positions from feed");

                if (positions.isEmpty()) {
                    System.out.println("Warning: No vehicle positions found in the feed.");
                    System.out.println("This could indicate an issue with the feed URL, API token, or the agency may not have active vehicles at this time.");
                    return;
                }

                // Print sample data (first 5 vehicles)
                System.out.println("\nSample data (first 5 vehicles):");
                positions.stream()
                        .limit(5)
                        .forEach(pos -> System.out.println(reportingService.formatVehicleData(pos)));

                // Analyze the data
                reportingService.analyzeVehicleData(positions);

            } catch (IOException e) {
                logger.error("Error testing GTFS feed: {}", e.getMessage());
                System.err.println("Check your internet connection and API token.");
            }
        } catch (Exception e) {
            logger.error("Error initializing Ignite connection: {}", e.getMessage());
        }

        System.out.println("=== GTFS Feed Example Completed");
    }
}