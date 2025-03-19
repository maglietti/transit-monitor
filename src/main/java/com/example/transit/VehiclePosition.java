package com.example.transit;

import java.sql.Timestamp;
import java.time.Instant;
import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a single vehicle position record from GTFS-realtime data.
 * This class captures the essential information about a transit vehicle's
 * location and status at a specific point in time.
 */
public class VehiclePosition implements Serializable {
    // Good practice to define a serialVersionUID for Serializable classes
    private static final long serialVersionUID = 1L;

    private String vehicleId;    // Unique identifier for the vehicle
    private String routeId;      // The route this vehicle is serving
    private double latitude;     // Geographic latitude coordinate
    private double longitude;    // Geographic longitude coordinate
    private long timestamp;      // When this position was recorded (epoch millis)
    private String currentStatus; // Vehicle status (IN_TRANSIT_TO, STOPPED_AT, etc.)

    /**
     * Default no-arg constructor required for serialization and ORM frameworks
     */
    public VehiclePosition() {
        // Default constructor required for POJO usage with Ignite
    }

    /**
     * Full constructor for creating a VehiclePosition object
     *
     * @param vehicleId Unique identifier for the vehicle
     * @param routeId The route this vehicle is serving
     * @param latitude Geographic latitude coordinate
     * @param longitude Geographic longitude coordinate
     * @param timestamp When this position was recorded (epoch millis)
     * @param currentStatus Vehicle status (IN_TRANSIT_TO, STOPPED_AT, etc.)
     */
    public VehiclePosition(String vehicleId, String routeId, double latitude,
                           double longitude, long timestamp, String currentStatus) {
        this.vehicleId = vehicleId;
        this.routeId = routeId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.currentStatus = currentStatus;
    }

    // Getters and Setters
    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }

    public String getRouteId() { return routeId; }
    public void setRouteId(String routeId) { this.routeId = routeId; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(String currentStatus) { this.currentStatus = currentStatus; }

    /**
     * Convenience method to convert the epoch millisecond timestamp to an Instant
     *
     * @return The timestamp as a Java Instant
     */
    public Instant getTimestampAsInstant() { return Instant.ofEpochMilli(this.timestamp); }

    /**
     * Equals method for object comparison
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VehiclePosition that = (VehiclePosition) o;

        return Double.compare(that.latitude, latitude) == 0 &&
                Double.compare(that.longitude, longitude) == 0 &&
                timestamp == that.timestamp &&
                Objects.equals(vehicleId, that.vehicleId) &&
                Objects.equals(routeId, that.routeId) &&
                Objects.equals(currentStatus, that.currentStatus);
    }

    /**
     * Hash code implementation for collections
     */
    @Override
    public int hashCode() {
        return Objects.hash(vehicleId, routeId, latitude, longitude, timestamp, currentStatus);
    }

    /**
     * String representation of the vehicle position
     */
    @Override
    public String toString() {
        return "Vehicle ID: " + vehicleId +
                ", Route: " + routeId +
                ", Position: (" + latitude + ", " + longitude + ")" +
                ", Status: " + currentStatus +
                ", Time: " + new Timestamp(timestamp);
    }
}