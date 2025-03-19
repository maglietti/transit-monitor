package com.example.transit;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.client.RetryLimitPolicy;
import org.apache.ignite.network.ClusterNode;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Test class for verifying connection to an Ignite 3 cluster.
 * This class demonstrates how to connect to a cluster and retrieve information.
 */
public class IgniteClusterTest {

    public static void main(String[] args) {
        // Configure logging to be quiet before any other operations
        configureLogging();

        // Connect to Ignite cluster
        IgniteClient client = null;

        try {
            // Get client connection
            System.out.println("--- Connecting to Ignite cluster...");
            client = IgniteConnection.getClient();

            // Test the connection by retrieving cluster nodes
            System.out.println("Testing connection by retrieving cluster nodes...");
            testConnection(client);

            System.out.println("Ignite cluster operations completed successfully");

        } catch (Exception e) {
            System.err.println("Error during Ignite operations: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Always properly disconnect from the cluster
            if (client != null) {
                System.out.println("--- Disconnecting from Ignite cluster...");
                IgniteConnection.close();
            }
        }
    }

    /**
     * Configure logging to completely suppress all log messages.
     * This is useful for tests to keep the console output to a minimum.
     */
    private static void configureLogging() {
        // Get the Logback root logger and set it to OFF
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.OFF);

        // Specifically set Netty logger to OFF as well
        Logger nettyLogger = (Logger) LoggerFactory.getLogger("io.netty");
        nettyLogger.setLevel(Level.OFF);
    }

    /**
     * Tests the connection to the Ignite cluster and displays useful information
     *
     * @param client The IgniteClient instance
     */
    private static void testConnection(IgniteClient client) {
        System.out.println("\n========== IGNITE CLUSTER OVERVIEW ==========");

        // Get list of active connections (cluster nodes)
        List<ClusterNode> clusterNnodes = client.connections();

        // 1. Cluster Topology Information
        System.out.println("\nCLUSTER TOPOLOGY:");
        try {
            // Get complete cluster topology
            List<ClusterNode> allNodes = client.clusterNodes().stream().collect(Collectors.toList());

            System.out.println("  • Total cluster nodes: " + allNodes.size());

            // Display simple list of nodes
            for (int i = 0; i < allNodes.size(); i++) {
                ClusterNode node = allNodes.get(i);
                System.out.println("    - Node " + (i + 1) + ": " + node.name() +
                        " (ID: " + node.id() + ", Address: " + node.address() + ")");
            }

            // Also show which node(s) the client is currently connected to
            List<ClusterNode> connectedNodes = client.connections();
            System.out.println("  • Currently connected to: " +
                    (connectedNodes.isEmpty() ? "None" : connectedNodes.get(0).name()));
        } catch (Exception e) {
            System.out.println("  • Could not retrieve full cluster topology: " + e.getMessage());

            // Fall back to showing just the connected nodes
            List<ClusterNode> currentNodes = client.connections();
            System.out.println("  • Connected nodes: " + currentNodes.size());
            for (int i = 0; i < currentNodes.size(); i++) {
                ClusterNode node = currentNodes.get(i);
                System.out.println("    - Node " + (i + 1) + ": " + node.name() +
                        " (ID: " + node.id() + ", Address: " + node.address() + ")");
            }
        }

        // 2. Connection Details
        System.out.println("\nCONNECTION DETAILS:");
        System.out.println("  • Connection timeout: " + client.configuration().connectTimeout() + "ms");
        System.out.println("  • Operation timeout: " +
                (client.configuration().operationTimeout() > 0 ?
                        client.configuration().operationTimeout() + "ms" : "No timeout (unlimited)"));
        System.out.println("  • Heartbeat interval: " + client.configuration().heartbeatInterval() + "ms");

        // 3. Available Resources - Tables
        try {
            List<String> tables = client.tables().tables().stream()
                    .map(table -> table.name())
                    .collect(Collectors.toList());

            System.out.println("\nAVAILABLE TABLES:");
            if (tables.isEmpty()) {
                System.out.println("  • No tables found. Your cluster is ready for you to create tables.");
                System.out.println("  • Tip: Use client.tables().createTable(...) to create your first table.");
            } else {
                System.out.println("  • Found " + tables.size() + " table(s):");
                for (String tableName : tables) {
                    System.out.println("    - " + tableName);
                }
                System.out.println("  • Tip: Access a table with client.tables().table(\"" +
                        (tables.isEmpty() ? "table_name" : tables.get(0)) + "\")");
            }
        } catch (Exception e) {
            System.out.println("\nAVAILABLE TABLES:");
            System.out.println("  • Could not retrieve tables: " + e.getMessage());
            System.out.println("  • Tip: You may need additional permissions to view tables");
        }

        // 4. Client Retry Policy
        System.out.println("\nRETRY POLICY:");
        if (client.configuration().retryPolicy() != null) {
            System.out.println("  • Type: " + client.configuration().retryPolicy().getClass().getSimpleName());
            if (client.configuration().retryPolicy() instanceof RetryLimitPolicy) {
                RetryLimitPolicy policy = (RetryLimitPolicy) client.configuration().retryPolicy();
                System.out.println("  • Retry limit: " + policy.retryLimit());
            }
            System.out.println("  • Tip: The retry policy helps maintain connection during network issues");
        } else {
            System.out.println("  • No retry policy configured");
            System.out.println("  • Tip: Consider adding a RetryReadPolicy for better resilience");
        }

        // 5. Security Status
        System.out.println("\nSECURITY STATUS:");
        if (client.configuration().authenticator() != null) {
            System.out.println("  • Authentication: Enabled");
            System.out.println("  • Type: " + client.configuration().authenticator().type());
        } else {
            System.out.println("  • Authentication: Not configured");
        }

        if (client.configuration().ssl() != null && client.configuration().ssl().enabled()) {
            System.out.println("  • SSL/TLS: Enabled");
        } else {
            System.out.println("  • SSL/TLS: Disabled");
            System.out.println("  • Tip: Consider enabling SSL for secure communication");
        }

        System.out.println("\nCONNECTION SUCCESSFUL! You are now ready to use Ignite.");
        System.out.println("========== END OF CLUSTER OVERVIEW ==========\n");
    }
}