# Building the Data Ingestion Service

In this module, you'll implement a data ingestion service that forms the backbone of your transit monitoring application. This service will continuously fetch vehicle position data from GTFS feeds and store it in Apache Ignite, creating the real-time data foundation needed for monitoring and analysis.

## Understanding Data Ingestion Requirements

Real-time transit monitoring demands a reliable data pipeline with specific characteristics:

1. **Periodic data collection**: Regularly fetching the latest data to maintain freshness
2. **Efficient data storage**: Minimizing database overhead during insertion
3. **Fault tolerance**: Handling errors without service disruption
4. **Resource management**: Properly managing connections and threads
5. **Configurable behavior**: Adjusting parameters like frequency based on requirements

## The Data Ingestion Workflow

The data ingestion workflow in our application follows these steps:

1. **Scheduled execution**: A timer triggers data collection at regular intervals
2. **Data fetching**: The GTFS client retrieves vehicle positions from the transit feed
3. **Data transformation**: Vehicle data is converted to our domain model format
4. **Batch processing**: Records are grouped into batches for efficient storage
5. **Transactional storage**: Each batch is stored atomically in the database
6. **Statistics tracking**: Metrics are collected to monitor ingestion performance

## Ingest Service Implementation

Let's examine how the `DataIngestionService` class implements this workflow. Open the file:

```shell
open src/main/java/com/example/transit/service/DataIngestionService.java
```

The core functionality is in the `fetchAndStoreData()` method, which is called periodically by a scheduler:

```java
private void fetchAndStoreData() {
    long fetchStartTime = System.currentTimeMillis();
    try {
        // Fetch the latest vehicle positions
        List<VehiclePosition> positions = gtfsService.getVehiclePositions();
        lastFetchCount.set(positions.size());
        totalFetched.addAndGet(positions.size());

        if (!positions.isEmpty()) {
            // Store the positions in the database
            int recordsStored = storeVehiclePositions(positions);
            totalStored.addAndGet(recordsStored);

            System.out.println(">>> Fetched " + positions.size() +
                    " and stored " + recordsStored +
                    " vehicle positions");
        } else {
            System.out.println("No vehicle positions fetched from feed");
        }

    } catch (Exception e) {
        logger.error("Error in data ingestion: {}", e.getMessage());
    } finally {
        lastFetchTime.set(System.currentTimeMillis() - fetchStartTime);
    }
}
```

This method:

1. Records the start time for performance measurement
2. Calls the GTFS service to fetch vehicle positions
3. Updates statistics counters
4. Calls `storeVehiclePositions()` to save the data to Ignite
5. Captures and handles any exceptions
6. Updates the timing statistics

The actual storage happens in the `storeVehiclePositions()` method:

```java
private int storeVehiclePositions(List<VehiclePosition> positions) {
    if (positions.isEmpty()) {
        return 0;
    }

    int recordsProcessed = 0;
    IgniteClient client = connectionManager.getClient();

    try {
        // Get table and record view for vehicle positions
        Table vehiclePositionsTable = client.tables().table(VehiclePosition.class.getSimpleName());
        RecordView<VehiclePosition> recordView = vehiclePositionsTable.recordView(VehiclePosition.class);

        // Process records in batches
        for (int i = 0; i < positions.size(); i += batchSize) {
            // Determine the end index for current batch
            int endIndex = Math.min(i + batchSize, positions.size());
            List<VehiclePosition> batch = positions.subList(i, endIndex);

            // Use runInTransaction to automatically handle transaction lifecycle
            client.transactions().runInTransaction(tx -> {
                recordView.upsertAll(tx, batch);
                return null;
            });

            recordsProcessed += batch.size();
        }

        return recordsProcessed;
    } catch (Exception e) {
        logger.error("Error storing vehicle positions: {}", e.getMessage());
        return recordsProcessed;
    }
}
```

This method:

1. Obtains a reference to the vehicle positions table
2. Creates a `RecordView` for type-safe operations
3. Processes records in configurable batches for efficiency
4. Uses transactions to ensure data consistency
5. Handles exceptions and returns the count of processed records

## Statistics Tracking

The service keeps track of various statistics to monitor its performance:

```java
// Statistics tracking
private final AtomicLong totalFetched = new AtomicLong(0);
private final AtomicLong totalStored = new AtomicLong(0);
private final AtomicLong lastFetchCount = new AtomicLong(0);
private final AtomicLong lastFetchTime = new AtomicLong(0);
private long startTime;
```

These statistics are exposed through the `IngestStats` class, which is an immutable data container:

```shell
open src/main/java/com/example/transit/model/IngestStats.java
```

This class provides a snapshot of the current service state:

```java
public class IngestStats {
    private final long totalFetched;
    private final long totalStored;
    private final long lastFetchCount;
    private final long lastFetchTimeMs;
    private final long runningTimeMs;
    private final boolean running;
    
    // Constructor and getters...
}
```

## Service Lifecycle Management

The `DataIngestionService` includes methods to start and stop the service:

