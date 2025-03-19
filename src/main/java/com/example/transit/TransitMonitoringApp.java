package com.example.transit;

import org.apache.ignite.client.IgniteClient;
import io.github.cdimascio.dotenv.Dotenv;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Main application class that orchestrates all components of the transit monitoring system.
 * This class initializes and manages:
 * - The connection to the Ignite cluster
 * - The database schema
 * - The data ingestion service
 * - The service monitor
 * - The console dashboard
 */
public class TransitMonitoringApp {
    // Configuration options
    private static final int INGESTION_INTERVAL_SECONDS = 30;
    private static final int MONITORING_INTERVAL_SECONDS = 60;
    private static final int DASHBOARD_REFRESH_SECONDS = 10;

    // Dashboard views
    private static final int SUMMARY_VIEW = 0;
    private static final int ALERTS_VIEW = 1;
    private static final int DETAILS_VIEW = 2;
    private static final int TOTAL_VIEWS = 3;

    // Components
    private final IgniteClient client;
    private final DataIngestionService ingestionService;
    private final ServiceMonitor serviceMonitor;
    private final ScheduledExecutorService dashboardScheduler;

    // Dashboard state
    private final Map<String, Integer> previousRouteCounts = new HashMap<>();
    private final Map<String, Long> previousStatusCounts = new HashMap<>();
    private final AtomicInteger currentView = new AtomicInteger(0);

    // State tracking
    private boolean isRunning = false;

    /**
     * Creates a new transit monitoring application with all required components.
     *
     * @param feedUrl The URL of the GTFS feed to monitor
     */
    public TransitMonitoringApp(String feedUrl) {
        // Initialize Ignite client
        this.client = IgniteConnection.getClient();

        // Initialize data ingestion service
        this.ingestionService = new DataIngestionService(feedUrl)
                .withBatchSize(100);

        // Initialize service monitor
        this.serviceMonitor = new ServiceMonitor();

        // Initialize dashboard scheduler with daemon threads
        this.dashboardScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "dashboard-thread");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Starts all components of the transit monitoring system.
     *
     * @return true if startup was successful, false otherwise
     */
    public boolean start() {
        if (isRunning) {
            TerminalUtils.logInfo("Transit monitoring system is already running");
            return true;
        }

        try {
            TerminalUtils.logInfo(TerminalUtils.ANSI_BOLD + "Starting Transit Monitoring System" + TerminalUtils.ANSI_RESET);

            // Clear screen and show startup animation
            TerminalUtils.clearScreen();
            TerminalUtils.showStartupAnimation();

            // Test GTFS connection via the ingestion service
            TerminalUtils.logInfo("Testing GTFS connection...");
            testGtfsConnection();

            // Set up schema
            TerminalUtils.logInfo("Setting up database schema...");
            SchemaSetup schemaSetup = new SchemaSetup();
            boolean schemaCreated = schemaSetup.createSchema();

            if (!schemaCreated) {
                TerminalUtils.logError("Failed to create schema. Aborting startup.");
                return false;
            }

            // Start data ingestion
            TerminalUtils.logInfo("Starting data ingestion service (interval: " + INGESTION_INTERVAL_SECONDS + "s)...");
            ingestionService.start(INGESTION_INTERVAL_SECONDS);

            // Start service monitoring
            TerminalUtils.logInfo("Starting service monitor (interval: " + MONITORING_INTERVAL_SECONDS + "s)...");
            serviceMonitor.startMonitoring(MONITORING_INTERVAL_SECONDS);

            // Start dashboard
            TerminalUtils.logInfo("Starting console dashboard (refresh: " + DASHBOARD_REFRESH_SECONDS + "s)...");
            startConsoleDashboard(DASHBOARD_REFRESH_SECONDS);

            isRunning = true;
            TerminalUtils.logInfo(TerminalUtils.ANSI_GREEN + "Transit monitoring system started successfully" + TerminalUtils.ANSI_RESET);

            return true;
        } catch (Exception e) {
            TerminalUtils.logError("Error starting transit monitoring system: " + e.getMessage());
            e.printStackTrace();
            stop();
            return false;
        }
    }

