package com.example.transit.config;

import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.client.RetryReadPolicy;

/**
 * Manages connection to the Ignite cluster.
 * Provides a client connection that can be used throughout the application.
 */
public class IgniteConnectionManager implements AutoCloseable {
    private IgniteClient igniteClient;

    /**
     * Constructor that initializes the Ignite client connection.
     *
     * @throws RuntimeException if the connection cannot be established
     */
    public IgniteConnectionManager() {
        try {
            igniteClient = IgniteClient.builder()
                    .addresses(
                            "127.0.0.1:10800",
                            "127.0.0.1:10801",
                            "127.0.0.1:10802"
                    )
                    .retryPolicy(new RetryReadPolicy())
                    .build();

            System.out.println("Connected to Ignite cluster: " + igniteClient.connections());
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
     */
    @Override
    public void close() {
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