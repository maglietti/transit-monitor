package com.example.transit;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.client.RetryReadPolicy;

/**
 * Singleton class that manages the connection to the Ignite cluster.
 * This class uses the Ignite 3 client API to establish and maintain
 * a connection to the cluster throughout our application's lifecycle.
 */
public class IgniteConnection {
    private static IgniteClient igniteClient;

    /**
     * Gets a singleton instance of IgniteClient.
     * The client connects to a local Ignite 3 cluster running on the default port.
     *
     * @return An initialized IgniteClient instance
     * @throws RuntimeException if the connection cannot be established
     */
    public static synchronized IgniteClient getClient() {
        if (igniteClient == null) {
            try {
                // Using the builder pattern introduced in Ignite 3
                igniteClient = IgniteClient.builder()
                        // Configure the addresses of all three Ignite server nodes
                        // This provides redundancy and failover capabilities
                        .addresses(
                                "127.0.0.1:10800",  // Node 1
                                "127.0.0.1:10801",  // Node 2
                                "127.0.0.1:10802"   // Node 3
                        )
                        // Set connection timeout to 10 seconds
                        .connectTimeout(10_000)
                        // RetryReadPolicy allows read operations to be retried automatically on connection issues
                        .retryPolicy(new RetryReadPolicy())
                        // Build the client instance
                        .build();

                System.out.println("Successfully connected to Ignite cluster" + igniteClient.connections());
            } catch (Exception e) {
                System.err.println("Failed to connect to Ignite cluster: " + e.getMessage());
                throw new RuntimeException("Ignite connection failure", e);
            }
        }
        return igniteClient;
    }

    /**
     * Closes the connection to the Ignite cluster.
     * Call this method when shutting down your application to release resources.
     */
    public static void close() {
        if (igniteClient != null) {
            try {
                igniteClient.close();
                igniteClient = null;
                System.out.println("Ignite client connection closed");
            } catch (Exception e) {
                System.err.println("Error closing Ignite client: " + e.getMessage());
            }
        }
    }
}