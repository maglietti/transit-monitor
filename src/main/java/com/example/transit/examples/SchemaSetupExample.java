package com.example.transit.examples;

import com.example.transit.config.IgniteConnectionManager;
import com.example.transit.config.VehiclePositionTableManager;
import com.example.transit.model.VehiclePosition;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.table.RecordView;
import org.apache.ignite.table.Table;

import java.time.LocalDateTime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Example demonstrating table creation and basic CRUD operations.
 * Creates the vehicle position table and performs test operations.
 */
public class SchemaSetupExample {

    private static final Logger logger = LogManager.getLogger(SchemaSetupExample.class);

    private static final String VEHICLE_TABLE = VehiclePosition.class.getSimpleName();

    public static void main(String[] args) {

        // Handle JUL logging
        java.util.logging.LogManager.getLogManager().reset();
        org.apache.logging.log4j.jul.LogManager.getLogManager();
        java.util.logging.Logger.getLogger("").setLevel(java.util.logging.Level.WARNING);

        System.out.println("=== Schema Setup Example ===");

        try (IgniteConnectionManager connectionManager = new IgniteConnectionManager()) {
            IgniteClient client = connectionManager.getClient();
            System.out.println("--- Connected to Ignite cluster");

            // Create schema using the vehicle position table manager
            System.out.println("=== Creating vehicle positions schema");
            VehiclePositionTableManager tableManager = new VehiclePositionTableManager(connectionManager);

            if (tableManager.createSchema()) {
                testTableOperations(client);
            } else {
                System.err.println("Table setup failed.");
            }
        } catch (Exception e) {
            logger.error("Error: {}", e.getMessage());
        }

        System.out.println("=== Example completed");
    }

    /**
     * Test CRUD operations on the vehicle position table.
     */
    private static void testTableOperations(IgniteClient client) {

        System.out.println("=== Testing table operations");

        try {
            // Create test data
            LocalDateTime now = LocalDateTime.now();
            VehiclePosition testVehicle = new VehiclePosition(
                    "test-vehicle-1",
                    now,
                    "test-route-100",
                    47.6062,
                    -122.3321,
                    "STOPPED_AT"
            );

            // Get table and record view
            Table vehicleTable = client.tables().table(VEHICLE_TABLE);
            RecordView<VehiclePosition> vehicleView = vehicleTable.recordView(VehiclePosition.class);

            // Insert test record
            System.out.println(">>> Inserting test vehicle: " + testVehicle.getVehicleId());
            vehicleView.upsert(null, testVehicle);

            // Retrieve the record
            VehiclePosition keyVehicle = new VehiclePosition();
            keyVehicle.setVehicleId(testVehicle.getVehicleId());
            keyVehicle.setTimestamp(testVehicle.getTimestamp());

            VehiclePosition retrievedVehicle = vehicleView.get(null, keyVehicle);
            if (retrievedVehicle != null) {
                System.out.println("<<< Retrieved vehicle: " + retrievedVehicle.getVehicleId() +
                        " on route " + retrievedVehicle.getRouteId() +
                        " status " + retrievedVehicle.getCurrentStatus());
            } else {
                System.out.println("!!! Vehicle not found");
            }

            // Update the vehicle status
            if (retrievedVehicle != null) {
                System.out.println(">>> Updating vehicle status");
                retrievedVehicle.setCurrentStatus("IN_TRANSIT_TO");
                vehicleView.upsert(null, retrievedVehicle);

                // Verify update
                VehiclePosition updatedVehicle = vehicleView.get(null, keyVehicle);
                if (updatedVehicle != null) {
                    System.out.println("<<< Updated vehicle: " + updatedVehicle.getVehicleId() +
                            " status is now " + updatedVehicle.getCurrentStatus());
                }
            }

            // Delete the record
            System.out.println(">>> Deleting test vehicle");
            vehicleView.delete(null, keyVehicle);

            // Verify deletion with SQL
            var countResult = client.sql().execute(null,
                    "SELECT COUNT(*) as cnt FROM " + VEHICLE_TABLE +
                            " WHERE vehicle_id = ?", testVehicle.getVehicleId());

            long count = 0;
            if (countResult.hasNext()) {
                count = countResult.next().longValue("cnt");
            }

            System.out.println("<<< Records remaining: " + count);

        } catch (Exception e) {
            logger.error("Table operations failed: {}", e.getMessage());
        }
    }
}