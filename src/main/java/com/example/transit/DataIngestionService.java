package com.example.transit;

import org.apache.ignite.client.IgniteClient;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service responsible for periodically fetching transit data and storing it in Ignite.
 * Implements a resilient data ingestion pipeline with configurable scheduling.
 *
 * This service uses a scheduled executor to periodically fetch data from the GTFS
 * feed and store it in the Ignite database using batch processing for efficiency.
 */
public class DataIngestionService {
    private final GTFSFeedClient feedClient;
    private final IgniteClient igniteClient;
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduledTask;
    private int batchSize = 100; // Default batch size

    // Statistics tracking
    private final AtomicLong totalFetched = new AtomicLong(0);
    private final AtomicLong totalStored = new AtomicLong(0);
    private final AtomicLong lastFetchCount = new AtomicLong(0);
    private final AtomicLong lastFetchTime = new AtomicLong(0);
    private long startTime;

    /**
     * Constructs a new data ingestion service.
     *
     * @param feedUrl The URL of the GTFS realtime feed
     */
    public DataIngestionService(String feedUrl) {
        this.feedClient = new GTFSFeedClient(feedUrl);
        this.igniteClient = IgniteConnection.getClient();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "data-ingestion-thread");
            t.setDaemon(true);
            // Make the thread respond better to interrupts
            t.setUncaughtExceptionHandler((thread, ex) -> {
                System.err.println("Uncaught exception in " + thread.getName() + ": " + ex.getMessage());
                ex.printStackTrace();
            });
            return t;
        });
    }

    /**
     * Sets the batch size for database operations.
     * Larger batch sizes can improve performance but consume more memory.
     *
     * @param batchSize Number of records to process in each batch
     * @return This DataIngestionService instance for method chaining
     */
    public DataIngestionService withBatchSize(int batchSize) {
        if (batchSize < 1) {
            throw new IllegalArgumentException("Batch size must be at least 1");
        }
        this.batchSize = batchSize;
        return this;
    }

    /**
     * Starts the data ingestion service with the specified interval.
     *
     * @param intervalSeconds The interval between data fetches in seconds
     */
    public void start(int intervalSeconds) {
        if (scheduledTask != null) {
            System.out.println("Ingestion service is already running. Stop it first before restarting.");
            return;
        }

        this.startTime = System.currentTimeMillis();

        // Reset statistics
        totalFetched.set(0);
        totalStored.set(0);
        lastFetchCount.set(0);
        lastFetchTime.set(0);

        // Schedule the task with initial delay of 0 (start immediately)
        scheduledTask = scheduler.scheduleAtFixedRate(
                this::fetchAndStoreData,
                0,
                intervalSeconds,
                TimeUnit.SECONDS
        );

        System.out.println("Data ingestion service started with "
                + intervalSeconds + " second interval");
    }

    /**
     * Stops the data ingestion service and cleans up resources.
     */
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
                System.out.println("Data ingestion service stopped");
            } catch (InterruptedException e) {
                // If we're interrupted during shutdown, force immediate shutdown
                scheduler.shutdownNow();
                Thread.currentThread().interrupt(); // Preserve interrupt status
                System.out.println("Data ingestion service shutdown interrupted");
            }

            printStatistics();
        } else {
            System.out.println("Ingestion service is not running");
        }
    }

    /**
     * Fetches data from the GTFS feed and stores it in Ignite.
     * This method is called periodically by the scheduler.
     */
    private void fetchAndStoreData() {
        long fetchStartTime = System.currentTimeMillis();
        try {
            // Step 1: Fetch the latest vehicle positions
            List<VehiclePosition> positions = feedClient.getVehiclePositions();
            lastFetchCount.set(positions.size());
            totalFetched.addAndGet(positions.size());

            if (!positions.isEmpty()) {
                // Step 2: Store the positions in the database
                int recordsStored = storeVehiclePositions(positions);
                totalStored.addAndGet(recordsStored);

                System.out.println("Fetched " + positions.size() +
                        " and stored " + recordsStored +
                        " vehicle positions");
            } else {
                System.out.println("No vehicle positions fetched from feed");
            }

        } catch (Exception e) {
            System.err.println("Error in data ingestion: " + e.getMessage());
            e.printStackTrace();
        } finally {
            lastFetchTime.set(System.currentTimeMillis() - fetchStartTime);
        }
    }

    /**
     * Stores vehicle positions in Ignite using efficient batch processing.
     * Each batch is processed in a single transaction for atomicity.
     *
     * @param positions List of vehicle positions to store
     * @return Number of records successfully stored
     */
    private int storeVehiclePositions(List<VehiclePosition> positions) {
        if (positions.isEmpty()) {
            return 0;
        }

        int recordsProcessed = 0;

        try {
            // Process records in batches
            for (int i = 0; i < positions.size(); i += batchSize) {
                // Determine the end index for current batch
                int endIndex = Math.min(i + batchSize, positions.size());
                List<VehiclePosition> batch = positions.subList(i, endIndex);

                // Create a transaction for each batch
                var tx = igniteClient.transactions().begin();

                try {
                    // Insert all records in the current batch
                    for (VehiclePosition position : batch) {
                        // Convert epoch milliseconds to LocalDateTime for Ignite
                        LocalDateTime timestamp = LocalDateTime.ofInstant(
                                position.getTimestampAsInstant(),
                                ZoneId.systemDefault()
                        );

                        // Use SQL API to execute insert within transaction
                        igniteClient.sql().execute(tx,
                                "INSERT INTO vehicle_positions " +
                                        "(vehicle_id, route_id, latitude, longitude, time_stamp, current_status) " +
                                        "VALUES (?, ?, ?, ?, ?, ?)",
                                position.getVehicleId(),
                                position.getRouteId(),
                                position.getLatitude(),
                                position.getLongitude(),
                                timestamp,
                                position.getCurrentStatus()
                        );
                    }

                    // Commit the transaction for this batch
                    tx.commit();
                    recordsProcessed += batch.size();

                } catch (Exception e) {
                    // If there was an error, try to roll back the transaction
                    try {
                        tx.rollback();
                    } catch (Exception rollbackEx) {
                        System.err.println("Error rolling back transaction: " + rollbackEx.getMessage());
                    }
                    throw e; // Re-throw to be caught by outer catch
                }
            }

            return recordsProcessed;
        } catch (Exception e) {
            System.err.println("Error storing vehicle positions: " + e.getMessage());
            e.printStackTrace();
            return recordsProcessed;
        }
    }

    /**
     * Returns a snapshot of current ingestion statistics.
     *
     * @return Map containing statistic values
     */
    public IngestStats getStatistics() {
        long runningTimeMs = System.currentTimeMillis() - startTime;

        return new IngestStats(
                totalFetched.get(),
                totalStored.get(),
                lastFetchCount.get(),
                lastFetchTime.get(),
                runningTimeMs,
                scheduledTask != null
        );
    }

    /**
     * Prints current statistics to the console.
     */
    public void printStatistics() {
        IngestStats stats = getStatistics();

        System.out.println("\n=== Data Ingestion Statistics ===");
        System.out.println("• Status: " + (stats.isRunning() ? "Running" : "Stopped"));
        System.out.println("• Running time: " + formatDuration(stats.getRunningTimeMs()));
        System.out.println("• Total records fetched: " + stats.getTotalFetched());
        System.out.println("• Total records stored: " + stats.getTotalStored());
        System.out.println("• Last fetch count: " + stats.getLastFetchCount());
        System.out.println("• Last fetch time: " + stats.getLastFetchTimeMs() + "ms");

        // Calculate rates if we have data
        if (stats.getRunningTimeMs() > 0 && stats.getTotalFetched() > 0) {
            double recordsPerSecond = stats.getTotalFetched() * 1000.0 / stats.getRunningTimeMs();
            System.out.println("• Ingestion rate: " + String.format("%.2f", recordsPerSecond) + " records/second");
        }
        System.out.println("==============================\n");
    }

    /**
     * Formats milliseconds into a human-readable duration string.
     */
    private String formatDuration(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        seconds %= 60;
        minutes %= 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    /**
     * Immutable class representing ingestion statistics at a point in time.
     */
    public static class IngestStats {
        private final long totalFetched;
        private final long totalStored;
        private final long lastFetchCount;
        private final long lastFetchTimeMs;
        private final long runningTimeMs;
        private final boolean running;

        public IngestStats(long totalFetched, long totalStored, long lastFetchCount,
                           long lastFetchTimeMs, long runningTimeMs, boolean running) {
            this.totalFetched = totalFetched;
            this.totalStored = totalStored;
            this.lastFetchCount = lastFetchCount;
            this.lastFetchTimeMs = lastFetchTimeMs;
            this.runningTimeMs = runningTimeMs;
            this.running = running;
        }

        // Getters
        public long getTotalFetched() { return totalFetched; }
        public long getTotalStored() { return totalStored; }
        public long getLastFetchCount() { return lastFetchCount; }
        public long getLastFetchTimeMs() { return lastFetchTimeMs; }
        public long getRunningTimeMs() { return runningTimeMs; }
        public boolean isRunning() { return running; }
    }
}