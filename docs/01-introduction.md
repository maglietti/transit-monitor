# Your First Ignite App with Java: Public Transit Monitoring System

## Introduction

In this hands-on tutorial, you'll build a real-time transit monitoring system that demonstrates Apache Ignite 3's distributed data capabilities. Through this project, you'll experience firsthand how Ignite enables rapid development of applications that require high throughput, low latency, and resilient data processing.

Transit data provides an ideal context for learning Ignite because it combines several challenging characteristics found in modern applications:

- **Real-time data streams** that must be continuously processed
- **Time-series data** requiring efficient storage and retrieval
- **Geospatial components** for tracking vehicle locations
- **Complex queries** for analyzing system performance
- **Continuous monitoring** for detecting service anomalies

By completing this tutorial, you'll have:

- A functional transit monitoring application with source code
- A running Ignite cluster with populated data
- A collection of reusable code components for working with Ignite
- Practical experience with Ignite's core features
- Knowledge to build your own distributed data applications

You can complete the modules in a single session or spread them across multiple days. Each module builds incrementally on previous ones, with clear checkpoints to validate your progress.

## What You'll Build

By the end of this tutorial, you'll have created a complete transit monitoring dashboard that looks like this:

```text
╔══════════════════════════════════════════════════════╗
║            TRANSIT MONITORING DASHBOARD              ║
╚══════════════════════════════════════════════════════╝
Current time: 2023-05-21 14:22:38

SUMMARY VIEW
────────────────────────────────────────────────────────────────
ACTIVE VEHICLES BY ROUTE (last 15 minutes)
• Route 14      : 32 vehicles ↑
• Route 5       : 30 vehicles =
• Route 38      : 28 vehicles ↓
• Route 1       : 26 vehicles =
• Route 8       : 24 vehicles ↑

VEHICLE STATUS DISTRIBUTION
• IN_TRANSIT_TO  : 342 vehicles ↑
• STOPPED_AT     : 198 vehicles ↓

DATA INGESTION STATUS
• Status: Running
• Total records fetched: 45,280
• Total records stored: 45,280
• Last fetch count: 540
• Last fetch time: 876ms
• Running time: 01:23:45
• Ingestion rate: 9.12 records/second

Views rotate automatically every 10 seconds
Press ENTER at any time to exit
```

Your application will connect to real-time transit data feeds, store vehicle positions in a distributed Ignite database, analyze movement patterns, and alert operators to potential service disruptions. The system architecture will include:

- A clustered Ignite database for distributed storage
- A data ingestion pipeline for real-time updates
- SQL-based analytics for operational insights
- A monitoring system for service disruptions
- A real-time console dashboard for visualization

## What You'll Learn

Throughout this tutorial, you'll gain hands-on experience with key Apache Ignite 3 features:

### Data Modeling and Storage

- Creating tables using Ignite's Java API
- Defining appropriate schemas for time-series data
- Managing primary keys and indices for optimal performance

### Data Ingestion

- Building data pipelines with proper error handling
- Implementing efficient batch processing for high-throughput scenarios
- Using scheduled execution for continuous data updates

### Querying and Analysis

- Writing SQL queries against distributed data
- Using temporal functions for time-series analysis
- Implementing complex joins and aggregations for operational insights

### Monitoring and Alerting

- Creating monitoring systems using SQL-based polling
- Detecting anomalies in real-time data streams
- Building simple visualization components for system status

## Prerequisites

Before starting this tutorial, please ensure you have:

- **Java 11 or later** installed and properly configured
  - Verify with `java --version` in your terminal
- **Maven 3.6+** for dependency management
  - Verify with `mvn --version`
- **Docker 20.10+** and Docker Compose for running the Ignite 3 cluster
  - Verify with `docker --version` and `docker-compose --version`
- **IDE** such as IntelliJ IDEA or VS Code with Java extensions
- **Basic Java knowledge**, including familiarity with classes, interfaces, and collections
- **Some SQL experience** for understanding the query examples
- **Completed the "Use the Java API" How-To guide** (recommended but not required)

## Tutorial Flow

This tutorial is designed as a progressive journey through building a complete application.

Each module builds on the previous ones, introducing new concepts while reinforcing what you've already learned. The code examples are designed to work together as a cohesive application, but each component also illustrates standalone concepts that can be applied to other projects.

At the end of each module, you'll find clear checkpoints to validate your progress and ensure you're ready to move to the next section. If you encounter any issues, each module includes troubleshooting guidance to help you overcome common challenges.

 **Next Steps:** Continue to [Module 2: Project Setup and Configuration](02-project-setup.md) to set up our project structure and configure our Ignite cluster!
 