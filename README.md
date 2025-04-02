# Transit Monitoring System with Apache Ignite 3

A real-time public transit monitoring application that demonstrates Apache Ignite 3's distributed data processing capabilities through practical, production-ready patterns.

## Overview

This project showcases how to build a scalable transit monitoring system by leveraging Apache Ignite 3's distributed computing and data storage capabilities. The application consumes GTFS (General Transit Feed Specification) data to track vehicle positions, detect service disruptions, and provide operational insights through a terminal-based dashboard.

## Key Features

- **Real-time data ingestion** from GTFS feeds with resilient error handling
- **Time-series data storage** using Ignite's distributed tables
- **Automated service monitoring** for vehicle delays, bunching, and coverage gaps
- **Interactive dashboard** with rotating views of system status
- **Structured component architecture** demonstrating clean Java design patterns

## Prerequisites

- Java 11 or later
- Maven 3.6+
- Docker and Docker Compose
- API key from a GTFS provider (e.g., 511.org)

## Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/transit-monitor.git
cd transit-monitor
```

### 2. Configure Environment

Copy the `.env.example` to `.env` and add your GTFS API token:

```bash
cp .env.example .env
```

Edit `.env` to include your API token.

### 3. Start Ignite Cluster

```bash
docker compose up -d
```

### 4. Initialize the Cluster

```bash
docker run --rm -it --network=host -e LANG=C.UTF-8 -e LC_ALL=C.UTF-8 apacheignite/ignite:3.0.0 cli
connect http://localhost:10300
cluster init --name=ignite3 --metastorage-group=node1,node2,node3
exit
```

## Learning Path

Follow our step-by-step tutorial to understand the application architecture:

1. [Introduction](docs/01-introduction.md)
2. [Project Setup](docs/02-project-setup.md)
3. [Understanding GTFS](docs/03-understanding-gtfs.md)
4. [GTFS Client](docs/04-gtfs-client.md)
5. [Data Ingestion](docs/05-data-ingestion.md)
6. [Service Monitoring](docs/06-continuous-query.md)
7. [Application Integration](docs/07-putting-together.md)

## Example Applications

Explore individual components with these standalone examples:

```bash
# Test Ignite connectivity
mvn compile exec:java@connect-example

# Verify schema creation
mvn compile exec:java@schema-setup-example

# Test GTFS feed access
mvn compile exec:java@gtfs-feed-example

# Run data ingestion process
mvn compile exec:java@ingest-example

# Try service monitoring
mvn compile exec:java@service-monitor-example

# Run the final application
mvn compile exec:java@run-app
```

## License

[Apache License 2.0](LICENSE)