```java
public void start(int intervalSeconds) {
    if (scheduledTask != null) {
        System.out.println("Ingestion service is already running. Stop it first before restarting.");
        return;
    }

    // Reset statistics tracking
    this.startTime = System.currentTimeMillis();
    this.totalFetched.set(0);
    this.totalStored.set(0);
    this.lastFetchCount.set(0);
    this.lastFetchTime.set(0);

    // Schedule the task to run at fixed intervals
    scheduledTask = scheduler.scheduleAtFixedRate(
            this::fetchAndStoreData,
            0,  // Start immediately
            intervalSeconds,
            TimeUnit.SECONDS);

    System.out.println("--- Data ingestion service started with "
            + intervalSeconds + " second interval");
}

public void stop() {
    if (scheduledTask != null) {
        scheduledTask.cancel(false); // Don't interrupt if running
        scheduledTask = null;

        // Properly shut down the executor service
        try {
            // Attempt to shut down gracefully
            scheduler.shutdown();
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                // Force shutdown if graceful shutdown fails
                scheduler.shutdownNow();
            }
            System.out.println("=== Data ingestion service stopped");
        } catch (InterruptedException e) {
            // If we're interrupted during shutdown, force immediate shutdown
            scheduler.shutdownNow();
            Thread.currentThread().interrupt(); // Preserve interrupt status
            System.err.println("Data ingestion service shutdown interrupted");
        }
    } else {
        System.err.println("Ingestion service is not running");
    }
}
```

These methods handle:

1. Service initialization and scheduling
2. Statistics reset on start
3. Graceful shutdown with timeout
4. Thread interrupt handling
5. Status reporting

## Ingest Service Example

Let's run the ingestion service example to see it in action:

```bash
mvn compile exec:java@ingest-example
```

This command runs the `IngestExample` class, which demonstrates a complete ingestion workflow. Let's examine this example:

```shell
open src/main/java/com/example/transit/examples/IngestExample.java
```

The example follows these steps:

1. Verify database connectivity
2. Set up the database schema if it doesn't exist
3. Start the ingestion service with a 15-second interval
4. Run for 45 seconds while showing a progress indicator
5. Display statistics after ingestion
6. Stop the ingestion service and clean up resources

The core part of the example is:

```java
// Create GTFS feed service and data ingestion service
System.out.println("\n=== Starting data ingestion service");
GtfsService feedService = new GtfsService(config.getFeedUrl());
DataIngestionService ingestService = new DataIngestionService(
        feedService, connectionManager)
        .withBatchSize(100); // Configure batch size

try {
    ingestService.start(15); // Fetch every 15 seconds
    reportingService.displayIngestionStatus(ingestService.getStatistics());

    // Wait for some data to be ingested
    System.out.println("\n=== Waiting for data ingestion (45 seconds)...");
    
    // ... (waiting code) ...
    
    // Verify data after ingestion
    System.out.println("\n--- Data state after ingestion");
    reportingService.displaySystemStatistics();

    // Display ingestion statistics
    ingestService.printStatistics();
} finally {
    // Always stop the ingestion service before exiting
    System.out.println("\n=== Stopping data ingestion service");
    ingestService.stop();
    reportingService.displayIngestionStatus(ingestService.getStatistics());
}
```

When you run this example, you'll see output similar to:

```text
=== Data Ingestion Service Example ===
--- Setting up database schema
--- Vehicle positions table already exists

--- Initial data state
Verifying data in VehiclePosition table
<<< Table contains 1532 records

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

=== Starting data ingestion service
--- Data ingestion service started with 15 second interval
• Status: Running
• Records fetched: 0
• Records stored: 0
• Last fetch count: 0
• Running time: 00:00:00

=== Waiting for data ingestion (45 seconds)...
--- Data ingestion wait complete!

--- Data state after ingestion
• Total records: 2068
• Unique vehicles: 536
• Oldest record: 2025-03-25 16:54:46
• Newest record: 2025-03-25 17:01:11
• Data collection active

=== Ingestion Statistics ===
• Status: Running
• Running time: 00:00:45
• Total records fetched: 536
• Total records stored: 536
• Last fetch count: 536
• Last fetch time: 1253ms
• Ingestion rate: 11.91 records/second
============================

=== Stopping data ingestion service
=== Data ingestion service stopped
• Status: Stopped
• Records fetched: 536
• Records stored: 536
• Last fetch count: 536
• Last fetch time: 1253ms
• Running time: 00:00:46

=== Example completed successfully!
```

This output shows:

1. The initial database state before ingestion
2. The ingestion service starting and running
3. Statistics about the data after ingestion
4. Performance metrics like fetch time and ingestion rate

## Next Steps

In this module, you've implemented a robust data ingestion service that:

1. Periodically fetches vehicle position data from a GTFS-realtime feed
2. Efficiently stores this data in Apache Ignite using batch processing
3. Handles errors gracefully to ensure continuous operation
4. Provides statistics to monitor the ingestion process
5. Manages resources properly through a clean lifecycle

This service forms the backbone of our transit monitoring system, ensuring our database is constantly updated with the latest vehicle positions.

In the next module, we'll build on this foundation by implementing a monitoring service that detects service disruptions in real-time.

**Next Steps:** Continue to [Module 6: Implementing a Service Monitor](06-continuous-query.md)
