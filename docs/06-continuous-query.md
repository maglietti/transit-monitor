# Adding a Service Monitor

In this module, we'll implement a monitoring system that continuously polls for potential service disruptions in our transit network. Leveraging Apache Ignite's SQL query capability, we can detect specific conditions and trigger appropriate responses when those conditions are met.

## Transit Service Monitoring

An effective transit monitoring system needs to detect potential issues by analyzing the current state of the fleet. We'll implement a monitoring service that checks for several key conditions:

1. **Delayed vehicles**: Vehicles that have been stopped for too long
2. **Vehicle bunching**: Multiple vehicles on the same route too close together
3. **Low route coverage**: Routes with fewer than the minimum required vehicles
4. **Offline vehicles**: Vehicles that haven't reported their position recently

These conditions help operators identify service disruptions before they significantly impact passengers.

## Understanding the Monitor Service

Let's explore the `MonitorService` class that handles monitoring for our transit system:

```shell
open src/main/java/com/example/transit/service/MonitorService.java
```

The class contains several configuration components:

```java
// Thresholds for monitoring conditions
private static final int STOPPED_THRESHOLD_MINUTES = 5;
private static final int BUNCHING_DISTANCE_KM = 1;
private static final int MINIMUM_VEHICLES_PER_ROUTE = 2;
private static final int OFFLINE_THRESHOLD_MINUTES = 15;

// Monitoring infrastructure
private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
private final IgniteClient client;

// Statistics tracking
private final Map<String, AtomicInteger> alertCounts = new HashMap<>();
private final List<ServiceAlert> recentAlerts = new ArrayList<>();
```

This code defines:

1. **Threshold constants** that determine when an alert is triggered
2. **A scheduler** that runs monitoring tasks at regular intervals
3. **A client connection** to the Ignite database
4. **Statistics tracking** to count alerts by type and store recent alerts

Let's look at how the service is started:

```java
public void startMonitoring(int intervalSeconds) {
    System.out.println("--- Starting service disruption monitoring (polling every " + intervalSeconds + " seconds)");

    // Schedule all monitoring tasks
    scheduler.scheduleAtFixedRate(
            this::performMonitoring,
            5,
            intervalSeconds,
            TimeUnit.SECONDS);
}
```

This method schedules a monitoring task to run at a specified interval. The `performMonitoring` method calls several individual monitoring methods:

```java
private void performMonitoring() {
    checkForDelayedVehicles();
    checkForVehicleBunching();
    checkForLowRouteCoverage();
    checkForOfflineVehicles();
}
```

## Monitoring Delayed Vehicles

Let's examine how the service checks for delayed vehicles:

```java
private void checkForDelayedVehicles() {
    try {
        // Query to detect vehicles stopped for more than the threshold time
        String querySql = "SELECT " +
                "    v.vehicle_id, " +
                "    v.route_id, " +
                "    v.current_status, " +
                "    v.latitude, " +
                "    v.longitude, " +
                "    v.time_stamp, " +
                "    TIMESTAMPDIFF(MINUTE, v.time_stamp, CURRENT_TIMESTAMP) as stopped_minutes " +
                "FROM VehiclePosition v " +
                "JOIN (" +
                "    SELECT vehicle_id, MAX(time_stamp) as latest_ts " +
                "    FROM VehiclePosition " +
                "    GROUP BY vehicle_id " +
                ") l ON v.vehicle_id = l.vehicle_id AND v.time_stamp = l.latest_ts " +
                "WHERE " +
                "    v.current_status = 'STOPPED_AT' " +
                "    AND TIMESTAMPDIFF(MINUTE, v.time_stamp, CURRENT_TIMESTAMP) >= ?";

        // Execute the query with the threshold parameter
        var result = client.sql().execute(null, querySql, STOPPED_THRESHOLD_MINUTES);

        int count = 0;
        // Process each row in the result
        while (result.hasNext()) {
            var row = result.next();
            count++;

            String vehicleId = row.stringValue("vehicle_id");
            String routeId = row.stringValue("route_id");
            int stoppedMinutes = row.intValue("stopped_minutes");

            // Create and record the alert
            ServiceAlert alert = new ServiceAlert(
                    "DELAYED_VEHICLE",
                    "Vehicle " + vehicleId + " on route " + routeId +
                            " has been stopped for " + stoppedMinutes + " minutes",
                    routeId,
                    vehicleId,
                    row.doubleValue("latitude"),
                    row.doubleValue("longitude"),
                    stoppedMinutes);

            recordAlert(alert);

            // Only log the alert if not in quiet mode
            if (!quietMode) {
                System.out.println("[" + LocalDateTime.now().format(timeFormatter) +
                        "] ALERT: " + alert.getMessage());
            }
        }

        if (count > 0) {
            System.out.println("--- Found " + count + " delayed vehicles");
        }
    } catch (Exception e) {
        System.err.println("Error checking for delayed vehicles: " + e.getMessage());
    }
}
```

