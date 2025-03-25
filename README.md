# Transit Monitor System

> [!note]
> This repository is the companion to the Your First Ignite App with Java. The tutorial walks you through building a real-time transit monitoring system using Apache Ignite 3.

A real-time public transit monitoring system that tracks vehicle positions, detects service disruptions, and provides a live dashboard of transit operations.

## Overview

The Transit Monitoring System is a Java application that connects to a GTFS-realtime feed to ingest vehicle position data and monitor transit operations for potential service disruptions. The system detects common issues such as vehicle bunching, delayed vehicles, offline vehicles, and routes with insufficient coverage.

## Features

- **Real-time data ingestion**: Connects to GTFS-realtime feeds to retrieve vehicle position data
- **Database storage**: Stores transit data in **Apache Ignite 3** for fast querying and analysis
- **Service monitoring**: Automatically detects potential service disruptions:
    - Delayed vehicles (stopped for too long)
    - Vehicle bunching (vehicles on same route too close together)
    - Routes with insufficient coverage
    - Offline vehicles (not reporting positions)
- **Live dashboard**: Console-based dashboard with three rotating views:
    - Summary view (active vehicles by route, status distribution)
    - Alerts view (recent service alerts and statistics)
    - System details view (system statistics, monitoring thresholds)

## Prerequisites

- Java 11 or later
- Apache Ignite 3 cluster (running on localhost ports 10800-10802)
- GTFS-realtime feed access (API token required)
- Docker and Docker Compose (for running the Ignite cluster)

## Configuration

### Environment Variables

Copy the `.env.example` file to `.env` and update the variables:

```
API_TOKEN=your_gtfs_api_token
GTFS_BASE_URL=https://example.com/gtfs-rt/vehicle-positions
GTFS_AGENCY=agency_id
```

### Apache Ignite Setup

The application requires an Apache Ignite 3 cluster. You can easily start a local 3-node cluster using the provided Docker Compose file:

```bash
# Start the Ignite cluster
docker-compose up -d

# Check the status of the nodes
docker-compose ps
```

The Docker setup creates three Ignite nodes accessible at:
- Node1: localhost:10800
- Node2: localhost:10801
- Node3: localhost:10802

To stop the cluster:

```bash
docker-compose down
```

## Building

```bash
# Compile the application
mvn clean package

# Run the application
java -jar target/transit-monitor-1.0.jar
```

## Example Applications

The repository includes several example applications to demonstrate specific components of the system:

### ConnectExample

Demonstrates connectivity to the Apache Ignite cluster and displays cluster information.

```bash
java -cp target/transit-monitor-1.0.jar com.example.transit.examples.ConnectExample
```

This example:
- Connects to the Ignite cluster
- Displays cluster topology information
- Shows connection details and retry policies
- Lists available tables in the database

### SchemaSetupExample

Demonstrates database schema creation and verification.

```bash
java -cp target/transit-monitor-1.0.jar com.example.transit.examples.SchemaSetupExample
```

This example:
- Creates the required database table for vehicle positions
- Inserts test data to verify write operations
- Queries the data to verify read operations
- Deletes the test data and verifies deletion

### GtfsFeedExample

Demonstrates connecting to a GTFS-realtime feed and retrieving vehicle positions.

```bash
java -cp target/transit-monitor-1.0.jar com.example.transit.examples.GtfsFeedExample
```

This example:
- Connects to the configured GTFS feed
- Retrieves vehicle position data
- Displays sample data and statistics
- Stores the data in the database

### IngestExample

Demonstrates the continuous data ingestion pipeline.

```bash
java -cp target/transit-monitor-1.0.jar com.example.transit.examples.IngestExample
```

This example:
- Sets up the database schema
- Starts the data ingestion service
- Runs for 45 seconds, fetching data every 15 seconds
- Displays ingestion statistics
- Verifies the data was stored correctly

### ServiceMonitorExample

Demonstrates the service monitoring functionality.

```bash
java -cp target/transit-monitor-1.0.jar com.example.transit.examples.ServiceMonitorExample
```

This example:
- Verifies the database contains vehicle position data
- Starts the monitoring service to detect service disruptions
- Displays alert statistics every 30 seconds
- Shows sample service alerts when stopped

## Dashboard

The main application's dashboard automatically rotates between three views:

1. **Summary View**: Shows active vehicles by route, vehicle status distribution, and data ingestion status
2. **Alerts View**: Displays recent service alerts and alert statistics by type
3. **Details View**: Shows system statistics, monitoring thresholds, and connection status

## Extending the System

The modular architecture makes it easy to extend the system:

- Add new monitoring conditions in `MonitorService`
- Create new dashboard views in `TransitMonitorApp`
- Implement additional data sources by creating new service classes

## Troubleshooting

Common issues and solutions:

- **Connection failures**: Verify Ignite cluster is running on the expected ports
- **Empty data**: Check API token and agency ID in the .env file
- **Schema errors**: Make sure Ignite is properly configured with SQL support
- **Monitoring not detecting issues**: Verify thresholds in MonitorService

## License

[Apache License 2.0](LICENSE)
