package com.example.transit;

import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.Position;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Client for retrieving GTFS-realtime feed data.
 * This class handles the connection to the transit agency's GTFS feed,
 * parses the protobuf-formatted data, and converts it to our domain model.
 */
public class GTFSFeedClient {
    private final String feedUrl;

    /**
     * Creates a new GTFS feed client.
     *
     * @param feedUrl The URL of the GTFS-realtime vehicle positions feed
     */
    public GTFSFeedClient(String feedUrl) {
        this.feedUrl = feedUrl;
    }

    /**
     * Retrieves vehicle positions from the GTFS feed.
     * This method:
     * 1. Connects to the feed URL
     * 2. Parses the protobuf data
     * 3. Transforms it into our VehiclePosition domain objects
     *
     * @return List of vehicle positions
     * @throws IOException if there's an error fetching or parsing the feed
     */
    public List<VehiclePosition> getVehiclePositions() throws IOException {
        List<VehiclePosition> positions = new ArrayList<>();

        try {
            // Parse feed directly from URL
            URL url = new URL(feedUrl);
            FeedMessage feed = FeedMessage.parseFrom(url.openStream());

            // Log feed metadata
            System.out.println("GTFS Feed Version: " + feed.getHeader().getGtfsRealtimeVersion());
            System.out.println("Feed Timestamp: " + feed.getHeader().getTimestamp());
            System.out.println("Total entities: " + feed.getEntityCount());

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
                        String status = "UNKNOWN";
                        if (vehicle.hasCurrentStatus()) {
                            switch (vehicle.getCurrentStatus()) {
                                case IN_TRANSIT_TO:
                                    status = "IN_TRANSIT_TO";
                                    break;
                                case STOPPED_AT:
                                    status = "STOPPED_AT";
                                    break;
                                case INCOMING_AT:
                                    status = "INCOMING_AT";
                                    break;
                                default:
                                    status = "UNKNOWN";
                                    break;
                            }
                        }

                        // Create our vehicle position object
                        positions.add(new VehiclePosition(
                                vehicleId,
                                routeId,
                                position.getLatitude(),
                                position.getLongitude(),
                                // Convert seconds to milliseconds if present, otherwise use current time
                                vehicle.hasTimestamp() ? vehicle.getTimestamp() * 1000 : System.currentTimeMillis(),
                                status
                        ));
                    }
                }
            }

            System.out.println("Fetched " + positions.size() + " vehicle positions from feed");

        } catch (IOException e) {
            System.err.println("Error fetching GTFS feed: " + e.getMessage());
            throw e; // Rethrow to allow caller to handle the exception
        } catch (Exception e) {
            System.err.println("Error parsing GTFS feed: " + e.getMessage());
            throw new IOException("Failed to process GTFS feed", e);
        }

        return positions;
    }
}