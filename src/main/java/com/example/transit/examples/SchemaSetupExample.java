package com.example.transit.examples;

import com.example.transit.service.ConnectService;
import com.example.transit.service.SchemaService;
import com.example.transit.util.LoggingUtil;
import org.apache.ignite.client.IgniteClient;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

/**
 * Example demonstrating database connectivity and schema operations.
 *
 * This class shows how to:
 * 1. Connect to an Apache Ignite cluster
 * 2. Create a table for vehicle position data
 * 3. Perform basic CRUD operations to verify functionality
 *
 * Run this example after initializing an Ignite cluster to verify
 * that your application can interact with the database correctly.
 */
public class SchemaSetupExample {

    private static final String VEHICLE_TABLE = "vehicle_positions";

    /**
     * Main method to run the schema setup example.
     *
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        // Configure logging to suppress unnecessary output
        LoggingUtil.setLogs("OFF");

        System.out.println("=== Table Creation Example ===");
        System.out.println("=== Connect to Ignite cluster");

        try (ConnectService connectionService = new ConnectService()) {
            IgniteClient client = connectionService.getClient();

            // Create schema and verify with test data
            System.out.println("=== Create vehicle positions table");
            SchemaService schemaSetup = new SchemaService(connectionService);

            if (schemaSetup.createSchema()) {
                verifyTableWithTestData(client);
            } else {
                System.err.println("Table setup failed.");
            }
        } catch (Exception e) {
            System.err.println("Table setup failed: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("=== Table operations completed");
    }

    /**
     * Performs a sequence of operations to verify table functionality.
     *
     * @param client Ignite client connection
     */
    private static void verifyTableWithTestData(IgniteClient client) {
        System.out.println("=== Table operations");

        try {
            verifyTableExists(client);

            Map<String, Object> testData = createTestData();
            insertTestData(client, testData);

            queryTestData(client, testData);
            deleteAndVerify(client, testData);
        } catch (Exception e) {
            System.err.println("Table operations failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Verifies that the vehicle positions table exists in the schema.
     *
     * @param client Ignite client connection
     */
    private static void verifyTableExists(IgniteClient client) {
        try {
            var table = client.tables().table(VEHICLE_TABLE);
            if (table != null) {
                System.out.println("+++ Table exists: " + table.name());
            } else {
                System.out.println("Table not found in schema.");
            }
        } catch (Exception e) {
            System.err.println("Error checking if table exists: " + e.getMessage());
        }
    }

    /**
     * Creates a map with test vehicle data.
     *
     * @return Map containing test vehicle position data
     */
    private static Map<String, Object> createTestData() {
        long currentTime = System.currentTimeMillis();
        Map<String, Object> testData = new HashMap<>();
        testData.put("vehicle_id", "test-vehicle-1");
        testData.put("route_id", "test-route-100");
        testData.put("latitude", 47.6062);
        testData.put("longitude", -122.3321);
        testData.put("timestamp", currentTime);
        testData.put("current_status", "STOPPED");

        return testData;
    }

    /**
     * Inserts test vehicle data into the database using SQL.
     *
     * @param client Ignite client connection
     * @param testData Map containing the test data
     */
    private static void insertTestData(IgniteClient client, Map<String, Object> testData) {
        // Convert timestamp to LocalDateTime expected by Ignite
        LocalDateTime localDateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli((Long) testData.get("timestamp")),
                ZoneId.systemDefault()
        );

        // SQL parameters are provided in the order they appear in the query
        String insertSql = "INSERT INTO vehicle_positions " +
                "(vehicle_id, route_id, latitude, longitude, time_stamp, current_status) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        client.sql().execute(null, insertSql,
                testData.get("vehicle_id"),
                testData.get("route_id"),
                testData.get("latitude"),
                testData.get("longitude"),
                localDateTime,
                testData.get("current_status"));

        System.out.println("+++ Test record inserted successfully: " + testData);
    }

    /**
     * Queries the inserted test data and displays results.
     *
     * @param client Ignite client connection
     * @param testData Map containing the original test data
     */
    private static void queryTestData(IgniteClient client, Map<String, Object> testData) {
        String querySql = "SELECT vehicle_id, route_id, latitude, longitude, " +
                "time_stamp, current_status FROM vehicle_positions WHERE vehicle_id = ?";

        try (var resultSet = client.sql().execute(null, querySql, testData.get("vehicle_id"))) {
            int resultCount = 0;

            while (resultSet.hasNext()) {
                var row = resultSet.next();
                resultCount++;

                // Convert timestamp and display the result
                LocalDateTime resultDateTime = row.value("time_stamp");
                Instant instant = resultDateTime.atZone(ZoneId.systemDefault()).toInstant();
                long timestamp = instant.toEpochMilli();

                Map<String, Object> resultData = Map.of(
                        "vehicle_id", row.stringValue("vehicle_id"),
                        "route_id", row.stringValue("route_id"),
                        "latitude", row.doubleValue("latitude"),
                        "longitude", row.doubleValue("longitude"),
                        "timestamp", timestamp,
                        "current_status", row.stringValue("current_status")
                );

                System.out.println("+++ Found test record: " + resultData);
            }

            System.out.println("+++ Retrieved " + resultCount + " vehicle position records");
        }
    }

    /**
     * Deletes test data and verifies it was removed.
     *
     * @param client Ignite client connection
     * @param testData Map containing the test data to delete
     */
    private static void deleteAndVerify(IgniteClient client, Map<String, Object> testData) {
        // Delete the test record using SQL
        String deleteSql = "DELETE FROM vehicle_positions WHERE vehicle_id = ?";
        client.sql().execute(null, deleteSql, testData.get("vehicle_id"));
        System.out.println("+++ Test record deleted successfully.");

        // Verify deletion by counting remaining matching records
        long count = 0;
        String verifySql = "SELECT COUNT(*) as cnt FROM vehicle_positions WHERE vehicle_id = ?";

        try (var verifyResultSet = client.sql().execute(null, verifySql, testData.get("vehicle_id"))) {
            if (verifyResultSet.hasNext()) {
                count = verifyResultSet.next().longValue("cnt");
            }
        }

        System.out.println("+++ Records remaining after delete: " + count);
        if (count == 0) {
            System.out.println("+++ Deletion verification successful.");
        } else {
            System.err.println("Warning: Test data deletion may have failed.");
        }
    }
}