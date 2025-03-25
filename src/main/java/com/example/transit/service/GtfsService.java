package com.example.transit.service;

import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.Position;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for retrieving GTFS-realtime feed data.
 * This class handles the connection to the transit agency's GTFS feed,
 * parses the protobuf-formatted data, and converts it to maps for storage.
 */
public class GtfsService {
    private final String feedUrl;

    /**
     * Creates a new GTFS feed service.
     *
     * @param feedUrl The URL of the GTFS-realtime vehicle positions feed
     */
    public GtfsService(String feedUrl) {
        this.feedUrl = feedUrl;
    }

    /**
     * Retrieves vehicle positions from the GTFS feed.
     *
     * @return List of maps containing vehicle positions
     * @throws IOException if there's an error fetching or parsing the feed
     */
    public List<Map<String, Object>> getVehiclePositions() throws IOException {
        List<Map<String, Object>> positions = new ArrayList<>();

        try {
            // Parse feed directly from URL
            URL url = new URL(feedUrl);
            FeedMessage feed = FeedMessage.parseFrom(url.openStream());

            // Process each entity in the feed
            for (FeedEntity entity : feed.getEntityList()) {
                // Only process entities that contain vehicle position data
                if (entity.hasVehicle()) {
                    com.google.transit.realtime.GtfsRealtime.VehiclePosition vehicle = entity.getVehicle();

                    // Ensure we have the required fields before processing
                    if (vehicle.hasPosition() && vehicle.hasVehicle() && vehicle.hasTrip()) {
                        Position position = vehicle.getPosition();
                        String vehicleId = vehicle.getVehicle().getId();
                        String routeId = vehicle.getTrip().getRouteId();

                        // Map the GTFS status to our string representation
                        String status = mapVehicleStatus(vehicle);

                        // Get timestamp (convert seconds to milliseconds if present, otherwise use
                        // current time)
                        long timestamp = vehicle.hasTimestamp() ? vehicle.getTimestamp() * 1000
                                : System.currentTimeMillis();

                        // LocalDateTime for Ignite storage
                        LocalDateTime localDateTime = LocalDateTime.ofInstant(
                                Instant.ofEpochMilli(timestamp),
                                ZoneId.systemDefault());

                        // Create a map for the vehicle position
                        Map<String, Object> vehiclePosition = new HashMap<>();
                        vehiclePosition.put("vehicle_id", vehicleId);
                        vehiclePosition.put("route_id", routeId);
                        vehiclePosition.put("latitude", position.getLatitude());
                        vehiclePosition.put("longitude", position.getLongitude());
                        vehiclePosition.put("time_stamp", localDateTime);
                        vehiclePosition.put("current_status", status);

                        positions.add(vehiclePosition);
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Error fetching GTFS feed: " + e.getMessage());
            throw e; // Rethrow to allow caller to handle the exception
        } catch (Exception e) {
            System.err.println("Error parsing GTFS feed: " + e.getMessage());
            throw new IOException("Failed to process GTFS feed", e);
        }

        return positions;
    }

    /**
     * Maps the GTFS vehicle status enum to a string representation.
     *
     * @param vehicle The GTFS vehicle position object
     * @return String representation of the vehicle status
     */
    private String mapVehicleStatus(com.google.transit.realtime.GtfsRealtime.VehiclePosition vehicle) {
        if (!vehicle.hasCurrentStatus()) {
            return "UNKNOWN";
        }

        switch (vehicle.getCurrentStatus()) {
            case IN_TRANSIT_TO:
                return "IN_TRANSIT_TO";
            case STOPPED_AT:
                return "STOPPED_AT";
            case INCOMING_AT:
                return "INCOMING_AT";
            default:
                return "UNKNOWN";
        }
    }
}