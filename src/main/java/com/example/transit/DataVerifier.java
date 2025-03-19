package com.example.transit;

import org.apache.ignite.client.IgniteClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for verifying and examining data in the Ignite database.
 */
public class DataVerifier {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Verifies the existence and integrity of vehicle position data in Ignite.
     */
    public static void verifyData() {
        System.out.println("Verifying data in vehicle_positions table...");

        try {
            IgniteClient client = IgniteConnection.getClient();

            // Check table existence and count records
            System.out.println("Checking table records...");
            // The SQL error shows that "COUNT(*) as count" is failing - let's try a simpler approach
            String countSql = "SELECT COUNT(*) FROM vehicle_positions";
            var countResult = client.sql().execute(null, countSql);

            long recordCount = 0;
            if (countResult.hasNext()) {
                // Don't use named column access since "as count" is failing
                recordCount = countResult.next().longValue(0);
                System.out.println("Table exists");
                System.out.println("Table contains " + recordCount + " records");
            } else {
                System.out.println("Table appears to exist but COUNT query returned no results");
            }

            if (recordCount == 0) {
                System.out.println("Table is empty. Let's start the ingestion service to load some data.");
                return;
            }

            // Sample recent records
            System.out.println("\nSample records (most recent):");
            String sampleSql = "SELECT * FROM vehicle_positions ORDER BY time_stamp DESC LIMIT 3";
            var sampleResult = client.sql().execute(null, sampleSql);

            while (sampleResult.hasNext()) {
                var record = sampleResult.next();
                LocalDateTime timestamp = record.value("time_stamp");

                System.out.println("Vehicle: " + record.stringValue("vehicle_id") +
                        ", Route: " + record.stringValue("route_id") +
                        ", Status: " + record.stringValue("current_status") +
                        ", Time: " + timestamp.format(DATETIME_FORMATTER));
            }

            // Get route statistics
            System.out.println("\nTop routes by number of records:");
            String routeStatsSql = "SELECT route_id, COUNT(*) as total " +
                    "FROM vehicle_positions " +
                    "GROUP BY route_id " +
                    "ORDER BY total DESC " +
                    "LIMIT 5";

            var routeResult = client.sql().execute(null, routeStatsSql);

            while (routeResult.hasNext()) {
                var record = routeResult.next();
                System.out.println("Route " + record.stringValue("route_id") +
                        ": " + record.longValue("total") + " records");
            }

            System.out.println("\nVerification complete - data exists in Ignite");

        } catch (Exception e) {
            System.err.println("Error verifying data: " + e.getMessage());
            e.printStackTrace();
        }
    }
}