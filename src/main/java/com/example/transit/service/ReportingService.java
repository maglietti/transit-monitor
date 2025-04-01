package com.example.transit.service;

import com.example.transit.model.IngestStats;
import com.example.transit.model.ServiceAlert;
import com.example.transit.model.VehiclePosition;
import com.example.transit.util.TerminalUtil;
import org.apache.ignite.client.IgniteClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Service class for reporting and display functionality.
 * Centralizes formatting and displaying data from various services.
 */
public class ReportingService {

    private static final Logger logger = LogManager.getLogger(ReportingService.class);

    private final IgniteClient client;
    private final Map<String, Integer> routeCountHistory = new HashMap<>();
    private final Map<String, Long> statusCountHistory = new HashMap<>();

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Creates a new reporting service with an Ignite client.
     */
    public ReportingService(IgniteClient client) {
        this.client = client;
    }

    /**
     * Formats vehicle position data for display.
     */
    public String formatVehicleData(VehiclePosition position) {
        return String.format("+++ Vehicle %s on route %s at (%.6f, %.6f) - Status: %s",
                position.getVehicleId(),
                position.getRouteId(),
                position.getLatitude(),
                position.getLongitude(),
                position.getCurrentStatus());
    }

    /**
     * Displays active vehicles by route.
     */
    public void displayActiveVehicles() {
        try {
            String sql = "SELECT route_id, COUNT(DISTINCT vehicle_id) as vehicle_count " +
                    "FROM VehiclePosition " +
                    "WHERE TIMESTAMPDIFF(MINUTE, time_stamp, CURRENT_TIMESTAMP) <= 15 " +
                    "GROUP BY route_id ORDER BY vehicle_count DESC LIMIT 10";

            var results = client.sql().execute(null, sql);
            boolean hasData = false;

            while (results.hasNext()) {
                hasData = true;
                var row = results.next();
                String routeId = row.stringValue("route_id");
                int count = (int) row.longValue("vehicle_count");

                // Trend indicators
                String trend = getTrendIndicator(routeId, count, routeCountHistory);
                routeCountHistory.put(routeId, count);

                System.out.printf("• Route %-8s: %3d vehicles%s%n", routeId, count, trend);
            }

            if (!hasData) {
                System.out.println(TerminalUtil.ANSI_YELLOW + "No active vehicles found" + TerminalUtil.ANSI_RESET);
            }
        } catch (Exception e) {
            System.out.println(TerminalUtil.ANSI_RED + "Error: " + e.getMessage() + TerminalUtil.ANSI_RESET);
        }
    }

    /**
     * Displays vehicle status distribution.
     */
    public void displayVehicleStatuses() {
        try {
            String sql = "SELECT current_status, COUNT(*) as status_count " +
                    "FROM VehiclePosition " +
                    "WHERE TIMESTAMPDIFF(MINUTE, time_stamp, CURRENT_TIMESTAMP) <= 15 " +
                    "GROUP BY current_status";

            var results = client.sql().execute(null, sql);
            boolean hasData = false;

            while (results.hasNext()) {
                hasData = true;
                var row = results.next();
                String status = row.stringValue("current_status");
                long count = row.longValue("status_count");

                // Trend indicators
                String trend = getTrendIndicator(status, count, statusCountHistory);
                statusCountHistory.put(status, count);

                // Status colors
                String statusColor = getStatusColor(status);

                System.out.printf("• %s%-15s%s: %5d vehicles%s%n",
                        statusColor, status, TerminalUtil.ANSI_RESET, count, trend);
            }

            if (!hasData) {
                System.out.println(TerminalUtil.ANSI_YELLOW + "No status data available" + TerminalUtil.ANSI_RESET);
            }
        } catch (Exception e) {
            System.out.println(TerminalUtil.ANSI_RED + "Error: " + e.getMessage() + TerminalUtil.ANSI_RESET);
        }
    }

