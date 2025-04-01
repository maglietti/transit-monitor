package com.example.transit.model;

import org.apache.ignite.catalog.annotations.Column;
import org.apache.ignite.catalog.annotations.ColumnRef;
import org.apache.ignite.catalog.annotations.Id;
import org.apache.ignite.catalog.annotations.Index;
import org.apache.ignite.catalog.annotations.Table;
import org.apache.ignite.catalog.annotations.Zone;

import java.time.LocalDateTime;

/**
 * POJO for storing vehicle position data in Ignite.
 * Maps to a database table with the same name.
 */
@Table(
        zone = @Zone(value = "transit", storageProfiles = "default"),
        indexes = {
                @Index(value = "IDX_VP_ROUTE_ID", columns = { @ColumnRef("route_id") }),
                @Index(value = "IDX_VP_STATUS", columns = { @ColumnRef("current_status") })
        }
)
public class VehiclePosition {
    @Id
    @Column(value = "vehicle_id", nullable = false)
    private String vehicleId;

    @Id
    @Column(value = "time_stamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(value = "route_id", nullable = false)
    private String routeId;

    @Column(value = "latitude", nullable = false)
    private Double latitude;

    @Column(value = "longitude", nullable = false)
    private Double longitude;

    @Column(value = "current_status", nullable = false)
    private String currentStatus;

    /**
     * Default constructor required for Ignite.
     */
    public VehiclePosition() {
    }

    /**
     * Constructor with all fields.
     */
    public VehiclePosition(String vehicleId, LocalDateTime timestamp, String routeId,
                           double latitude, double longitude, String currentStatus) {
        this.vehicleId = vehicleId;
        this.timestamp = timestamp;
        this.routeId = routeId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.currentStatus = currentStatus;
    }

    // Getters and setters
    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getRouteId() {
        return routeId;
    }

    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }

    @Override
    public String toString() {
        return "VehiclePosition{" +
                "vehicleId='" + vehicleId + '\'' +
                ", timestamp=" + timestamp +
                ", routeId='" + routeId + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", currentStatus='" + currentStatus + '\'' +
                '}';
    }
}