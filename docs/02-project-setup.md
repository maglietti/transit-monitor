# Project Setup and Configuration

In this module, you'll establish the foundation for your transit monitoring application by setting up the project structure, dependencies, and local Ignite cluster. A proper setup ensures you can focus on the core functionality in subsequent modules without environment-related interruptions.

## Getting the Source Code

Clone the GitHub repository:

```shell
git clone https://github.com/maglietti/transit-monitor.git
cd transit-monitor
```

Please explore the code and follow along with the tutorial using this repository.

## Development Environment Options

While the tutorial uses Maven and command-line operations, you can adapt the approach to your preferred development environment:

### IntelliJ IDEA Setup

1. Open IntelliJ IDEA
2. Select **File > New > Project from Existing Sources**
3. Navigate to the cloned repository and select the `pom.xml` file
4. Follow the prompts to import the project
5. Once imported, you can run the examples from the IDE

### Visual Studio Code Setup

1. Open VS Code
2. Use **File > Open Folder** and select the cloned repository
3. Install the Java Extension Pack if you haven't already
4. Open the Maven extension to view and run the project goals
5. Use the Java extension to run the application

### Using Other Editors

If you prefer another editor:

1. Navigate to the project directory in your terminal
2. Use command-line Maven to build and run the project

## Directory Structure

The project structure is organized as follows:

```shell
transit-monitoring/
├── docker-compose.yml
├── pom.xml
├── README.md
└── src
    ├── main
    │   ├── java
    │   │   └── com
    │   │       └── example
    │   │           └── transit
    │   │               ├── app
    │   │               │   └── TransitMonitorApp.java
    │   │               ├── config
    │   │               │   ├── ConfigService.java
    │   │               │   ├── IgniteConnectionManager.java
    │   │               │   └── VehiclePositionTableManager.java
    │   │               ├── examples
    │   │               │   ├── ConnectExample.java
    │   │               │   ├── GtfsFeedExample.java
    │   │               │   ├── IngestExample.java
    │   │               │   ├── SchemaSetupExample.java
    │   │               │   └── ServiceMonitorExample.java
    │   │               ├── model
    │   │               │   ├── IngestStats.java
    │   │               │   ├── ServiceAlert.java
    │   │               │   └── VehiclePosition.java
    │   │               ├── service
    │   │               │   ├── DataIngestionService.java
    │   │               │   ├── GtfsService.java
    │   │               │   ├── MonitorService.java
    │   │               │   └── ReportingService.java
    │   │               └── util
    │   │                   └── TerminalUtil.java
    │   └── resources
    │       └── log4j2.xml
    └── test
        └── java
```

### Maven Configuration

The Maven `pom.xml` file contains the necessary dependencies for your application. Open it in your editor to explore the dependencies:

```shell
open pom.xml  # or use your preferred editor
```

The key dependencies include:

- **Apache Ignite client library**: Enables connection to an Ignite cluster
- **MobilityData GTFS-realtime API**: Provides tools for parsing transit data
- **Log4j2**: Implements structured logging for application events
- **dotenv-java**: Manages configuration variables securely

After reviewing the `pom.xml` file, run `mvn verify` in your terminal from the project directory to download all dependencies.

## Understanding Ignite's Client-Server Architecture

Before we set up our Ignite cluster, let's briefly explore Ignite's architecture. Apache Ignite 3 follows a client-server model:

- **Server Nodes**: Form the distributed cluster where data is stored and processed
- **Clients**: Lightweight connections that communicate with the cluster

In our application, we'll use the Java API to connect to a cluster of Ignite server nodes running in Docker containers.

## Setting Up Your Ignite Cluster

To run the application, you will set up a local Ignite 3 cluster in Docker.

### Using the Docker Compose File

The repository contains a file named `docker-compose.yml`. This Docker Compose file creates a three-node Ignite cluster running on your local host. Each node is configured with:

- 4GB of memory allocation
- Exposed HTTP and client ports
- A shared configuration for node discovery
- In-memory storage (no data is persisted to disk)

### Starting the Ignite Cluster

To launch the cluster:

1. Open a terminal in the directory containing your `docker-compose.yml` file
2. Run: `docker compose up -d`
3. Verify the status with: `docker compose ps`

You should see output similar to:

```shell
NAME                COMMAND                  SERVICE             STATUS              PORTS
ignite3-node1-1     "/opt/ignite/bin/ign…"   node1               running             0.0.0.0:10300->10300/tcp, 0.0.0.0:10800->10800/tcp
ignite3-node2-1     "/opt/ignite/bin/ign…"   node2               running             0.0.0.0:10301->10300/tcp, 0.0.0.0:10801->10800/tcp
ignite3-node3-1     "/opt/ignite/bin/ign…"   node3               running             0.0.0.0:10302->10300/tcp, 0.0.0.0:10802->10800/tcp
```