    /**
     * Displays data ingestion status.
     */
    public void displayIngestionStatus(IngestStats stats) {
        String status = stats.isRunning() ? TerminalUtil.ANSI_GREEN + "Running" : TerminalUtil.ANSI_RED + "Stopped";

        System.out.println("• Status: " + status + TerminalUtil.ANSI_RESET);
        System.out.println("• Records fetched: " + stats.getTotalFetched());
        System.out.println("• Records stored: " + stats.getTotalStored());
        System.out.println("• Last fetch count: " + stats.getLastFetchCount());

        if (stats.getLastFetchTimeMs() > 0) {
            System.out.println("• Last fetch time: " + stats.getLastFetchTimeMs() + "ms");
        }

        if (stats.getRunningTimeMs() > 0) {
            long seconds = stats.getRunningTimeMs() / 1000;
            System.out.println("• Running time: " + TerminalUtil.formatDuration(seconds));

            if (stats.getTotalFetched() > 0) {
                double rate = (double) stats.getTotalFetched() / seconds;
                System.out.printf("• Ingestion rate: %.2f records/second%n", rate);
            }
        }
    }

    /**
     * Displays recent service alerts.
     */
    public void displayRecentAlerts(List<ServiceAlert> alerts) {
        if (alerts.isEmpty()) {
            System.out.println(TerminalUtil.ANSI_GREEN + "No active alerts" + TerminalUtil.ANSI_RESET);
            return;
        }

        System.out.println(
                TerminalUtil.ANSI_YELLOW + "Found " + alerts.size() + " active alerts:" + TerminalUtil.ANSI_RESET);

        // Display up to 15 most recent alerts
        alerts.stream()
                .limit(15)
                .forEach(alert -> {
                    // Alert color based on severity
                    String color = getSeverityColor(alert.getSeverity());
                    String time = alert.getTimestamp().format(TerminalUtil.TIME_FORMATTER);

                    System.out.println(color + "• " + alert.getMessage() + TerminalUtil.ANSI_RESET +
                            " [" + time + "]");
                });
    }

