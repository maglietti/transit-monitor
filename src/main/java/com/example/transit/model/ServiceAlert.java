package com.example.transit.model;

import java.time.LocalDateTime;

/**
 * Represents a service alert in the transit monitoring system.
 * This class encapsulates information about detected issues such as
 * delayed vehicles, vehicle bunching, offline vehicles, or low route coverage.
 */
public class ServiceAlert {
    private final String type;
    private final String message;
    private final String routeId;
    private final String vehicleId;
    private final double latitude;
    private final double longitude;
    private final int severity;
    private final LocalDateTime timestamp;

    /**
     * Creates a new service alert.
     *
     * @param type Alert type (e.g., "DELAYED_VEHICLE", "VEHICLE_BUNCHING")
     * @param message Human-readable description of the alert
     * @param routeId Affected route identifier, or null if not applicable
     * @param vehicleId Affected vehicle identifier, or null if not applicable
     * @param latitude Geographic latitude of the alert, or 0 if not applicable
     * @param longitude Geographic longitude of the alert, or 0 if not applicable
     * @param severity Alert severity (higher numbers indicate more severe issues)
     */
    public ServiceAlert(String type, String message, String routeId, String vehicleId,
            double latitude, double longitude, int severity) {
        this.type = type;
        this.message = message;
        this.routeId = routeId;
        this.vehicleId = vehicleId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.severity = severity;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Gets the alert type.
     *
     * @return Alert type identifier
     */
    public String getType() {
        return type;
    }

    /**
     * Gets the human-readable alert message.
     *
     * @return Alert message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets the affected route identifier.
     *
     * @return Route ID or null if not applicable
     */
    public String getRouteId() {
        return routeId;
    }

    /**
     * Gets the affected vehicle identifier.
     *
     * @return Vehicle ID or null if not applicable
     */
    public String getVehicleId() {
        return vehicleId;
    }

    /**
     * Gets the geographic latitude of the alert.
     *
     * @return Latitude or 0 if not applicable
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * Gets the geographic longitude of the alert.
     *
     * @return Longitude or 0 if not applicable
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * Gets the alert severity.
     * Higher numbers indicate more severe issues.
     *
     * @return Severity value
     */
    public int getSeverity() {
        return severity;
    }

    /**
     * Gets the timestamp when the alert was created.
     *
     * @return Alert creation timestamp
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "ServiceAlert{" +
                "type='" + type + '\'' +
                ", message='" + message + '\'' +
                ", routeId='" + routeId + '\'' +
                ", vehicleId='" + vehicleId + '\'' +
                ", severity=" + severity +
                ", timestamp=" + timestamp +
                '}';
    }
}