This method:

1. Executes an SQL query to find vehicles that:
   - Have a status of "STOPPED_AT"
   - Have been stopped for longer than the threshold time
2. Processes each result to create and record alerts
3. Updates statistics tracking
4. Handles exceptions gracefully

## Understanding the Query Structure

The query uses a Common Table Expression (CTE) with a JOIN to find the latest position for each vehicle:

```sql
SELECT v.vehicle_id, v.route_id, v.current_status, v.latitude, v.longitude, v.time_stamp, 
       TIMESTAMPDIFF(MINUTE, v.time_stamp, CURRENT_TIMESTAMP) as stopped_minutes
FROM VehiclePosition v
JOIN (
    SELECT vehicle_id, MAX(time_stamp) as latest_ts
    FROM VehiclePosition
    GROUP BY vehicle_id
) l ON v.vehicle_id = l.vehicle_id AND v.time_stamp = l.latest_ts
WHERE v.current_status = 'STOPPED_AT'
  AND TIMESTAMPDIFF(MINUTE, v.time_stamp, CURRENT_TIMESTAMP) >= ?
```

This query:

1. Creates a subquery that finds the latest timestamp for each vehicle
2. Joins with the main table to get only the most recent position data
3. Filters for vehicles with a "STOPPED_AT" status
4. Calculates how long each vehicle has been stopped
5. Filters to include only vehicles stopped longer than the threshold

## Service Alerts

The monitoring service creates `ServiceAlert` objects to represent detected issues. Let's look at the `ServiceAlert` class:

```shell
open src/main/java/com/example/transit/model/ServiceAlert.java
```

This class captures alert information:

```java
public class ServiceAlert {
    private final String type;
    private final String message;
    private final String routeId;
    private final String vehicleId;
    private final double latitude;
    private final double longitude;
    private final int severity;
    private final LocalDateTime timestamp;
    
    // Constructor and getters...
}
```

Each alert includes:

- The alert type (e.g., "DELAYED_VEHICLE")
- A human-readable message
- Related route and vehicle IDs
- Geographic coordinates
- A severity value
- A timestamp when the alert was created

## Service Monitor Example

Let's run the Service Monitor example to see it in action:

```bash
mvn compile exec:java@service-monitor-example
```

This command runs the `ServiceMonitorExample` class. Let's examine this file:

```shell
open src/main/java/com/example/transit/examples/ServiceMonitorExample.java
```

The example follows these steps:

1. Connect to the Ignite cluster
2. Verify there's data in the database
3. Create and start the monitoring service
4. Schedule a task to print statistics periodically
5. Wait for user input to stop the service
6. Display final results and statistics

The core part of the example is:

```java
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
```

When you run this example, you'll see output similar to:

