package com.example.transit.examples;

import com.example.transit.config.IgniteConnectionManager;
import com.example.transit.model.ServiceAlert;
import com.example.transit.service.MonitorService;
import com.example.transit.service.ReportingService;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Example application demonstrating the use of the monitoring service.
 * Shows how to set up and run the monitoring service to detect transit system issues.
 */
public class ServiceMonitorExample {

    private static final Logger logger = LogManager.getLogger(ServiceMonitorExample.class);

    public static void main(String[] args) {

        // Handle JUL logging
        java.util.logging.LogManager.getLogManager().reset();
        org.apache.logging.log4j.jul.LogManager.getLogManager();
        java.util.logging.Logger.getLogger("").setLevel(java.util.logging.Level.WARNING);

        System.out.println("=== Service Monitor Example ===");

        // Create a connection service that will be used throughout the application
        try (IgniteConnectionManager connectionManager = new IgniteConnectionManager()) {
            runMonitoringDemo(connectionManager);
        } catch (Exception e) {
            logger.error("Error during monitor example: {}", e.getMessage());
        }
    }

    /**
     * Run the monitoring demonstration with the provided connection manager.
     */
    private static void runMonitoringDemo(IgniteConnectionManager connectionManager) throws IOException {
        // Create reporting service
        ReportingService reportingService = new ReportingService(connectionManager.getClient());

        // First, verify we have data to monitor
        System.out.println("\n--- Verifying database data...");
        reportingService.sampleVehicleData();

        // Create scheduler for periodic stats reporting
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        try {
            // Create and start the monitoring service
            System.out.println("\n=== Starting monitoring service...");
            MonitorService monitor = new MonitorService(connectionManager);

            // Set quiet mode to true to suppress individual alert output
            monitor.setQuietMode(true);

            // Start monitoring (check every 60 seconds)
            monitor.startMonitoring(60);

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
            shutdownScheduler(scheduler);

            // Display final results
            displayFinalResults(monitor, reportingService);

        } finally {
            // Ensure scheduler is shut down if an exception occurs
            shutdownScheduler(scheduler);
        }
    }

    /**
     * Properly shut down the scheduler.
     */
    private static void shutdownScheduler(ScheduledExecutorService scheduler) {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Display the final monitoring results.
     */
    private static void displayFinalResults(MonitorService monitor, ReportingService reportingService) {
        List<ServiceAlert> alerts = monitor.getRecentAlerts();
        Map<String, Integer> alertCounts = monitor.getAlertCounts();

        System.out.println("\n=== Monitoring Results ===");
        System.out.println("Total alerts detected: " +
                alertCounts.values().stream().mapToInt(Integer::intValue).sum());

        // Display alert statistics
        reportingService.displayAlertStatistics(alertCounts);

        if (!alerts.isEmpty()) {
            System.out.println("\nSample alerts:");
            reportingService.displayRecentAlerts(
                    alerts.stream().limit(5).collect(java.util.stream.Collectors.toList())
            );
        }

        System.out.println("\nExample completed successfully!");
    }
}