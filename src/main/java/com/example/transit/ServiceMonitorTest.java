package com.example.transit;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.ignite.client.IgniteClient;

/**
 * Test application for the service monitor.
 * Demonstrates setting up and running the monitoring service.
 */
public class ServiceMonitorTest {

    public static void main(String[] args) {
        System.out.println("=== Service Monitor Test ===");

        try {
            // First, verify we have data to monitor
            System.out.println("\nVerifying database data...");
            DataVerifier.verifyData();

            // Create and start the service monitor
            System.out.println("\nStarting service monitor...");
            ServiceMonitor monitor = new ServiceMonitor();
            monitor.startMonitoring(60); // Check every 60 seconds

            // Let the monitor run for a while
            System.out.println("\nMonitor is running. Press Enter to stop...");
            System.in.read();

            // Stop the monitor
            System.out.println("\nStopping service monitor...");
            monitor.stopMonitoring();

            // Display final results
            List<ServiceMonitor.ServiceAlert> alerts = monitor.getRecentAlerts();
            Map<String, Integer> alertCounts = monitor.getAlertCounts();

            System.out.println("\n=== Monitoring Results ===");
            System.out.println("Total alerts detected: " + alerts.size());

            alertCounts.forEach((type, count) -> {
                System.out.println("• " + type + ": " + count);
            });

            if (!alerts.isEmpty()) {
                System.out.println("\nSample alerts:");
                alerts.stream()
                        .limit(5)
                        .forEach(alert -> {
                            System.out.println("• " + alert.getMessage() +
                                    " (Severity: " + alert.getSeverity() + ")");
                        });
            }

            System.out.println("\nTest completed successfully!");

        } catch (IOException e) {
            System.err.println("Error during monitor test: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Error during monitor test: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Clean up connection
            // Using Ignite 3 client closing pattern instead of a custom connection class
            try {
                // Assuming IgniteConnection is a wrapper around IgniteClient
                IgniteClient client = IgniteConnection.getClient();
                if (client != null) {
                    client.close();
                }
            } catch (Exception e) {
                System.err.println("Error closing Ignite client: " + e.getMessage());
            }
        }
    }
}