package com.example.transit.service;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.client.RetryReadPolicy;

/**
 * Service class that manages the connection to the Ignite cluster.
 * This class uses the Ignite 3 client API to establish and maintain
 * a connection to the cluster throughout our application's lifecycle.
 * Implements AutoCloseable to support try-with-resources.
 */
public class ConnectService implements AutoCloseable {
    private IgniteClient igniteClient;

    /**
     * Constructor that initializes the Ignite client connection.
     *
     * @throws RuntimeException if the connection cannot be established
     */
    public ConnectService() {
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

            System.out.println("--- Successfully connected to Ignite cluster: " + igniteClient.connections());
        } catch (Exception e) {
            System.err.println("Failed to connect to Ignite cluster: " + e.getMessage());
            throw new RuntimeException("Ignite connection failure", e);
        }
    }

    /**
     * Gets the IgniteClient instance.
     *
     * @return An initialized IgniteClient instance
     */
    public IgniteClient getClient() {
        return igniteClient;
    }

    /**
     * Closes the connection to the Ignite cluster.
     * This method is automatically called when used with try-with-resources.
     */
    @Override
    public void close() {
        if (igniteClient != null) {
            try {
                igniteClient.close();
                igniteClient = null;
                System.out.println("--- Ignite client connection closed");
            } catch (Exception e) {
                System.err.println("Error closing Ignite client: " + e.getMessage());
            }
        }
    }
}