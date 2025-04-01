# Building and Testing the GTFS Client

In this module, you'll implement a client that communicates with GTFS-realtime feeds to fetch transit vehicle positions. This client serves as the data acquisition layer of your application, providing a clean interface between external transit data and your Ignite database.

## Obtaining an API Token

To access real transit data, you'll need an API token from a transit data provider. For this tutorial, we'll use the San Francisco Bay Area's 511.org API, which provides GTFS-realtime data for multiple transit agencies.

Follow these steps to obtain an API token:

1. Visit <https://511.org/open-data/token>
2. Complete the registration form with your details
3. Submit the form
4. Save the API token that's emailed to you

> [!note]
> The process of obtaining an API token is similar for most transit data providers. If you want to use data from a different agency, check their developer portal for instructions on getting access.

## Configuring Environment Variables

To securely manage API tokens and other configuration without hardcoding them in our source code, we'll use environment variables loaded from a `.env` file.

Create a file named `.env` in the root of your project with the following content, replacing `your_token_here` with your actual API token:

```conf
# 511.org API token - get yours at https://511.org/open-data/token
API_TOKEN=your_token_here

# GTFS Feed URL
GTFS_BASE_URL=https://api.511.org/transit/vehiclepositions

# GTFS Agency - default is San Francisco Muni
GTFS_AGENCY=SF
```

> [!caution]
> Never commit your `.env` file to version control. Add it to your `.gitignore` file to prevent accidentally exposing your API credentials.

## Understanding the Client's Role

The GTFS client is responsible for:

1. **External Data Acquisition**: Connecting to a GTFS-realtime feed provided by a transit agency
2. **Protocol Buffer Processing**: Parsing the complex binary format used by GTFS-realtime
3. **Data Transformation**: Converting external data structures into our domain model
4. **Error Handling**: Dealing robustly with network issues, data format changes, and other potential problems

## Configuration Service

Let's first understand how our application loads configuration from the `.env` file. Open the `ConfigService.java` file:

```shell
open src/main/java/com/example/transit/config/ConfigService.java
```

The `ConfigService` uses the `dotenv-java` library to load environment variables from the `.env` file. The key method is:

```java
// Load environment variables from .env file
Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

// Get required config values
this.apiToken = dotenv.get(API_TOKEN_KEY);
this.baseUrl = dotenv.get(BASE_URL_KEY);
this.agency = dotenv.get(AGENCY_KEY);

// Pre-build the feed URL
if (isValid()) {
    this.feedUrl = String.format("%s?api_key=%s&agency=%s", baseUrl, apiToken, agency);
} else {
    this.feedUrl = null;
}
```

This code:

1. Loads environment variables from the `.env` file
2. Retrieves API token, base URL, and agency values
3. Constructs the complete feed URL with query parameters
4. Provides methods to access these values throughout the application

## Implementing the GTFS Client

The GTFS client is implemented in the `GtfsService` class. Let's examine it:

```shell
open src/main/java/com/example/transit/service/GtfsService.java
```

This class is responsible for fetching and parsing transit data from the GTFS feed. The core of the implementation is the `getVehiclePositions()` method:

```java
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
```

This method:

1. Opens a connection to the GTFS feed URL
2. Uses the `FeedMessage.parseFrom()` method from the GTFS library to parse the binary Protocol Buffer data
3. Iterates through each entity in the feed
4. Extracts relevant fields (vehicle ID, route ID, position, timestamp, status)
5. Converts timestamps to `LocalDateTime` objects for compatibility with Ignite
6. Creates `VehiclePosition` objects that match our database schema
7. Handles errors appropriately with detailed messages

The `mapVehicleStatus()` method converts GTFS enum values to readable string values:

```java
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
```

This mapping ensures our database stores consistent status values.

## Testing the GTFS Client

Let's run the GTFS feed example to test our client:

```bash
mvn compile exec:java@gtfs-feed-example
```

This runs the `GtfsFeedExample` class, which demonstrates fetching and analyzing transit data. Let's explore this example:

```shell
open src/main/java/com/example/transit/examples/GtfsFeedExample.java
```

The example follows these steps:

