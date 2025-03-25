package com.example.transit.service;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.catalog.ColumnType;
import org.apache.ignite.catalog.definitions.ColumnDefinition;
import org.apache.ignite.catalog.definitions.TableDefinition;
import org.apache.ignite.table.Table;

/**
 * Service responsible for creating and maintaining the database schema.
 * This class provides methods to set up tables for the transit monitoring system
 * using Apache Ignite 3 Catalog API.
 */
public class SchemaService {
    private static final String VEHICLE_POSITIONS_TABLE = "vehicle_positions";
    private final ConnectService connectionService;

    /**
     * Creates a new schema setup service using the provided connection service.
     *
     * @param connectionService Service that provides Ignite client connections
     */
    public SchemaService(ConnectService connectionService) {
        this.connectionService = connectionService;
    }

    /**
     * Creates the database schema for storing vehicle position data.
     * This method is idempotent and can be safely called multiple times.
     *
     * @return true if the schema setup was successful, false otherwise
     */
    public boolean createSchema() {
        try {
            IgniteClient client = connectionService.getClient();

            if (tableExists(client, VEHICLE_POSITIONS_TABLE)) {
                System.out.println(">>> Vehicle positions table already exists.");
                return true;
            }

            return createVehiclePositionsTable(client);
        } catch (Exception e) {
            logError("Failed to create schema", e);
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
    private boolean tableExists(IgniteClient client, String tableName) {
        try {
            return client.tables().table(tableName) != null;
        } catch (Exception e) {
            System.out.println("+++ Table does not exist: " + e.getMessage());
            return false;
        }
    }

    /**
     * Creates the vehicle positions table with appropriate columns and primary key.
     *
     * @param client Ignite client
     * @return true if creation was successful, false otherwise
     */
    private boolean createVehiclePositionsTable(IgniteClient client) {
        try {
            TableDefinition tableDefinition = buildVehiclePositionsTableDefinition();
            System.out.println("--- Creating table: " + tableDefinition);

            Table table = client.catalog().createTable(tableDefinition);
            System.out.println("+++ Table created successfully: " + table.name());
            return true;
        } catch (Exception e) {
            logError("Failed to create vehicle positions table", e);
            return false;
        }
    }

    /**
     * Builds the table definition for vehicle positions.
     *
     * @return TableDefinition for vehicle positions
     */
    private TableDefinition buildVehiclePositionsTableDefinition() {
        return TableDefinition.builder(VEHICLE_POSITIONS_TABLE)
                .ifNotExists()
                .columns(
                        ColumnDefinition.column("vehicle_id", ColumnType.VARCHAR),
                        ColumnDefinition.column("route_id", ColumnType.VARCHAR),
                        ColumnDefinition.column("latitude", ColumnType.DOUBLE),
                        ColumnDefinition.column("longitude", ColumnType.DOUBLE),
                        ColumnDefinition.column("time_stamp", ColumnType.TIMESTAMP),
                        ColumnDefinition.column("current_status", ColumnType.VARCHAR))
                // Define a composite primary key on vehicle_id and time_stamp
                // This enables efficient queries for a vehicle's history
                .primaryKey("vehicle_id", "time_stamp")
                .build();
    }

    /**
     * Logs an error message along with exception details.
     *
     * @param message Error message
     * @param e Exception that occurred
     */
    private void logError(String message, Exception e) {
        System.err.println(message + ": " + e.getMessage());
        Throwable cause = e;
        while (cause != null) {
            System.err.println("  Caused by: " + cause.getClass().getName() + ": " + cause.getMessage());
            cause = cause.getCause();
        }
        e.printStackTrace();
    }
}