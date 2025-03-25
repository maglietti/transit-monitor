package com.example.transit.examples;

import com.example.transit.service.*;
import com.example.transit.util.LoggingUtil;
import org.apache.ignite.client.IgniteClient;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Example demonstrating the data ingestion pipeline from GTFS feed to Ignite.
 * This class shows how to:
 * 1. Set up the database schema if not already set up
 * 2. Start and configure the data ingestion service
 * 3. Verify ingested data
 */
public class IngestExample {

    public static void main(String[] args) {
        System.out.println("=== Data Ingestion Service Example ===");

        // Configure logging to suppress unnecessary output
        LoggingUtil.setLogs("OFF");

        // Load configuration
        ConfigService config = ConfigService.getInstance();
        if (!config.validateConfiguration()) {
            return;
        }

        // Create references to hold services
        final ConnectService[] connectionServiceRef = new ConnectService[1];
        final IngestService[] ingestServiceRef = new IngestService[1];

        // Register shutdown hook with the reference arrays
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutdown hook triggered, cleaning up resources...");
            if (ingestServiceRef[0] != null) {
                ingestServiceRef[0].stop();
            }
            if (connectionServiceRef[0] != null) {
                connectionServiceRef[0].close();
            }
        }));

        try {
            // Create Ignite connection
            System.out.println("\n--- Connecting to Ignite cluster");
            ConnectService connectionService = new ConnectService();
            connectionServiceRef[0] = connectionService;

            // Create reporting service
            ReportService reportService = new ReportService(connectionService.getClient());

            // Create and initialize the schema
            System.out.println("\n--- Setting up database schema");
            SchemaService schemaService = new SchemaService(connectionService);
            boolean schemaCreated = schemaService.createSchema();

            if (!schemaCreated) {
                System.err.println("Failed to create schema. Aborting example.");
                return;
            }

            // Verify initial state (should be empty or contain previous data)
            System.out.println("\n--- Initial data state");
            reportService.sampleVehicleData();

            // Create GTFS feed service and data ingestion service
            System.out.println("\n=== Starting data ingestion service");
            GtfsService feedService = new GtfsService(config.getFeedUrl());

            IngestService ingestService = new IngestService(
                    feedService, connectionService)
                    .withBatchSize(100); // Configure batch size

            // Store the service in our reference array for the shutdown hook
            ingestServiceRef[0] = ingestService;

            ingestService.start(15); // Fetch every 15 seconds

            reportService.displayIngestionStatus(ingestService.getStatistics());

            // Wait for some data to be ingested
            System.out.println("\n=== Waiting for data ingestion (45 seconds)...");
            System.out.println(); // Add a blank line for separation

            // Create a volatile boolean for thread signaling
            final boolean[] keepSpinning = { true };

            // Spinning characters and counter
            String[] spinnerChars = new String[] { "⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏" };
            int[] seconds = { 0 }; // Using array to make it accessible inside the interceptor
            int[] spinPosition = { 0 }; // Track spinner position separately

            // Original System.out to be restored later
            PrintStream originalOut = System.out;

            // Thread-safe mechanism to manage spinner updates
            Object lock = new Object();

            // Create our interceptor to manage output and the spinner
            PrintStream interceptor = new PrintStream(new ByteArrayOutputStream()) {
                @Override
                public void println(String x) {
                    synchronized (lock) {
                        // Clear the spinner line first
                        originalOut.print("\r\033[K");
                        // Print the actual output
                        originalOut.println(x);
                        // If spinner is still active, redraw it
                        if (keepSpinning[0]) {
                            String spinChar = spinnerChars[spinPosition[0] % spinnerChars.length];
                            originalOut.print(spinChar + " " + (seconds[0] + 1) + "s elapsed");
                            originalOut.flush();
                        }
                    }
                }

                @Override
                public void print(String s) {
                    // We need this override for consistent behavior
                    // but nothing special is needed - just accumulate
                    super.print(s);
                }
            };

            // Set our interceptor as the system out
            System.setOut(interceptor);

            // Define spinner update interval for 1 full revolution per second
            int spinnerUpdatesPerSecond = 10; // 10 characters = 1 revolution
            long spinnerUpdateDelay = 1000 / spinnerUpdatesPerSecond; // milliseconds
            long nextSecondTime = System.currentTimeMillis() + 1000; // when to increment the seconds counter

            // Run the spinner in the main thread using a timer
            while (seconds[0] < 45 && keepSpinning[0]) {
                synchronized (lock) {
                    String spinChar = spinnerChars[spinPosition[0] % spinnerChars.length];
                    originalOut.print("\r" + spinChar + " " + (seconds[0] + 1) + "s elapsed");
                    originalOut.flush();
                }

                try {
                    Thread.sleep(spinnerUpdateDelay);

                    // Increment spinner position
                    spinPosition[0]++;

                    // Check if a second has passed
                    long currentTime = System.currentTimeMillis();
                    if (currentTime >= nextSecondTime) {
                        seconds[0]++;
                        nextSecondTime = currentTime + 1000;
                    }
                } catch (InterruptedException e) {
                    keepSpinning[0] = false;
                    break;
                }
            }

            // Clean up the spinner line
            originalOut.print("\r\033[K");
            originalOut.println("--- Data ingestion wait complete!");

            // Restore original System.out
            System.setOut(originalOut);

            // Verify data after ingestion
            System.out.println("\n--- Data state after ingestion");
            reportService.displaySystemStatistics();

            // Stop the ingestion service
            System.out.println("\n=== Stopping data ingestion service");
            ingestService.stop();
            reportService.displayIngestionStatus(ingestService.getStatistics());

            // Give threads time to clean up
            System.out.println("Waiting for all threads to terminate...");
            Thread.sleep(1000);

            System.out.println("\n=== Example completed successfully!");

        } catch (Exception e) {
            System.err.println("Error during example: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Make sure ingestion service is stopped
            if (ingestServiceRef[0] != null) {
                ingestServiceRef[0].stop();
            }

            // Clean up connection
            if (connectionServiceRef[0] != null) {
                connectionServiceRef[0].close();
            }
        }
    }
}