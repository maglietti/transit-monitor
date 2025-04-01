package com.example.transit.config;

import com.example.transit.model.VehiclePosition;
import org.apache.ignite.catalog.definitions.ZoneDefinition;
import org.apache.ignite.client.IgniteClient;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Manages the creation and verification of the vehicle position table in Ignite.
 */
public class VehiclePositionTableManager {
    private static final Logger logger = LogManager.getLogger(VehiclePositionTableManager.class);
    private static final String VEHICLE_POSITIONS_TABLE = VehiclePosition.class.getSimpleName();
    private final IgniteConnectionManager connectionManager;

    /**
     * Creates a new table manager with the given connection manager.
     */
    public VehiclePositionTableManager(IgniteConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    /**
     * Creates the database schema needed for storing vehicle positions.
     * This includes creating a zone and table if they don't exist.
     *
     * @return true if successful, false otherwise
     */
    public boolean createSchema() {
        try {
            IgniteClient client = connectionManager.getClient();

            // Check if table exists
            if (tableExists(client, VEHICLE_POSITIONS_TABLE)) {
                System.out.println("--- Vehicle positions table already exists");
                return true;
            }

            // Create zone if it doesn't exist
            System.out.println(">>> Creating 'transit' zone if it doesn't exist");
            ZoneDefinition transitZone = ZoneDefinition.builder("transit")
                    .ifNotExists()
                    .replicas(2)
                    .storageProfiles("default")
                    .build();
            client.catalog().createZone(transitZone);

            // Create table
            System.out.println(">>> Creating table: " + VEHICLE_POSITIONS_TABLE);
            client.catalog().createTable(VehiclePosition.class);

            return true;
        } catch (Exception e) {
            logger.error("Failed to create schema: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Checks if a table exists in the Ignite catalog.
     *
     * @param client Ignite client
     * @param tableName Name of the table to check
     * @return true if the table exists, false otherwise
     */
    public boolean tableExists(IgniteClient client, String tableName) {
        try {
            return client.tables().table(tableName) != null;
        } catch (Exception e) {
            return false;
        }
    }
}