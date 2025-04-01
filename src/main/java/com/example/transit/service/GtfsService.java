package com.example.transit.service;

import com.example.transit.model.VehiclePosition;
import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.Position;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for retrieving and processing GTFS-realtime feed data.
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
     * @return List of vehicle position objects
     * @throws IOException if there's an error fetching or parsing the feed
     */
    public List<VehiclePosition> getVehiclePositions() throws IOException {
        List<VehiclePosition> positions = new ArrayList<>();

        try {
            // Parse feed from URL
            URL url = new URL(feedUrl);
            FeedMessage feed = FeedMessage.parseFrom(url.openStream());

            // Process each entity in the feed
            for (FeedEntity entity : feed.getEntityList()) {
                if (entity.hasVehicle()) {
                    com.google.transit.realtime.GtfsRealtime.VehiclePosition vehicle = entity.getVehicle();

                    if (vehicle.hasPosition() && vehicle.hasVehicle() && vehicle.hasTrip()) {
                        Position position = vehicle.getPosition();
                        String vehicleId = vehicle.getVehicle().getId();
                        String routeId = vehicle.getTrip().getRouteId();
                        String status = mapVehicleStatus(vehicle);

                        // Get timestamp (convert seconds to milliseconds or use current time)
                        long timestamp = vehicle.hasTimestamp() ? vehicle.getTimestamp() * 1000
                                : System.currentTimeMillis();

                        // Convert to LocalDateTime for Ignite storage
                        LocalDateTime localDateTime = LocalDateTime.ofInstant(
                                Instant.ofEpochMilli(timestamp),
                                ZoneId.systemDefault());

                        // Create VehiclePosition object
                        VehiclePosition vehiclePosition = new VehiclePosition(
                                vehicleId,
                                localDateTime,
                                routeId,
                                position.getLatitude(),
                                position.getLongitude(),
                                status
                        );

                        positions.add(vehiclePosition);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error fetching GTFS feed: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("Error parsing GTFS feed: " + e.getMessage());
            throw new IOException("Failed to process GTFS feed", e);
        }

        return positions;
    }

    /**
     * Maps the GTFS vehicle status enum to a string representation.
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