```text
=== Service Monitor Example ===
Connected to Ignite cluster: [ClientClusterNode [id=269b35be-01cb-4013-9333-add1ef38e05a, name=node3, address=127.0.0.1:10802, nodeMetadata=null]]

--- Verifying database data...
Verifying data in VehiclePosition table...
Table contains 2154 records

Sample records (most recent):
Vehicle: 5730, Route: 22, Status: STOPPED_AT, Time: 2025-03-25 16:54:46
Vehicle: 1010, Route: F, Status: IN_TRANSIT_TO, Time: 2025-03-25 16:54:46
Vehicle: 5813, Route: 1, Status: IN_TRANSIT_TO, Time: 2025-03-25 16:54:46

Top routes by number of records:
Route 29: 88 records
Route 14R: 88 records
Route 49: 88 records
Route 1: 80 records
Route 22: 80 records

=== Starting monitoring service...
--- Starting service disruption monitoring (polling every 60 seconds)

=== Setting up statistics reporting...

--- Monitor is running. Press Enter to stop...
--- Found 206 delayed vehicles
--- Found 76 instances of vehicle bunching

===== SERVICE MONITORING ALERTS =====
Time: 2025-03-25 17:07:28

Alert counts by type:
• DELAYED_VEHICLE: 206 alerts
• VEHICLE_BUNCHING: 76 alerts
• LOW_ROUTE_COVERAGE: 0 alerts
• OFFLINE_VEHICLE: 0 alerts

Total alerts detected: 282
===========================================

[Press Enter to stop the monitor]

=== Stopping monitoring service...
+++ Service monitoring stopped

=== Monitoring Results ===
Total alerts detected: 282

===== SERVICE MONITORING ALERTS =====
Time: 2025-03-25 17:07:58

Alert counts by type:
• DELAYED_VEHICLE: 206 alerts
• VEHICLE_BUNCHING: 76 alerts
• LOW_ROUTE_COVERAGE: 0 alerts
• OFFLINE_VEHICLE: 0 alerts

Total alerts detected: 282
===========================================


Sample alerts:
• Vehicle 1000 on route 14R has been stopped for 89 minutes [17:07:28]
• Vehicle 1001 on route 14R has been stopped for 89 minutes [17:07:28]
• Vehicle 1006 on route F has been stopped for 89 minutes [17:07:28]
• Vehicle 1008 on route F has been stopped for 89 minutes [17:07:28]
• Vehicle 1010 on route F has been stopped for 89 minutes [17:07:28]

Example completed successfully!
```

This output shows:

1. The monitoring service starting and running
2. Alerts being detected (delayed vehicles and vehicle bunching)
3. Statistics about the total alerts detected
4. Sample alerts with their details

## Other Monitoring Conditions

The `MonitorService` class implements several other monitoring conditions, each with its own specialized SQL query:

### Vehicle Bunching

```java
private void checkForVehicleBunching() {
    // This query finds pairs of vehicles on the same route that are close to each other
    String querySql = "WITH latest_positions AS (" +
            "    SELECT v.vehicle_id, v.route_id, v.latitude, v.longitude " +
            "    FROM VehiclePosition v " +
            "    JOIN (" +
            "        SELECT vehicle_id, MAX(time_stamp) as latest_ts " +
            "        FROM VehiclePosition " +
            "        GROUP BY vehicle_id " +
            "    ) l ON v.vehicle_id = l.vehicle_id AND v.time_stamp = l.latest_ts " +
            "    WHERE v.current_status = 'IN_TRANSIT_TO' " + // Only consider moving vehicles
            ") " +
            "SELECT " +
            "    a.vehicle_id as vehicle1, " +
            "    b.vehicle_id as vehicle2, " +
            "    a.route_id, " +
            "    a.latitude as lat1, " +
            "    a.longitude as lon1, " +
            "    b.latitude as lat2, " +
            "    b.longitude as lon2, " +
            "    SQRT(POWER(a.latitude - b.latitude, 2) + POWER(a.longitude - b.longitude, 2)) * 111 as distance_km " +
            "FROM latest_positions a " +
            "JOIN latest_positions b ON a.route_id = b.route_id AND a.vehicle_id < b.vehicle_id " +
            "WHERE SQRT(POWER(a.latitude - b.latitude, 2) + POWER(a.longitude - b.longitude, 2)) * 111 < ? " +
            "ORDER BY distance_km";
    
    // ... execution and alert creation ...
}
```

