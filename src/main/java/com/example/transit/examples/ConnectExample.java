package com.example.transit.examples;

import com.example.transit.service.ConnectService;
import com.example.transit.util.LoggingUtil;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.network.ClusterNode;
import org.apache.ignite.client.RetryLimitPolicy;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Example class for verifying connection to an Ignite 3 cluster.
 * This class demonstrates how to connect to a cluster and retrieve information.
 */
public class ConnectExample {
    public static void main(String[] args) {
        // Configure logging to suppress unnecessary output
        LoggingUtil.setLogs("OFF");

        System.out.println("=== Connecting to Ignite cluster...");

        // Use try-with-resources to automatically handle connection cleanup
        try (ConnectService connectionService = new ConnectService()) {
            IgniteClient client = connectionService.getClient();
            clusterOverview(client);
        } catch (Exception e) {
            System.err.println("Error during Ignite connection: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("=== Disconnected from Ignite cluster");
    }

    /**
     * Connects to an Ignite cluster and displays useful information
     *
     * @param client The IgniteClient instance
     */
    private static void clusterOverview(IgniteClient client) {
        System.out.println("\n========== IGNITE CLUSTER OVERVIEW ==========");

        // Cluster Topology Information
        System.out.println("\nCLUSTER TOPOLOGY:");
        try {
            // Get complete cluster topology
            List<ClusterNode> allNodes = client.clusterNodes().stream().collect(Collectors.toList());

            System.out.println("  • Total cluster nodes: " + allNodes.size());

            // Display simple list of nodes
            for (int i = 0; i < allNodes.size(); i++) {
                ClusterNode node = allNodes.get(i);
                System.out.println("    - Node " + (i + 1) + ": " + node.name() + " (ID: " + node.id() + ", Address: " + node.address() + ")");
            }

            // Also show which node(s) the client is currently connected to
            List<ClusterNode> connectedNodes = client.connections();
            System.out.println("  • Currently connected to: " + (connectedNodes.isEmpty() ? "None" : connectedNodes.get(0).name()));
        } catch (Exception e) {
            System.out.println("  • Could not retrieve full cluster topology: " + e.getMessage());

            // Fall back to showing just the connected nodes
            List<ClusterNode> currentNodes = client.connections();
            System.out.println("  • Connected nodes: " + currentNodes.size());
            for (int i = 0; i < currentNodes.size(); i++) {
                ClusterNode node = currentNodes.get(i);
                System.out.println("    - Node " + (i + 1) + ": " + node.name() + " (ID: " + node.id() + ", Address: " + node.address() + ")");
            }
        }

        // Connection Details
        System.out.println("\nCONNECTION DETAILS:");
        System.out.println("  • Connection timeout: " + client.configuration().connectTimeout() + "ms");
        System.out.println("  • Operation timeout: " + (client.configuration().operationTimeout() > 0 ? client.configuration().operationTimeout() + "ms" : "No timeout (unlimited)"));
        System.out.println("  • Heartbeat interval: " + client.configuration().heartbeatInterval() + "ms");

        // Available Resources - Tables
        try {
            List<String> tables = client.tables().tables().stream().map(table -> table.name()).collect(Collectors.toList());

            System.out.println("\nAVAILABLE TABLES:");
            if (tables.isEmpty()) {
                System.out.println("  • No tables found. Your cluster is ready for you to create tables.");
                System.out.println("  • Tip: Use client.tables().createTable(...) to create your first table.");
            } else {
                System.out.println("  • Found " + tables.size() + " table(s):");
                for (String tableName : tables) {
                    System.out.println("    - " + tableName);
                }
                System.out.println("  • Tip: Access a table with client.tables().table(\"" + (tables.isEmpty() ? "table_name" : tables.get(0)) + "\")");
            }
        } catch (Exception e) {
            System.out.println("\nAVAILABLE TABLES:");
            System.out.println("  • Could not retrieve tables: " + e.getMessage());
            System.out.println("  • Tip: You may need additional permissions to view tables");
        }

        // Client Retry Policy
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

        // Security Status
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

        System.out.println("\n========== END OF CLUSTER OVERVIEW ==========\n");
    }
}
