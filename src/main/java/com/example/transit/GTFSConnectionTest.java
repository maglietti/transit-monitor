package com.example.transit;

import io.github.cdimascio.dotenv.Dotenv;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Test class to verify the GTFS connection and data parsing.
 * This standalone application demonstrates fetching and analyzing
 * real-time vehicle positions from a transit agency.
 */
public class GTFSConnectionTest {

    public static void main(String[] args) {
        System.out.println("=== GTFS Connection Test ===");

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

        System.out.println("Using GTFS feed URL: " + feedUrl.replaceAll(apiToken, "[API_TOKEN]")); // Hide token in logs

        // Create the feed client
        GTFSFeedClient feedClient = new GTFSFeedClient(feedUrl);

        try {
            // Fetch vehicle positions
            System.out.println("Fetching vehicle positions...");
            List<VehiclePosition> positions = feedClient.getVehiclePositions();

            if (positions.isEmpty()) {
                System.out.println("Warning: No vehicle positions found in the feed.");
                System.out.println("This could indicate an issue with the feed URL, API token, or the agency may not have active vehicles at this time.");
                return;
            }

            System.out.println("Success! Retrieved " + positions.size() + " vehicle positions.");

            // Print the first 5 positions as a sample
            System.out.println("\nSample data (first 5 vehicles):");
            positions.stream()
                    .limit(5)
                    .forEach(System.out::println);

            // Calculate and display statistics
            analyzeVehicleData(positions);

        } catch (IOException e) {
            System.err.println("Error testing GTFS feed: " + e.getMessage());
            System.err.println("Check your internet connection and API token.");
            e.printStackTrace();

            // Provide fallback options
            System.out.println("\nTroubleshooting suggestions:");
            System.out.println("1. Verify your API token is correct in the .env file");
            System.out.println("2. Check if the agency code is correct (e.g., 'SF' for San Francisco Muni)");
            System.out.println("3. Try accessing the feed URL in a browser (with your API token)");
        }
    }

    /**
     * Analyzes the vehicle position data and displays useful statistics.
     *
     * @param positions List of vehicle positions to analyze
     */
    private static void analyzeVehicleData(List<VehiclePosition> positions) {
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
                .collect(Collectors.groupingBy(
                        VehiclePosition::getCurrentStatus,
                        Collectors.counting()
                ));

        // Find top 5 routes by vehicle count
        Map<String, Long> routeCounts = positions.stream()
                .collect(Collectors.groupingBy(
                        VehiclePosition::getRouteId,
                        Collectors.counting()
                ));

        List<Map.Entry<String, Long>> topRoutes = routeCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toList());

        // Display statistics
        System.out.println("\n=== Transit System Statistics ===");
        System.out.println("• Unique routes: " + uniqueRoutes);
        System.out.println("• Unique vehicles: " + uniqueVehicles);

        System.out.println("\nVehicle status distribution:");
        statusCounts.forEach((status, count) ->
                System.out.println("• " + status + ": " + count + " vehicles (" +
                        String.format("%.1f", (count * 100.0 / positions.size())) + "%)"));

        System.out.println("\nTop 5 routes by vehicle count:");
        for (int i = 0; i < topRoutes.size(); i++) {
            Map.Entry<String, Long> route = topRoutes.get(i);
            System.out.println("• Route " + route.getKey() + ": " +
                    route.getValue() + " vehicles");
        }

        // Calculate geographic bounds
        double minLat = positions.stream().mapToDouble(VehiclePosition::getLatitude).min().orElse(0);
        double maxLat = positions.stream().mapToDouble(VehiclePosition::getLatitude).max().orElse(0);
        double minLon = positions.stream().mapToDouble(VehiclePosition::getLongitude).min().orElse(0);
        double maxLon = positions.stream().mapToDouble(VehiclePosition::getLongitude).max().orElse(0);

        System.out.println("\nGeographic coverage:");
        System.out.println("• Latitude range: " + minLat + " to " + maxLat);
        System.out.println("• Longitude range: " + minLon + " to " + maxLon);
    }
}