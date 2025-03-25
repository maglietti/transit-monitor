package com.example.transit.examples;

import com.example.transit.service.*;
import com.example.transit.util.LoggingUtil;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Example application demonstrating the use of the monitoring service.
 * Shows how to set up and run the monitoring service to detect transit system
 * issues.
 */
public class ServiceMonitorExample {

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static void main(String[] args) {
        System.out.println("=== Service Monitor Example ===");

        // Configure logging to suppress unnecessary output
        LoggingUtil.setLogs("OFF");

        // Create a connection service that will be used throughout the application
        try (ConnectService connectionService = new ConnectService()) {
            // Create reporting service
            ReportService reportingService = new ReportService(connectionService.getClient());

            // First, verify we have data to monitor
            System.out.println("\n--- Verifying database data...");
            reportingService.sampleVehicleData();

            // Create and start the monitoring service
            System.out.println("\n=== Starting monitoring service...");
            MonitorService monitor = new MonitorService(connectionService);

            // Set quiet mode to true to suppress individual alert output
            monitor.setQuietMode(true);

            monitor.startMonitoring(60); // Check every 60 seconds

            // Schedule a task to regularly print monitoring statistics
            System.out.println("\n=== Setting up statistics reporting...");
            scheduler.scheduleAtFixedRate(
                    () -> reportingService.displayAlertStatistics(monitor.getAlertCounts()),
                    30,  // Initial delay of 30 seconds
                    30,  // Print stats every 30 seconds
                    TimeUnit.SECONDS
            );

            // Let the monitor run and wait for user input to stop
            System.out.println("\n--- Monitor is running. Press Enter to stop...");
            System.in.read();

            // Stop the monitor and scheduler
            System.out.println("\n=== Stopping monitoring service...");
            monitor.stopMonitoring();

            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }

            // Display final results
            List<MonitorService.ServiceAlert> alerts = monitor.getRecentAlerts();
            Map<String, Integer> alertCounts = monitor.getAlertCounts();

            System.out.println("\n=== Monitoring Results ===");
            System.out.println("Total alerts detected: " +
                    alertCounts.values().stream().mapToInt(Integer::intValue).sum());

            // Display alert statistics
            reportingService.displayAlertStatistics(alertCounts);

            if (!alerts.isEmpty()) {
                System.out.println("\nSample alerts:");
                reportingService.displayRecentAlerts(alerts.stream().limit(5).collect(Collectors.toList()));
            }

            System.out.println("\nExample completed successfully!");
        } catch (Exception e) {
            System.err.println("Error during monitor example: " + e.getMessage());
            e.printStackTrace();
        }
    }
}