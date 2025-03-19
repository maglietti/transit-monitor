package com.example.transit;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.table.RecordView;
import org.apache.ignite.table.Table;
import org.apache.ignite.table.Tuple;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Test class to verify database connectivity and schema creation.
 * This class demonstrates how to connect to an Ignite cluster and set up
 * the table for the transit monitoring system.
 */
public class SchemaSetupTest {

    public static void main(String[] args) {
        System.out.println("Starting schema setup test...");

        // Test connection to Ignite cluster
        try {
            // Get the client connection
            IgniteClient client = IgniteConnection.getClient();
            System.out.println("Successfully connected to Ignite cluster");

            // Create schema for transit data
            SchemaSetup schemaSetup = new SchemaSetup();
            boolean success = schemaSetup.createSchema();

            if (success) {
                // Verify the table was created by querying it
                System.out.println("Verifying table creation...");
                try {
                    // Get a reference to the vehicle_positions table
                    Table table = client.tables().table("vehicle_positions");
                    System.out.println("Table: " + table.name());
                    System.out.println("Table creation successful.");

                    // Create a VehiclePosition object for testing
                    long currentTime = System.currentTimeMillis();
                    VehiclePosition testVehicle = new VehiclePosition(
                            "test-vehicle-1",
                            "test-route-100",
                            47.6062,
                            -122.3321,
                            currentTime,
                            "STOPPED"
                    );

                    // Convert timestamp to LocalDateTime expected by Ignite
                    LocalDateTime localDateTime = LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(testVehicle.getTimestamp()),
                            ZoneId.systemDefault()
                    );

                    // Use RecordView with Tuple approach for insert
                    RecordView<Tuple> recordView = table.recordView();

                    // Create a tuple with field names that match database columns
                    Tuple vehicleTuple = Tuple.create()
                            .set("vehicle_id", testVehicle.getVehicleId())
                            .set("route_id", testVehicle.getRouteId())
                            .set("latitude", testVehicle.getLatitude())
                            .set("longitude", testVehicle.getLongitude())
                            .set("time_stamp", localDateTime)  // Use LocalDateTime, not Instant
                            .set("current_status", testVehicle.getCurrentStatus());

                    // Insert test data using the recordView
                    recordView.insert(null, vehicleTuple);
                    System.out.println("Test record inserted successfully: " + testVehicle);

                    // Use SQL approach to query
                    String querySql = "SELECT vehicle_id, route_id, latitude, longitude, " +
                            "time_stamp, current_status FROM vehicle_positions WHERE vehicle_id = ?";

                    var resultSet = client.sql().execute(null, querySql, testVehicle.getVehicleId());

                    List<VehiclePosition> results = new ArrayList<>();
                    resultSet.forEachRemaining(row -> {
                        // Extract timestamp milliseconds from LocalDateTime
                        LocalDateTime resultDateTime = row.value("time_stamp");
                        // Convert LocalDateTime to Instant (requires a ZoneId)
                        Instant instant = resultDateTime.atZone(ZoneId.systemDefault()).toInstant();
                        long timestamp = instant.toEpochMilli();

                        VehiclePosition position = new VehiclePosition(
                                row.stringValue("vehicle_id"),
                                row.stringValue("route_id"),
                                row.doubleValue("latitude"),
                                row.doubleValue("longitude"),
                                timestamp,
                                row.stringValue("current_status")
                        );
                        results.add(position);
                        System.out.println("Found test record: " + position);
                    });

                    System.out.println("Retrieved " + results.size() + " vehicle position records");

                    // Use SQL for delete
                    String deleteSql = "DELETE FROM vehicle_positions WHERE vehicle_id = ?";
                    client.sql().execute(null, deleteSql, testVehicle.getVehicleId());
                    System.out.println("Test record deleted successfully.");

                    // Verify deletion using SQL
                    long count = 0;
                    var verifyDeleteRs = client.sql().execute(null,
                            "SELECT COUNT(*) as cnt FROM vehicle_positions WHERE vehicle_id = ?",
                            testVehicle.getVehicleId());

                    if (verifyDeleteRs.hasNext()) {
                        count = verifyDeleteRs.next().longValue("cnt");
                    }

                    System.out.println("Records remaining after delete: " + count);
                    if (count == 0) {
                        System.out.println("Deletion verification successful.");
                    } else {
                        System.out.println("Warning: Test data deletion may have failed.");
                    }

                } catch (Exception e) {
                    System.err.println("Table verification failed: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.err.println("Schema setup failed.");
            }

        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Clean up connection
            try {
                IgniteConnection.close();
                System.out.println("Test completed, resources cleaned up.");
            } catch (Exception e) {
                System.err.println("Error during cleanup: " + e.getMessage());
            }
        }
    }
}