package com.example.transit.examples;

import com.example.transit.service.*;
import com.example.transit.util.LoggingUtil;
import org.apache.ignite.client.IgniteClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Example application that demonstrates fetching and analyzing
 * real-time vehicle positions from a transit agency and storing
 * them in an Apache Ignite database.
 */
public class GtfsFeedExample {

    public static void main(String[] args) {
        // Configure logging to suppress unnecessary output
        LoggingUtil.setLogs("OFF");

        System.out.println("=== GTFS Feed Example ===");

        // Load configuration
        ConfigService config = ConfigService.getInstance();
        if (!config.validateConfiguration()) {
            return;
        }

        System.out.println("+++ Using GTFS feed URL: " + config.getRedactedFeedUrl());

        // Initialize Ignite connection service
        try (ConnectService connectionService = new ConnectService()) {
            IgniteClient client = connectionService.getClient();
            ReportService reportingService = new ReportService(client);

            // Set up the schema for storing vehicle positions
            SchemaService schemaService = new SchemaService(connectionService);
            if (!schemaService.createSchema()) {
                System.err.println("Failed to set up database schema. Exiting.");
                return;
            }

            // Create the feed service
            GtfsService feedService = new GtfsService(config.getFeedUrl());

            try {
                // Fetch vehicle positions
                System.out.println("=== Fetching vehicle positions...");
                List<Map<String, Object>> positions = feedService.getVehiclePositions();

                System.out.println("+++ Fetched " + positions.size() + " vehicle positions from feed");

                if (positions.isEmpty()) {
                    System.out.println("Warning: No vehicle positions found in the feed.");
                    System.out.println(
                            "This could indicate an issue with the feed URL, API token, or the agency may not have active vehicles at this time.");
                    return;
                }

                System.out.println("=== Success!");

                // Print the first 5 positions as a sample
                System.out.println("\nSample data (first 5 vehicles):");
                positions.stream()
                        .limit(5)
                        .forEach(pos -> System.out.println(reportingService.formatVehicleData(pos)));

                // Calculate and display statistics
                reportingService.analyzeVehicleData(positions);

            } catch (IOException e) {
                System.err.println("Error testing GTFS feed: " + e.getMessage());
                System.err.println("Check your internet connection and API token.");
                e.printStackTrace();

            }
        } catch (Exception e) {
            System.err.println("Error initializing Ignite connection: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("=== GTFS Feed Example Completed");
    }

}