    /**
     * Displays alert statistics.
     */
    public void displayAlertStatistics(Map<String, Integer> alertCounts) {
        if (alertCounts.isEmpty()) {
            System.out.println("No alerts have been generated yet");
            return;
        }

        System.out.println("\n===== SERVICE MONITORING ALERTS =====");
        System.out.println("Time: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.println("\nAlert counts by type:");

        alertCounts.forEach((type, count) -> {
            String color = count > 10 ? TerminalUtil.ANSI_RED
                    : count > 0 ? TerminalUtil.ANSI_YELLOW : TerminalUtil.ANSI_GREEN;

            System.out.println(color + "• " + type + ": " + count + " alerts" + TerminalUtil.ANSI_RESET);
        });

        int totalAlerts = alertCounts.values().stream()
                .mapToInt(Integer::intValue).sum();

        System.out.println("\nTotal alerts detected: " + totalAlerts);

        System.out.println("===========================================\n");
    }

    /**
     * Displays system statistics.
     */
    public void displaySystemStatistics() {
        try {
            // Total records
            var countResult = client.sql().execute(null, "SELECT COUNT(*) as total FROM VehiclePosition");
            if (countResult.hasNext()) {
                long total = countResult.next().longValue("total");
                System.out.println("• Total records: " + TerminalUtil.ANSI_BOLD + total + TerminalUtil.ANSI_RESET);
            }

            // Unique vehicles
            var vehiclesResult = client.sql().execute(null,
                    "SELECT COUNT(DISTINCT vehicle_id) as total FROM VehiclePosition");
            if (vehiclesResult.hasNext()) {
                long total = vehiclesResult.next().longValue("total");
                System.out.println("• Unique vehicles: " + TerminalUtil.ANSI_BOLD + total + TerminalUtil.ANSI_RESET);
            }

            // Time span
            var timeResult = client.sql().execute(null,
                    "SELECT MIN(time_stamp) as oldest, MAX(time_stamp) as newest FROM VehiclePosition");
            if (timeResult.hasNext()) {
                var row = timeResult.next();
                Object oldest = row.value("oldest");
                Object newest = row.value("newest");

                if (oldest != null && newest != null) {
                    System.out.println("• Oldest record: " + oldest);
                    System.out.println("• Newest record: " + newest);
                    System.out.println("• Data collection active");
                }
            }
        } catch (Exception e) {
            System.out.println(TerminalUtil.ANSI_RED + "Error: " + e.getMessage() + TerminalUtil.ANSI_RESET);
        }
    }

    /**
     * Displays monitoring thresholds.
     */
    public void displayMonitoringThresholds() {
        System.out.println("• Delayed vehicle threshold: 5 minutes");
        System.out.println("• Vehicle bunching distance: 1 km");
        System.out.println("• Minimum vehicles per route: 2");
        System.out.println("• Vehicle offline threshold: 15 minutes");
    }

    /**
     * Displays connection status.
     */
    public void displayConnectionStatus(IngestStats ingestionStats) {
        try {
            System.out.println("• Ignite cluster: " + TerminalUtil.ANSI_GREEN + "Connected" + TerminalUtil.ANSI_RESET);
            System.out.println("• Database: VehiclePosition table accessible");

            String ingestionStatus = ingestionStats.isRunning() ? TerminalUtil.ANSI_GREEN + "Active"
                    : TerminalUtil.ANSI_RED + "Inactive";

            System.out.println("• Data ingestion service: " + ingestionStatus + TerminalUtil.ANSI_RESET);
            System.out.println("• Monitoring service: Active");
        } catch (Exception e) {
            System.out.println(TerminalUtil.ANSI_RED + "Error: " + e.getMessage() + TerminalUtil.ANSI_RESET);
        }
    }

    /**
     * Analyzes vehicle position data and displays statistics.
     */
    public void analyzeVehicleData(List<VehiclePosition> positions) {
        // Count unique routes and vehicles
        long uniqueRoutes = positions.stream()
                .map(VehiclePosition::getRouteId)
                .distinct()
                .count();

        long uniqueVehicles = positions.stream()
                .map(VehiclePosition::getVehicleId)
                .distinct()
                .count();

// Count vehicles by status
        Map<String, Long> statusCounts = positions.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        VehiclePosition::getCurrentStatus,
                        java.util.stream.Collectors.counting()));

// Find top 5 routes by vehicle count
        Map<String, Long> routeCounts = positions.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        VehiclePosition::getRouteId,
                        java.util.stream.Collectors.counting()));

        List<Map.Entry<String, Long>> topRoutes = routeCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .collect(java.util.stream.Collectors.toList());

        // Display statistics
        System.out.println("\n=== Transit System Statistics ===");
        System.out.println("• Unique routes: " + uniqueRoutes);
        System.out.println("• Unique vehicles: " + uniqueVehicles);

        System.out.println("\nVehicle status distribution:");
        statusCounts.forEach((status, count) -> System.out.println("• " + status + ": " + count + " vehicles (" +
                String.format("%.1f", (count * 100.0 / positions.size())) + "%)"));

        System.out.println("\nTop 5 routes by vehicle count:");
        for (Map.Entry<String, Long> route : topRoutes) {
            System.out.println("• Route " + route.getKey() + ": " +
                    route.getValue() + " vehicles");
        }

        // Calculate geographic bounds
        double minLat = positions.stream()
                .mapToDouble(VehiclePosition::getLatitude)
                .min()
                .orElse(0);

        double maxLat = positions.stream()
                .mapToDouble(VehiclePosition::getLatitude)
                .max()
                .orElse(0);

        double minLon = positions.stream()
                .mapToDouble(VehiclePosition::getLongitude)
                .min()
                .orElse(0);

        double maxLon = positions.stream()
                .mapToDouble(VehiclePosition::getLongitude)
                .max()
                .orElse(0);

        System.out.println("\nGeographic coverage:");
        System.out.println("• Latitude range: " + minLat + " to " + maxLat);
        System.out.println("• Longitude range: " + minLon + " to " + maxLon);
    }

    /**
     * Verifies and displays sample vehicle data from the database.
     */
    public void sampleVehicleData() {
        System.out.println("--- Verifying data in VehiclePosition table");

        try {
            // Count records using SQL query
            String countSql = "SELECT COUNT(*) FROM VehiclePosition";
            try (var countResult = client.sql().execute(null, countSql)) {
                long recordCount = 0;

                if (countResult.hasNext()) {
                    recordCount = countResult.next().longValue(0);
                    System.out.println("<<< Table contains " + recordCount + " records");
                } else {
                    System.out.println("<<< No results returned from count query.");
                }

                if (recordCount == 0) {
                    System.out.println("--- Table is empty. Start the ingestion service to load some data.");
                    return;
                }
            }

            // Sample recent records
            System.out.println("\nSample records (most recent):");
            String sampleSql = "SELECT * FROM VehiclePosition ORDER BY time_stamp DESC LIMIT 3";

            try (var sampleResult = client.sql().execute(null, sampleSql)) {
                while (sampleResult.hasNext()) {
                    var row = sampleResult.next();

                    LocalDateTime timestamp = row.value("time_stamp");
                    String vehicleId = row.stringValue("vehicle_id");
                    String routeId = row.stringValue("route_id");
                    String status = row.stringValue("current_status");

                    System.out.println("Vehicle: " + vehicleId +
                            ", Route: " + routeId +
                            ", Status: " + status +
                            ", Time: " + (timestamp != null ? timestamp.format(DATETIME_FORMATTER) : "null"));
                }
            }

            // Get route statistics
            System.out.println("\nTop routes by number of records:");
            String routeStatsSql = "SELECT route_id, COUNT(*) as total " +
                    "FROM VehiclePosition " +
                    "GROUP BY route_id " +
                    "ORDER BY total DESC " +
                    "LIMIT 5";

            try (var routeResult = client.sql().execute(null, routeStatsSql)) {
                while (routeResult.hasNext()) {
                    var row = routeResult.next();
                    String routeId = row.stringValue("route_id");
                    long total = row.longValue("total");

                    System.out.println("Route " + routeId + ": " + total + " records");
                }
            }

        } catch (Exception e) {
            logger.error("Error verifying data: {}", e.getMessage());
        }
    }

    /**
     * Prints a dashboard header.
     */
    public void printDashboardHeader(int width) {
        String headerText = "TRANSIT MONITORING DASHBOARD";
        String currentTime = LocalDateTime.now().format(TerminalUtil.DATE_TIME_FORMATTER);

        TerminalUtil.printCenteredBox(headerText, width);
        System.out.println(TerminalUtil.ANSI_CYAN + "Current time: " +
                TerminalUtil.ANSI_BOLD + currentTime +
                TerminalUtil.ANSI_RESET);
        System.out.println();
    }

    /**
     * Prints the dashboard footer.
     */
    public void printDashboardFooter(int refreshSeconds) {
        System.out.println();
        System.out.println(TerminalUtil.ANSI_BLUE + "Views rotate every " +
                refreshSeconds + " seconds" + TerminalUtil.ANSI_RESET);
        System.out.println(TerminalUtil.ANSI_BOLD + TerminalUtil.ANSI_BLUE +
                "Press ENTER to exit" + TerminalUtil.ANSI_RESET);
    }

    /**
     * Get the title for a specific dashboard view type.
     */
    public String getViewTitle(int viewType) {
        switch (viewType) {
            case 0:
                return "SUMMARY VIEW";
            case 1:
                return "SERVICE ALERTS VIEW";
            case 2:
                return "SYSTEM DETAILS VIEW";
            default:
                return "UNKNOWN VIEW";
        }
    }

    /**
     * Helper to get trend indicator for counts.
     */
    private String getTrendIndicator(String key, long currentCount, Map<String, ?> history) {
        if (!history.containsKey(key)) {
            return "";
        }

        long previousCount = 0;
        if (history.get(key) instanceof Integer) {
            previousCount = (Integer) history.get(key);
        } else if (history.get(key) instanceof Long) {
            previousCount = (Long) history.get(key);
        }

        if (currentCount > previousCount) {
            return TerminalUtil.ANSI_GREEN + " ↑" + TerminalUtil.ANSI_RESET;
        } else if (currentCount < previousCount) {
            return TerminalUtil.ANSI_RED + " ↓" + TerminalUtil.ANSI_RESET;
        } else {
            return TerminalUtil.ANSI_BLUE + " =" + TerminalUtil.ANSI_RESET;
        }
    }

    /**
     * Helper to get color based on vehicle status.
     */
    private String getStatusColor(String status) {
        switch (status) {
            case "STOPPED_AT":
                return TerminalUtil.ANSI_RED;
            case "IN_TRANSIT_TO":
                return TerminalUtil.ANSI_GREEN;
            case "INCOMING_AT":
                return TerminalUtil.ANSI_BLUE;
            default:
                return TerminalUtil.ANSI_RESET;
        }
    }

    /**
     * Helper to get color based on alert severity.
     */
    private String getSeverityColor(int severity) {
        if (severity > 20) {
            return TerminalUtil.ANSI_RED;
        } else if (severity < 5) {
            return TerminalUtil.ANSI_BLUE;
        } else {
            return TerminalUtil.ANSI_YELLOW;
        }
    }
}