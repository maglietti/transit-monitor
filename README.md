# Transit Monitoring System with Apache Ignite 3

## Overview

This project demonstrates building a real-time transit monitoring application using Apache Ignite 3, showcasing distributed data processing, SQL querying, and continuous monitoring capabilities.

The application fetches real-time vehicle position data from GTFS (General Transit Feed Specification) feeds, stores it in a distributed Ignite database, and provides a live monitoring dashboard that tracks:

- Vehicle positions
- Route coverage
- Service disruptions
- System performance

## Features

- Real-time transit data ingestion
- Distributed data storage with Apache Ignite 3
- SQL-based analytics and monitoring
- Console dashboard with multiple views
- Automated service disruption detection

## Prerequisites

- Java 11 or later
- Maven 3.6+
- Docker and Docker Compose
- API key from a GTFS provider (e.g., 511.org)

## Tutorial

- [Introduction](docs/01-introduction.md)
- [Project Setup](docs/02-project-setup.md)
- [Understanding GTFS](docs/03-understanding-gtfs.md)
- [GTFS Client](docs/04-gtfs-client.md)
- [Data Ingestion](docs/05-data-ingestion.md)
- [Service Monitoring](docs/06-continuous-query.md)
- [Application Integration](docs/07-putting-together.md)

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

Edit `.env` and replace `your_token_here` with a valid GTFS API token.

### 3. Start Ignite Cluster

```bash
docker compose up -d
```

### 4. Initialize the cluster

```bash
docker run --rm -it --network=host -e LANG=C.UTF-8 -e LC_ALL=C.UTF-8 apacheignite/ignite:3.0.0 cli
connect http://localhost:10300
cluster init --name=ignite3 --metastorage-group=node1,node2,node3
exit
```

## Example Applications

The project includes several example applications to demonstrate different features:

### Connection Example

```bash
mvn compile exec:java@connect-example
```

### Schema Example

```bash
mvn compile exec:java@schema-setup-example
```

### GTFS Feed Example

```bash
mvn compile exec:java@gtfs-feed-example
```

### Ingestion Example

```bash
mvn compile exec:java@ingest-example
```

### Service Monitor Example

```bash
mvn compile exec:java@service-monitor-example
```

### Run the Service Monitor App

```bash
mvn compile exec:java@run-app
```

## Project Structure

- `src/main/java/com/example/transit/`
  - `app/`: Main application
  - `config/`: Configuration management
  - `examples/`: Demonstration applications
  - `model/`: Data models
  - `service/`: Business logic services
  - `util/`: Utility classes

## Technologies

- Apache Ignite 3
- Java
- Maven
- Docker
- GTFS-realtime
- Protocol Buffers

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

[Apache License 2.0](LICENSE)

## Acknowledgments

- Apache Ignite Community
- GTFS Specification Developers
- Transit Agencies Providing Open Data
