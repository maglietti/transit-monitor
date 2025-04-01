package com.example.transit.model;

/**
 * Immutable class representing ingestion statistics at a point in time.
 * This class provides information about the data ingestion process,
 * including counts of fetched and stored records, timing information,
 * and service status.
 */
public class IngestStats {
    private final long totalFetched;
    private final long totalStored;
    private final long lastFetchCount;
    private final long lastFetchTimeMs;
    private final long runningTimeMs;
    private final boolean running;

    /**
     * Creates a new instance of ingestion statistics.
     *
     * @param totalFetched Total number of records fetched since start
     * @param totalStored Total number of records stored in the database
     * @param lastFetchCount Number of records in the most recent fetch
     * @param lastFetchTimeMs Time in milliseconds taken by the last fetch operation
     * @param runningTimeMs Total running time of the ingestion service in milliseconds
     * @param running Whether the ingestion service is currently running
     */
    public IngestStats(long totalFetched, long totalStored, long lastFetchCount,
            long lastFetchTimeMs, long runningTimeMs, boolean running) {
        this.totalFetched = totalFetched;
        this.totalStored = totalStored;
        this.lastFetchCount = lastFetchCount;
        this.lastFetchTimeMs = lastFetchTimeMs;
        this.runningTimeMs = runningTimeMs;
        this.running = running;
    }

    /**
     * Gets the total number of records fetched.
     *
     * @return Total fetch count
     */
    public long getTotalFetched() {
        return totalFetched;
    }

    /**
     * Gets the total number of records stored.
     *
     * @return Total stored count
     */
    public long getTotalStored() {
        return totalStored;
    }

    /**
     * Gets the number of records fetched in the most recent operation.
     *
     * @return Last fetch count
     */
    public long getLastFetchCount() {
        return lastFetchCount;
    }

    /**
     * Gets the time in milliseconds taken by the last fetch operation.
     *
     * @return Last fetch time in milliseconds
     */
    public long getLastFetchTimeMs() {
        return lastFetchTimeMs;
    }

    /**
     * Gets the total running time of the ingestion service.
     *
     * @return Running time in milliseconds
     */
    public long getRunningTimeMs() {
        return runningTimeMs;
    }

    /**
     * Checks if the ingestion service is currently running.
     *
     * @return true if the service is running, false otherwise
     */
    public boolean isRunning() {
        return running;
    }
}
