package com.example.transit;

import io.github.cdimascio.dotenv.Dotenv;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Test class for the data ingestion service.
 */
public class DataIngestionTest {

    public static void main(String[] args) {
        System.out.println("=== Data Ingestion Service Test ===");

        // Load environment variables from .env file
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        // Retrieve configuration values
        String apiToken = dotenv.get("API_TOKEN");
        String baseUrl = dotenv.get("GTFS_BASE_URL");
        String agency = dotenv.get("GTFS_AGENCY");

        // Validate configuration
        if (apiToken == null || baseUrl == null || agency == null) {
            System.err.println("Missing configuration. Please check your .env file.");
            System.err.println("Required variables: API_TOKEN, GTFS_BASE_URL, GTFS_AGENCY");
            return;
        }

        // Construct the full feed URL
        String feedUrl = String.format("%s?api_key=%s&agency=%s", baseUrl, apiToken, agency);

        // Create a reference to hold the ingestion service
        final DataIngestionService[] serviceRef = new DataIngestionService[1];

        // Register shutdown hook with the reference array
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutdown hook triggered, cleaning up resources...");
            if (serviceRef[0] != null) {
                serviceRef[0].stop();
            }
            IgniteConnection.close();
        }));

        try {
            // Create and start the schema
            System.out.println("\n--- Setting up database schema ---");
            SchemaSetup schemaSetup = new SchemaSetup();
            boolean schemaCreated = schemaSetup.createSchema();

            if (!schemaCreated) {
                System.err.println("Failed to create schema. Aborting test.");
                return;
            }

            // Verify initial state (should be empty or contain previous test data)
            System.out.println("\n--- Initial data state ---");
            DataVerifier.verifyData();

            // Create and start the data ingestion service
            System.out.println("\n--- Starting data ingestion service ---");
            DataIngestionService ingestService = new DataIngestionService(feedUrl)
                    .withBatchSize(100); // Configure batch size

            // Store the service in our reference array for the shutdown hook
            serviceRef[0] = ingestService;

            ingestService.start(30); // Fetch every 30 seconds

            // Wait for some data to be ingested
            System.out.println("\nWaiting for data ingestion (45 seconds)...");
            System.out.println(); // Add a blank line for separation

            // Create a volatile boolean for thread signaling
            final boolean[] keepSpinning = {true};

            // Spinning characters and counter
            String[] spinnerChars = new String[]{"⠋","⠙","⠹","⠸","⠼","⠴","⠦","⠧","⠇","⠏"};
            int[] seconds = {0}; // Using array to make it accessible inside the interceptor
            int[] spinPosition = {0}; // Track spinner position separately

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
            originalOut.println("Data ingestion wait complete!");

            // Restore original System.out
            System.setOut(originalOut);

            // Print ingestion statistics
            ingestService.printStatistics();

            // Verify data after ingestion
            System.out.println("\n--- Data state after ingestion ---");
            DataVerifier.verifyData();

            // Stop the ingestion service
            System.out.println("\n--- Stopping data ingestion service ---");
            ingestService.stop();

            // Give threads time to clean up
            System.out.println("Waiting for all threads to terminate...");
            Thread.sleep(1000);

            System.out.println("\nTest completed successfully!");

        } catch (Exception e) {
            System.err.println("Error during test: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Make sure ingestion service is stopped if it exists
            if (serviceRef[0] != null) {
                serviceRef[0].stop();
            }

            // Clean up connection
            IgniteConnection.close();
        }
    }
}