1. Load configuration from the `.env` file
2. Connect to the Ignite cluster
3. Set up the schema if it doesn't exist
4. Create the GTFS feed service
5. Fetch vehicle positions from the feed
6. Display sample data
7. Analyze the data to show statistics

The core part of this example is:

```java
// Create the feed service
GtfsService feedService = new GtfsService(config.getFeedUrl());

try {
    // Fetch vehicle positions
    System.out.println("=== Fetching vehicle positions...");
    List<VehiclePosition> positions = feedService.getVehiclePositions();

    System.out.println(">>> Fetched " + positions.size() + " vehicle positions from feed");

    if (positions.isEmpty()) {
        System.out.println("Warning: No vehicle positions found in the feed.");
        System.out.println("This could indicate an issue with the feed URL, API token, or the agency may not have active vehicles at this time.");
        return;
    }

    // Print sample data (first 5 vehicles)
    System.out.println("\nSample data (first 5 vehicles):");
    positions.stream()
            .limit(5)
            .forEach(pos -> System.out.println(reportingService.formatVehicleData(pos)));

    // Analyze the data
    reportingService.analyzeVehicleData(positions);
}
```

When you run this example, you'll see output similar to:

```text
=== GTFS Feed Example ===
+++ Using GTFS feed URL: https://api.511.org/transit/vehiclepositions?api_key=[API_TOKEN]&agency=SF
Connected to Ignite cluster: [ClientClusterNode [id=269b35be-01cb-4013-9333-add1ef38e05a, name=node3, address=127.0.0.1:10802, nodeMetadata=null]]
--- Vehicle positions table already exists
=== Fetching vehicle positions...
>>> Fetched 536 vehicle positions from feed
=== Success!

Sample data (first 5 vehicles):
+++ Vehicle 1006 on route F at (37.798801, -122.397285) - Status: IN_TRANSIT_TO
+++ Vehicle 1010 on route F at (37.758701, -122.427879) - Status: IN_TRANSIT_TO
+++ Vehicle 1051 on route F at (37.793968, -122.395416) - Status: IN_TRANSIT_TO
+++ Vehicle 1056 on route F at (37.781357, -122.411392) - Status: INCOMING_AT
+++ Vehicle 1057 on route F at (37.808189, -122.416672) - Status: IN_TRANSIT_TO

=== Transit System Statistics ===
• Unique routes: 56
• Unique vehicles: 536

Vehicle status distribution:
• IN_TRANSIT_TO: 204 vehicles (38.1%)
• STOPPED_AT: 220 vehicles (41.0%)
• INCOMING_AT: 112 vehicles (20.9%)

Top 5 routes by vehicle count:
• Route 49: 22 vehicles
• Route 14R: 22 vehicles
• Route 29: 22 vehicles
• Route 1: 20 vehicles
• Route 22: 19 vehicles

Geographic coverage:
• Latitude range: 37.705257415771484 to 37.81974411010742
• Longitude range: -122.50987243652344 to -122.36726379394531
Ignite client connection closed
=== GTFS Feed Example Completed
```

The actual output will vary depending on the current state of the transit system.

## Analyzing the Data

The example includes data analysis functionality implemented in the `ReportingService` class. Open it to see how it works:

```shell
open src/main/java/com/example/transit/service/ReportingService.java
```

The `analyzeVehicleData()` method provides statistics about the transit system:

```java
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
    
    // Display more statistics...
}
```

This method uses Java streams to:

1. Count unique routes and vehicles
2. Group vehicles by status type
3. Find the most active routes
4. Calculate geographic bounds of the transit system

These statistics help understand the current state of the transit system and verify that our data acquisition is working correctly.

## Next Steps

In this module, you've built and tested a robust GTFS client that forms the data acquisition layer of our transit monitoring system. This client handles the complexities of connecting to external data sources, parsing protocol buffer formats, and transforming the data into our domain model.

In the next module, we'll implement a data ingestion service that uses this client to regularly fetch transit data and store it in our Ignite database, bringing our monitoring system to life.

**Next Steps:** Continue to [Module 5: Building the Data Ingestion Service](05-data-ingestion.md)