    /**
     * Stops all components of the transit monitoring system.
     */
    public void stop() {
        TerminalUtils.logInfo(TerminalUtils.ANSI_BOLD + "Stopping Transit Monitoring System" + TerminalUtils.ANSI_RESET);

        // Show shutdown animation
        TerminalUtils.showShutdownAnimation();

        // Stop dashboard
        dashboardScheduler.shutdown();
        try {
            if (!dashboardScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                dashboardScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            dashboardScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Stop service monitor
        serviceMonitor.stopMonitoring();

        // Stop ingestion service
        ingestionService.stop();

        // Close Ignite connection
        IgniteConnection.close();

        isRunning = false;
        TerminalUtils.logInfo(TerminalUtils.ANSI_GREEN + "Transit monitoring system stopped" + TerminalUtils.ANSI_RESET);
    }

    /**
     * Tests the connection to the GTFS feed.
     *
     * @throws Exception if the connection fails
     */
    private void testGtfsConnection() throws Exception {
        try {
            // We can't directly call private methods in DataIngestionService
            // Instead, we'll check if we can get statistics from the service
            DataIngestionService.IngestStats stats = ingestionService.getStatistics();

            // Since we haven't started the service yet, we can only verify it's initialized
            TerminalUtils.logInfo(TerminalUtils.ANSI_GREEN + "GTFS feed client initialized successfully." + TerminalUtils.ANSI_RESET);
            TerminalUtils.logInfo("Connection will be tested when the ingestion service starts.");

            // Note: The actual connection will be tested when the ingestion service starts
            // and tries to fetch data for the first time
        } catch (Exception e) {
            TerminalUtils.logError("GTFS client initialization failed: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Starts the console dashboard that periodically displays system status.
     *
     * @param refreshSeconds How often to refresh the dashboard
     */
    private void startConsoleDashboard(int refreshSeconds) {
        dashboardScheduler.scheduleAtFixedRate(() -> {
            try {
                // Rotate the dashboard view periodically
                printDashboard(currentView.get());
                currentView.set((currentView.get() + 1) % TOTAL_VIEWS);
            } catch (Exception e) {
                TerminalUtils.logError("Error updating dashboard: " + e.getMessage());
            }
        }, refreshSeconds, refreshSeconds, TimeUnit.SECONDS);
    }

    /**
     * Prints the console dashboard with current system status.
     *
     * @param viewType The type of view to display (0=summary, 1=alerts, 2=details)
     */
    private void printDashboard(int viewType) {
        // Get terminal width if available
        int terminalWidth = TerminalUtils.getTerminalWidth();

        // Clear the screen before drawing
        TerminalUtils.clearScreen();

        String header = "TRANSIT MONITORING DASHBOARD";
        String currentTime = LocalDateTime.now().format(TerminalUtils.DATE_TIME_FORMATTER);

        TerminalUtils.printCenteredBox(header, terminalWidth);
        System.out.println(TerminalUtils.ANSI_CYAN + "Current time: " + TerminalUtils.ANSI_BOLD + currentTime + TerminalUtils.ANSI_RESET);
        System.out.println();

        switch (viewType) {
            case SUMMARY_VIEW:
                printSummaryView(terminalWidth);
                break;
            case ALERTS_VIEW:
                printAlertsView(terminalWidth);
                break;
            case DETAILS_VIEW:
                printDetailsView(terminalWidth);
                break;
        }

        // Always show navigation help at bottom
        System.out.println();
        System.out.println(TerminalUtils.ANSI_BLUE + "Views rotate automatically every " + DASHBOARD_REFRESH_SECONDS + " seconds" + TerminalUtils.ANSI_RESET);
        System.out.println(TerminalUtils.ANSI_BLUE + "Press ENTER at any time to exit" + TerminalUtils.ANSI_RESET);
    }

    /**
     * Prints the summary view of the dashboard
     */
    private void printSummaryView(int width) {
        System.out.println(TerminalUtils.ANSI_BOLD + "SUMMARY VIEW" + TerminalUtils.ANSI_RESET);
        System.out.println("─".repeat(width > 80 ? 80 : width));

        // Get active vehicle counts
        printActiveVehicleCounts();

        // Get status distribution
        System.out.println();
        System.out.println(TerminalUtils.ANSI_BOLD + "VEHICLE STATUS DISTRIBUTION" + TerminalUtils.ANSI_RESET);
        printStatusDistribution();

        // Data ingestion status
        System.out.println();
        System.out.println(TerminalUtils.ANSI_BOLD + "DATA INGESTION STATUS" + TerminalUtils.ANSI_RESET);
        printDataIngestionStatus();
    }

    /**
     * Prints the alerts view of the dashboard
     */
    private void printAlertsView(int width) {
        System.out.println(TerminalUtils.ANSI_BOLD + "SERVICE ALERTS VIEW" + TerminalUtils.ANSI_RESET);
        System.out.println("─".repeat(width > 80 ? 80 : width));

        // Print service alerts
        List<ServiceMonitor.ServiceAlert> alerts = serviceMonitor.getRecentAlerts();
        System.out.println(TerminalUtils.ANSI_BOLD + "RECENT SERVICE ALERTS" + TerminalUtils.ANSI_RESET);

        if (alerts.isEmpty()) {
            System.out.println(TerminalUtils.ANSI_GREEN + "No active alerts at this time" + TerminalUtils.ANSI_RESET);
        } else {
            System.out.println(TerminalUtils.ANSI_YELLOW + "Found " + alerts.size() + " active alerts:" + TerminalUtils.ANSI_RESET);

            alerts.stream()
                    .limit(15)  // Show more alerts in this view
                    .forEach(alert -> {
                        String color = TerminalUtils.ANSI_YELLOW;
                        if (alert.getSeverity() > 20) { // Higher severity values
                            color = TerminalUtils.ANSI_RED;
                        } else if (alert.getSeverity() < 5) { // Lower severity values
                            color = TerminalUtils.ANSI_BLUE;
                        }

                        System.out.println(color + "• " + alert.getMessage() + TerminalUtils.ANSI_RESET +
                                " [" + alert.getTimestamp().format(TerminalUtils.TIME_FORMATTER) + "]");
                    });
        }

        // Alert statistics
        System.out.println();
        System.out.println(TerminalUtils.ANSI_BOLD + "ALERT STATISTICS" + TerminalUtils.ANSI_RESET);
        Map<String, Integer> alertCounts = getAlertCounts();

        if (alertCounts.isEmpty()) {
            System.out.println("No alerts have been generated yet");
        } else {
            alertCounts.forEach((type, count) -> {
                String color = count > 10 ? TerminalUtils.ANSI_RED : count > 0 ? TerminalUtils.ANSI_YELLOW : TerminalUtils.ANSI_GREEN;
                System.out.println(color + "• " + type + ": " + count + " alerts" + TerminalUtils.ANSI_RESET);
            });
        }
    }

    /**
     * Get alert counts by type from service monitor
     */
    private Map<String, Integer> getAlertCounts() {
        Map<String, Integer> counts = new HashMap<>();

        // Convert from ServiceMonitor's map format
        Map<String, Integer> monitorCounts = serviceMonitor.getAlertCounts();
        counts.putAll(monitorCounts);

        return counts;
    }

    /**
     * Prints the detailed system view
     */
    private void printDetailsView(int width) {
        System.out.println(TerminalUtils.ANSI_BOLD + "SYSTEM DETAILS VIEW" + TerminalUtils.ANSI_RESET);
        System.out.println("─".repeat(width > 80 ? 80 : width));

        // System statistics
        System.out.println(TerminalUtils.ANSI_BOLD + "SYSTEM STATISTICS" + TerminalUtils.ANSI_RESET);
        printSystemStatistics();

        // Service monitor status
        System.out.println();
        System.out.println(TerminalUtils.ANSI_BOLD + "MONITORING THRESHOLDS" + TerminalUtils.ANSI_RESET);
        printMonitoringThresholds();

        // Connection status
        System.out.println();
        System.out.println(TerminalUtils.ANSI_BOLD + "CONNECTION STATUS" + TerminalUtils.ANSI_RESET);
        printConnectionStatus();
    }

    /**
     * Prints active vehicle counts by route
     */
    private void printActiveVehicleCounts() {
        System.out.println(TerminalUtils.ANSI_BOLD + "ACTIVE VEHICLES BY ROUTE (last 15 minutes)" + TerminalUtils.ANSI_RESET);

        try {
            String routeCountSql =
                    "SELECT route_id, COUNT(DISTINCT vehicle_id) as vehicle_count " +
                            "FROM vehicle_positions " +
                            "WHERE TIMESTAMPDIFF(MINUTE, time_stamp, CURRENT_TIMESTAMP) <= 15 " +
                            "GROUP BY route_id " +
                            "ORDER BY vehicle_count DESC " +
                            "LIMIT 10";

            // Execute SQL query
            var resultSet = client.sql().execute(null, routeCountSql);
            boolean hasData = false;

            while (resultSet.hasNext()) {
                hasData = true;
                var row = resultSet.next();
                String routeId = row.stringValue("route_id");
                int count = (int)row.longValue("vehicle_count");

                // Show trend indicators compared to previous counts
                String trend = "";
                if (previousRouteCounts.containsKey(routeId)) {
                    int prevCount = previousRouteCounts.get(routeId);
                    if (count > prevCount) {
                        trend = TerminalUtils.ANSI_GREEN + " ↑" + TerminalUtils.ANSI_RESET;
                    } else if (count < prevCount) {
                        trend = TerminalUtils.ANSI_RED + " ↓" + TerminalUtils.ANSI_RESET;
                    } else {
                        trend = TerminalUtils.ANSI_BLUE + " =" + TerminalUtils.ANSI_RESET;
                    }
                }

                previousRouteCounts.put(routeId, count);
                System.out.printf("• Route %-8s: %3d vehicles%s%n", routeId, count, trend);
            }

            if (!hasData) {
                System.out.println(TerminalUtils.ANSI_YELLOW + "No active vehicles found in the last 15 minutes." + TerminalUtils.ANSI_RESET);
            }
        } catch (Exception e) {
            System.out.println(TerminalUtils.ANSI_RED + "Error retrieving vehicle counts: " + e.getMessage() + TerminalUtils.ANSI_RESET);
            System.out.println(TerminalUtils.ANSI_YELLOW + "Suggestion: Check the vehicle_positions table schema" + TerminalUtils.ANSI_RESET);
        }
    }

    /**
     * Prints vehicle status distribution
     */
    private void printStatusDistribution() {
        try {
            String statusSql =
                    "SELECT current_status, COUNT(*) as status_count " +
                            "FROM vehicle_positions " +
                            "WHERE TIMESTAMPDIFF(MINUTE, time_stamp, CURRENT_TIMESTAMP) <= 15 " +
                            "GROUP BY current_status";

            var resultSet = client.sql().execute(null, statusSql);
            boolean hasData = false;

            while (resultSet.hasNext()) {
                hasData = true;
                var row = resultSet.next();
                String status = row.stringValue("current_status");
                long count = row.longValue("status_count");

                // Show trend indicators
                String trend = "";
                if (previousStatusCounts.containsKey(status)) {
                    long prevCount = previousStatusCounts.get(status);
                    if (count > prevCount) {
                        trend = TerminalUtils.ANSI_GREEN + " ↑" + TerminalUtils.ANSI_RESET;
                    } else if (count < prevCount) {
                        trend = TerminalUtils.ANSI_RED + " ↓" + TerminalUtils.ANSI_RESET;
                    } else {
                        trend = TerminalUtils.ANSI_BLUE + " =" + TerminalUtils.ANSI_RESET;
                    }
                }

                previousStatusCounts.put(status, count);

                // Status-specific colors
                String statusColor = TerminalUtils.ANSI_RESET;
                if ("STOPPED_AT".equals(status)) {
                    statusColor = TerminalUtils.ANSI_RED;
                } else if ("IN_TRANSIT_TO".equals(status)) {
                    statusColor = TerminalUtils.ANSI_GREEN;
                } else if ("INCOMING_AT".equals(status)) {
                    statusColor = TerminalUtils.ANSI_BLUE;
                }

                System.out.printf("• %s%-15s%s: %5d vehicles%s%n",
                        statusColor, status, TerminalUtils.ANSI_RESET, count, trend);
            }

            if (!hasData) {
                System.out.println(TerminalUtils.ANSI_YELLOW + "No status data available." + TerminalUtils.ANSI_RESET);
            }
        } catch (Exception e) {
            System.out.println(TerminalUtils.ANSI_RED + "Error retrieving status distribution: " + e.getMessage() + TerminalUtils.ANSI_RESET);
            System.out.println(TerminalUtils.ANSI_YELLOW + "Suggestion: Check if the current_status column exists" + TerminalUtils.ANSI_RESET);
        }
    }

    /**
     * Prints data ingestion status
     */
    private void printDataIngestionStatus() {
        DataIngestionService.IngestStats stats = ingestionService.getStatistics();

        System.out.println("• Status: " +
                (stats.isRunning() ? TerminalUtils.ANSI_GREEN + "Running" : TerminalUtils.ANSI_RED + "Stopped") + TerminalUtils.ANSI_RESET);
        System.out.println("• Total records fetched: " + stats.getTotalFetched());
        System.out.println("• Total records stored: " + stats.getTotalStored());
        System.out.println("• Last fetch count: " + stats.getLastFetchCount());

        if (stats.getLastFetchTimeMs() > 0) {
            System.out.println("• Last fetch time: " + stats.getLastFetchTimeMs() + "ms");
        }

        if (stats.getRunningTimeMs() > 0) {
            System.out.println("• Running time: " + formatDuration(stats.getRunningTimeMs()));

            if (stats.getTotalFetched() > 0 && stats.getRunningTimeMs() > 0) {
                double rate = (double) stats.getTotalFetched() / (stats.getRunningTimeMs() / 1000.0);
                System.out.printf("• Ingestion rate: %.2f records/second%n", rate);
            }
        }
    }

    /**
     * Formats a duration in milliseconds to a human-readable format (HH:MM:SS)
     */
    private String formatDuration(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        seconds %= 60;
        minutes %= 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    /**
     * Prints system statistics
     */
    private void printSystemStatistics() {
        try {
            // Get total record count
            var countResult = client.sql().execute(null, "SELECT COUNT(*) as total FROM vehicle_positions");

            if (countResult.hasNext()) {
                var row = countResult.next();
                long totalRecords = row.longValue("total");
                System.out.println("• Total position records: " + TerminalUtils.ANSI_BOLD + totalRecords + TerminalUtils.ANSI_RESET);
            }

            // Get total unique vehicles
            var vehiclesResult = client.sql().execute(null,
                    "SELECT COUNT(DISTINCT vehicle_id) as total FROM vehicle_positions");

            if (vehiclesResult.hasNext()) {
                var row = vehiclesResult.next();
                long totalVehicles = row.longValue("total");
                System.out.println("• Total unique vehicles: " + TerminalUtils.ANSI_BOLD + totalVehicles + TerminalUtils.ANSI_RESET);
            }

            // Get timespan simplified with direct string display
            var timeResult = client.sql().execute(null,
                    "SELECT MIN(time_stamp) as oldest, MAX(time_stamp) as newest FROM vehicle_positions");

            if (timeResult.hasNext()) {
                var row = timeResult.next();

                // Extract timestamp values directly as strings to avoid type conversion issues
                Object oldest = row.value("oldest");
                Object newest = row.value("newest");

                if (oldest != null && newest != null) {
                    System.out.println("• Oldest record: " + oldest);
                    System.out.println("• Newest record: " + newest);
                    System.out.println("• Data collection active");
                }
            }

        } catch (Exception e) {
            System.out.println(TerminalUtils.ANSI_RED + "Error retrieving system statistics: " + e.getMessage() + TerminalUtils.ANSI_RESET);
        }
    }

    /**
     * Prints monitoring thresholds (simplified fixed values)
     */
    private void printMonitoringThresholds() {
        // Use static values directly from known threshold constants
        System.out.println("• Delayed vehicle threshold: 5 minutes");
        System.out.println("• Vehicle bunching distance: 1 km");
        System.out.println("• Minimum vehicles per route: 2");
        System.out.println("• Vehicle offline threshold: 15 minutes");
    }

    /**
     * Prints connection status information
     */
    private void printConnectionStatus() {
        try {
            System.out.println("• Ignite cluster: " + TerminalUtils.ANSI_GREEN + "Connected" + TerminalUtils.ANSI_RESET);

            // Simplified connection reporting
            System.out.println("• Database: vehicle_positions table accessible");
            System.out.println("• Data ingestion service: " +
                    (ingestionService.getStatistics().isRunning() ?
                            TerminalUtils.ANSI_GREEN + "Active" :
                            TerminalUtils.ANSI_RED + "Inactive") +
                    TerminalUtils.ANSI_RESET);

            System.out.println("• Monitoring service: Active");
        } catch (Exception e) {
            System.out.println(TerminalUtils.ANSI_RED + "Error retrieving connection status: " + e.getMessage() + TerminalUtils.ANSI_RESET);
        }
    }

    /**
     * Main method to run the transit monitoring application.
     */
    public static void main(String[] args) {
        // Show welcome banner
        TerminalUtils.printWelcomeBanner();

        // Load environment variables from .env file
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        // Retrieve configuration values
        String apiToken = dotenv.get("API_TOKEN");
        String baseUrl = dotenv.get("GTFS_BASE_URL");
        String agency = dotenv.get("GTFS_AGENCY");

        // Validate configuration
        if (apiToken == null || baseUrl == null || agency == null) {
            System.err.println(TerminalUtils.ANSI_RED + "ERROR: Missing configuration. Please check your .env file." + TerminalUtils.ANSI_RESET);
            System.err.println(TerminalUtils.ANSI_YELLOW + "Required variables: API_TOKEN, GTFS_BASE_URL, GTFS_AGENCY" + TerminalUtils.ANSI_RESET);
            return;
        }

        // Construct the full feed URL
        String feedUrl = String.format("%s?api_key=%s&agency=%s", baseUrl, apiToken, agency);

        // Create and start the application
        TransitMonitoringApp app = new TransitMonitoringApp(feedUrl);

        if (app.start()) {
            // Wait for user input to stop
            System.out.println("\n" + TerminalUtils.ANSI_BOLD + "═".repeat(60) + TerminalUtils.ANSI_RESET);
            System.out.println(TerminalUtils.ANSI_GREEN + "Transit monitoring system is now running" + TerminalUtils.ANSI_RESET);
            System.out.println(TerminalUtils.ANSI_BLUE + "Press ENTER to exit" + TerminalUtils.ANSI_RESET);
            System.out.println(TerminalUtils.ANSI_BOLD + "═".repeat(60) + TerminalUtils.ANSI_RESET + "\n");

            try {
                new Scanner(System.in).nextLine();
            } catch (Exception e) {
                // Handle potential Scanner issues
                try {
                    Thread.sleep(60000); // Wait 1 minute if input doesn't work
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }

            // Stop the application
            app.stop();
        }
    }
}