This query:

1. Finds the latest position for each vehicle that's in transit
2. Self-joins the results to find pairs of vehicles on the same route
3. Calculates the distance between them in kilometers
4. Filters for pairs closer than the threshold distance

### Low Route Coverage

```java
private void checkForLowRouteCoverage() {
    String querySql = "WITH active_vehicles AS (" +
            "    SELECT DISTINCT v.route_id, v.vehicle_id " +
            "    FROM VehiclePosition v " +
            "    JOIN (" +
            "        SELECT vehicle_id, MAX(time_stamp) as latest_ts " +
            "        FROM VehiclePosition " +
            "        GROUP BY vehicle_id " +
            "    ) l ON v.vehicle_id = l.vehicle_id AND v.time_stamp = l.latest_ts " +
            "    WHERE TIMESTAMPDIFF(MINUTE, v.time_stamp, CURRENT_TIMESTAMP) < 15" + // Only consider recent positions
            ") " +
            "SELECT route_id, COUNT(*) as vehicle_count " +
            "FROM active_vehicles " +
            "GROUP BY route_id " +
            "HAVING COUNT(*) < ? " +
            "ORDER BY vehicle_count";
    
    // ... execution and alert creation ...
}
```

This query:

1. Finds the latest position for each vehicle
2. Filters for positions reported in the last 15 minutes
3. Groups by route and counts vehicles per route
4. Filters for routes with fewer than the minimum required vehicles

### Offline Vehicles

```java
private void checkForOfflineVehicles() {
    String querySql = "WITH latest_timestamps AS (" +
            "    SELECT vehicle_id, MAX(time_stamp) as latest_ts " +
            "    FROM VehiclePosition " +
            "    GROUP BY vehicle_id " +
            "), active_routes AS (" +
            "    SELECT DISTINCT route_id " +
            "    FROM VehiclePosition v " +
            "    JOIN latest_timestamps l ON v.vehicle_id = l.vehicle_id AND v.time_stamp = l.latest_ts " +
            "    WHERE TIMESTAMPDIFF(MINUTE, v.time_stamp, CURRENT_TIMESTAMP) < 15" +
            ") " +
            "SELECT v.vehicle_id, v.route_id, v.latitude, v.longitude, v.time_stamp, " +
            "       TIMESTAMPDIFF(MINUTE, v.time_stamp, CURRENT_TIMESTAMP) as offline_minutes " +
            "FROM VehiclePosition v " +
            "JOIN latest_timestamps l ON v.vehicle_id = l.vehicle_id AND v.time_stamp = l.latest_ts " +
            "WHERE v.route_id IN (SELECT route_id FROM active_routes) " + // Only check routes with some active vehicles
            "  AND TIMESTAMPDIFF(MINUTE, v.time_stamp, CURRENT_TIMESTAMP) >= ? " +
            "ORDER BY offline_minutes DESC";
    
    // ... execution and alert creation ...
}
```

This query:

1. Finds the latest timestamp for each vehicle
2. Identifies routes that have some active vehicles
3. Finds vehicles on those routes that haven't reported in longer than the threshold time
4. Calculates how long each vehicle has been offline

## Next Steps

In this module, we've implemented a service monitoring system that:

1. Continuously checks for potential service disruptions
2. Detects multiple types of issues, including:
   - Vehicles stopped for too long
   - Vehicles bunching on routes
   - Routes with insufficient coverage
   - Vehicles that have gone offline
3. Tracks statistics about detected issues
4. Provides a foundation for integration with external systems

This monitoring system transforms our transit application from a passive data collection system into an active monitoring tool that can alert operators to potential problems before they significantly impact service.

In the next module, we'll bring all the components together to create the complete transit monitoring application, including a simple dashboard to visualize the system status.

**Next Steps:** Continue to [Module 8: Putting It All Together](07-putting-together.md)