The cluster exposes two ports for each node:

- **10300-10302**: REST API ports for administrative operations
- **10800-10802**: Client connection ports for our Java application

### Initializing the Cluster

Before we can use our cluster, we need to initialize it. Ignite 3 requires explicit cluster initialization, which configures the metastorage group responsible for system metadata.

Start the Ignite 3 CLI in Docker:

```bash
docker run --rm -it --network=host -e LANG=C.UTF-8 -e LC_ALL=C.UTF-8 apacheignite/ignite:3.0.0 cli
```

Inside the CLI:

1. Connect to a node: `connect http://localhost:10300`
2. Initialize the cluster: `cluster init --name=ignite3 --metastorage-group=node1,node2,node3`

The initialization step creates essential system tables and structures in the Ignite cluster. The metastorage group defines which nodes will store this critical system information, with enough redundancy to maintain availability if one node fails.

## Understanding the Ignite Connection Manager

Now that our cluster is running, we need to connect to it from our Java application. Open the `IgniteConnectionManager.java` file in the repository:

```shell
open src/main/java/com/example/transit/config/IgniteConnectionManager.java
```

The core connection code in this file is:

```java
igniteClient = IgniteClient.builder()
        .addresses(
                "127.0.0.1:10800",
                "127.0.0.1:10801",
                "127.0.0.1:10802"
        )
        .retryPolicy(new RetryReadPolicy())
        .build();
```

This code accomplishes several things:

- Uses the builder pattern to configure the connection
- Specifies addresses for all three Ignite server nodes for redundancy
- Configures a retry policy that automatically handles temporary connection issues
- Creates a client that can be used throughout the application

The class implements `AutoCloseable` for proper resource management, ensuring connections are properly released when the application shuts down.

## Testing the Connection

Run the connection example to verify that you can connect to the Ignite cluster:

```bash
mvn compile exec:java@connect-example
```

This command uses the predefined execution in the Maven POM file to run the `ConnectExample` class. The example should connect to the Ignite cluster and display information about it, similar to this output:

```text
=== Connecting to Ignite cluster...
Connected to Ignite cluster: [ClientClusterNode [id=adbc68bc-7e81-4a1d-93f0-4c7d9c09189c, name=node3, address=127.0.0.1:10802, nodeMetadata=null]]

========== IGNITE CLUSTER OVERVIEW ==========

CLUSTER TOPOLOGY:
  • Total cluster nodes: 3
    - Node 1: node1 (ID: 1bdd785a-7b97-491e-a6b0-9b6b143dcc63, Address: 172.18.0.3:3344)
    - Node 2: node2 (ID: 8c739985-24a6-4fc9-a39d-260bf701dfa6, Address: 172.18.0.2:3344)
    - Node 3: node3 (ID: adbc68bc-7e81-4a1d-93f0-4c7d9c09189c, Address: 172.18.0.4:3344)
  • Currently connected to: node3

CONNECTION DETAILS:
  • Connection timeout: 5000ms
  • Operation timeout: No timeout (unlimited)
  • Heartbeat interval: 30000ms

AVAILABLE TABLES:
  • Found 1 table(s):
    - VEHICLEPOSITION
  • Tip: Access a table with client.tables().table("VEHICLEPOSITION")

RETRY POLICY:
  • Type: RetryReadPolicy
  • Retry limit: 16
  • Tip: The retry policy helps maintain connection during network issues

SECURITY STATUS:
  • Authentication: Not configured
  • SSL/TLS: Disabled
  • Tip: Consider enabling SSL for secure communication

========== END OF CLUSTER OVERVIEW ==========

Ignite client connection closed
=== Disconnected from Ignite cluster
```

Let's examine what the connection example does. Open `ConnectExample.java`:

```shell
open src/main/java/com/example/transit/examples/ConnectExample.java
```

The example:

1. Creates an instance of `IgniteConnectionManager`
2. Gets the Ignite client from the manager
3. Calls a method to display information about the cluster
4. Automatically closes the connection when done using try-with-resources

## Next Steps

You've now set up a complete development environment for our transit monitoring application:

- A three-node Ignite cluster running in Docker containers
- A Maven project with all necessary dependencies
- A robust connection management class
- A working example application that verifies connectivity

This foundation gives us everything we need to start building our transit monitoring application. In the next module, we'll explore the GTFS data format and design our schema for storing transit data in Ignite.

**Next Steps:** Continue to [Module 3: Understanding GTFS Data and Creating the Transit Schema](03-understanding-gtfs.md)
