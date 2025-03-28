package com.example.transit;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.catalog.ColumnType;
import org.apache.ignite.catalog.definitions.ColumnDefinition;
import org.apache.ignite.catalog.definitions.TableDefinition;
import org.apache.ignite.table.Table;

/**
 * Creates and maintains the database schema for the transit monitoring system.
 * This class handles the creation of tables using Ignite's Catalog API.
 */
public class SchemaSetup {
    /**
     * Creates the database schema for storing vehicle position data.
     * This method is idempotent and can be safely run multiple times.
     *
     * @return true if the schema setup was successful
     */
    public boolean createSchema() {
        try {
            // Get the client connection
            IgniteClient client = IgniteConnection.getClient();

            // Check if table already exists
            boolean tableExists = false;
            try {
                // Try to directly get the table
                var table = client.tables().table("vehicle_positions");
                if (table != null) {
                    tableExists = true;
                    System.out.println("Vehicle positions table already exists. Schema setup complete.");
                }
            } catch (Exception e) {
                System.out.println("Table does not exist, will create it: " + e.getMessage());
                // Continue with table creation
            }

            if (!tableExists) {
                // Define and create the table
                TableDefinition tableDefinition = TableDefinition.builder("vehicle_positions")
                        .ifNotExists()
                        .record(VehiclePosition.class)
                        // Define a composite primary key on vehicle_id and time_stamp
                        // This enables efficient queries for a vehicle's history
                        .primaryKey("vehicle_id", "time_stamp")
                        .build();

                System.out.println("Creating table using Catalog API: " + tableDefinition);
                Table table = client.catalog().createTable(tableDefinition);
                System.out.println("Vehicle positions table created successfully: " + table.name());
            }

            return true;
        } catch (Exception e) {
            System.err.println("Failed to create schema: " + e.getMessage());
            Throwable cause = e;
            while (cause != null) {
                System.err.println("  Caused by: " + cause.getClass().getName() + ": " + cause.getMessage());
                cause = cause.getCause();
            }
            e.printStackTrace();
            return false;
        }
    }
}