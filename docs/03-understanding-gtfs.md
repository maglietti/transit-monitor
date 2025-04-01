# Understanding GTFS Data and Creating the Transit Schema

In this module, you'll learn about the General Transit Feed Specification (GTFS) data format and how to model the data within Apache Ignite. You'll create a schema that enables efficient storage and querying of transit vehicle positions.

## The GTFS Format: Transit Data in Motion

The [General Transit Feed Specification](https://gtfs.org) (GTFS) has become the universal language of public transportation data. Created through a collaboration between Google and Portland's TriMet transit agency in 2006, it's now the industry standard used by transit agencies worldwide to share transit information in a consistent, machine-readable format.

GTFS comes in two formats:

1. **GTFS Static**: The foundation of transit data, containing:
   - Route definitions (paths that vehicles travel)
   - Stop locations (where vehicles pick up passengers)
   - Schedules (when vehicles are expected at stops)
   - Fares (how much it costs to ride)

2. **GTFS Realtime**: The dynamic extension that provides near real-time updates:
   - Vehicle Positions (where vehicles are right now)
   - Service Alerts (disruptions, detours, etc.)
   - Trip Updates (predictions of arrival/departure times)

For our transit monitoring system, we'll focus on the **Vehicle Positions** component of GTFS Realtime. This gives us a continuous stream of data points showing where each transit vehicle is located, what route it's serving, and its current status (in transit, stopped at a location, etc.).

## Analyzing the Data: What's in a Vehicle Position?

Before designing our schema, let's examine what information is available in a GTFS vehicle position record:

| Field | Description | Example | Will We Use It? |
|-------|-------------|---------|----------------|
| Vehicle ID | Unique identifier for the vehicle | "1234" | Yes - Primary key |
| Route ID | Identifier for the route the vehicle is servicing | "42" | Yes - For filtering |
| Trip ID | Identifier for the specific trip being made | "trip_morning_1" | No - Not needed for monitoring |
| Position | Latitude and longitude coordinates | (37.7749, -122.4194) | Yes - For mapping |
| Timestamp | When the position was recorded | 1616724123000 | Yes - Primary key component |
| Status | Current status of the vehicle | "IN_TRANSIT_TO", "STOPPED_AT" | Yes - For monitoring |
| Stop ID | Identifier of the stop if the vehicle is stopped | "stop_downtown_3" | No - Not needed for basic monitoring |
| Congestion Level | Level of traffic congestion | "RUNNING_SMOOTHLY" | No - Not in scope |
| Occupancy Status | How full the vehicle is | "MANY_SEATS_AVAILABLE" | No - Not in scope |

For our application, we'll focus on the most essential fields: vehicle ID, route ID, position coordinates, timestamp, and status. This gives us the core information needed for monitoring while keeping our schema clean and focused.

## Ignite 3 Annotation System

Apache Ignite 3 provides a rich annotation system that allows you to define database schemas directly in your Java code. This approach creates a clear mapping between your application objects and database tables, offering:

- **Type safety**: Compile-time checking prevents many schema-related errors
- **Co-location of code and schema**: Changes to objects automatically reflect in the schema
- **Reduced boilerplate**: No need for separate SQL schema definitions
- **IDE support**: Code completion and refactoring tools help maintain consistency

### Core Annotations

Ignite 3 provides several key annotations for defining your database schema:

| Annotation | Purpose | Location |
|------------|---------|----------|
| `@Table` | Marks a class as an Ignite table | Class level |
| `@Id` | Designates a field as part of the primary key | Field level |
| `@Column` | Maps a field to a database column | Field level |
| `@Zone` | Specifies the distribution zone for the table | Inside `@Table` |
| `@Index` | Creates a secondary index on columns | Inside `@Table` |
| `@ColumnRef` | References a column in an index or co-location | Inside `@Index` or co-location |

## Creating the Model Class with Annotations

Let's examine the `VehiclePosition.java` file from the repository:

```shell
open src/main/java/com/example/transit/model/VehiclePosition.java
```

Here's the key part of the class with the annotations:

```java
@Table(
        zone = @Zone(value = "transit", storageProfiles = "default"),
        indexes = {
                @Index(value = "IDX_VP_ROUTE_ID", columns = { @ColumnRef("route_id") }),
                @Index(value = "IDX_VP_STATUS", columns = { @ColumnRef("current_status") })
        }
)
public class VehiclePosition {
    @Id
    @Column(value = "vehicle_id", nullable = false)
    private String vehicleId;

    @Id
    @Column(value = "time_stamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(value = "route_id", nullable = false)
    private String routeId;

    @Column(value = "latitude", nullable = false)
    private Double latitude;

    @Column(value = "longitude", nullable = false)
    private Double longitude;

    @Column(value = "current_status", nullable = false)
    private String currentStatus;
    
    // Constructors, getters, setters...
}
```

Let's break down each annotation in detail:

### @Table Annotation

```java
@Table(
    zone = @Zone(value = "transit", storageProfiles = "default"),
    indexes = {
        @Index(value = "IDX_VP_ROUTE_ID", columns = { @ColumnRef("route_id") }),
        @Index(value = "IDX_VP_STATUS", columns = { @ColumnRef("current_status") })
    }
)
```

The `@Table` annotation marks this class as a database table in Ignite:

- **zone**: Specifies which distribution zone the table belongs to
- **indexes**: Defines secondary indexes for query optimization
- The table name defaults to the class name ("VehiclePosition")

### @Zone Annotation

```java
@Zone(value = "transit", storageProfiles = "default")
```

The `@Zone` annotation defines the distribution properties:

- **value**: The name of the zone ("transit")
- **storageProfiles**: The storage profile to use ("default" in this case)

Zones control how data is distributed across the cluster, including:

- How many partitions the data is split into
- How many replicas exist for redundancy
- Which storage engines and policies are used

### @Index Annotation

```java
@Index(value = "IDX_VP_ROUTE_ID", columns = { @ColumnRef("route_id") })
```

The `@Index` annotation creates secondary indexes:

- **value**: A unique name for the index
- **columns**: The columns to index, referenced by their database names

Indexes improve query performance when filtering or sorting on specific columns.

### @Id Annotation

```java
@Id
@Column(value = "vehicle_id", nullable = false)
private String vehicleId;

@Id
@Column(value = "time_stamp", nullable = false)
private LocalDateTime timestamp;
```

The `@Id` annotation marks a field as part of the primary key:

- Multiple `@Id` annotations create a composite primary key
- Primary key fields should be marked as `nullable = false`
- Order matters - the first `@Id` field is the most significant in the key

### @Column Annotation

```java
@Column(value = "route_id", nullable = false)
private String routeId;
```

The `@Column` annotation maps a Java field to a database column:

- **value**: The column name in the database
- **nullable**: Whether NULL values are allowed (default is true)

Additional attributes available (not used in our example):

- **length**: For string columns, specifies the maximum length
- **precision** and **scale**: For decimal values, controls numeric precision

## Understanding the Table Manager

The repository includes a `VehiclePositionTableManager` class that handles creating the schema. Let's examine it:

```shell
open src/main/java/com/example/transit/config/VehiclePositionTableManager.java
```

The key method in this class is `createSchema()`:

```java
public boolean createSchema() {
    try {
        IgniteClient client = connectionManager.getClient();

        // Check if table exists
        if (tableExists(client, VEHICLE_POSITIONS_TABLE)) {
            System.out.println("--- Vehicle positions table already exists");
            return true;
        }

        // Create zone if it doesn't exist
        System.out.println(">>> Creating 'transit' zone if it doesn't exist");
        ZoneDefinition transitZone = ZoneDefinition.builder("transit")
                .ifNotExists()
                .replicas(2)
                .storageProfiles("default")
                .build();
        client.catalog().createZone(transitZone);

        // Create table
        System.out.println(">>> Creating table: " + VEHICLE_POSITIONS_TABLE);
        client.catalog().createTable(VehiclePosition.class);

        return true;
    } catch (Exception e) {
        logger.error("Failed to create schema: {}", e.getMessage());
        return false;
    }
}
```

This method:

1. Checks if the table already exists to avoid duplicate creation
2. Creates a distribution zone named "transit" with 2 replicas for redundancy
3. Creates the `VehiclePosition` table using the annotations in the POJO class

The method `client.catalog().createTable(VehiclePosition.class)` reads the annotations from the `VehiclePosition` class and creates a corresponding table in the Ignite cluster.

### From Annotations to SQL DDL

Under the hood, Ignite translates the annotated class into SQL DDL statements. Our `VehiclePosition` class would generate something like:

```sql
-- Create the zone if it doesn't exist
CREATE ZONE IF NOT EXISTS transit 
WITH STORAGE_PROFILES='default', REPLICAS=2;

-- Create the table
CREATE TABLE VehiclePosition (
    vehicle_id VARCHAR NOT NULL,
    time_stamp TIMESTAMP NOT NULL,
    route_id VARCHAR NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    current_status VARCHAR NOT NULL,
    PRIMARY KEY (vehicle_id, time_stamp)
) ZONE transit;

-- Create the indexes
CREATE INDEX IDX_VP_ROUTE_ID ON VehiclePosition(route_id);
CREATE INDEX IDX_VP_STATUS ON VehiclePosition(current_status);
```

This SQL equivalent shows how the annotations map to standard SQL DDL statements.

## Schema Design Decisions Explained

Let's look at some key decisions in our schema design:

1. **Composite Primary Key**:
   We defined a primary key consisting of `vehicle_id` and `time_stamp`. This allows us to:
   - Store multiple positions for the same vehicle (at different times)
   - Efficiently query the history of a specific vehicle
   - Enforce uniqueness for each vehicle's position at a given time

2. **Column Types**:
   - `VARCHAR` for string identifiers (vehicle_id, route_id, current_status)
   - `DOUBLE` for precise geographic coordinates
   - `TIMESTAMP` for temporal data, which allows for SQL time functions and comparisons

3. **Distribution Zone**:
   The "transit" zone with 2 replicas provides:
   - Data redundancy (each record exists on two nodes)
   - Fault tolerance (the system continues if one node fails)
   - Load balancing (queries can be directed to either node)

4. **Indexes**:
   We created two secondary indexes:
   - On `route_id` to quickly find all vehicles on a specific route
   - On `current_status` to easily filter by vehicle status (stopped, in transit, etc.)

## Interacting with Ignite

Let's see how we can interact with our schema by running the schema setup example:

```bash
mvn compile exec:java@schema-setup-example
```

This command uses the predefined execution in the Maven POM file to run the `SchemaSetupExample` class. Let's examine this file:

```shell
open src/main/java/com/example/transit/examples/SchemaSetupExample.java
```

The example performs a complete cycle of operations:

1. Connects to the Ignite cluster using the connection manager
2. Creates the schema using the `VehiclePositionTableManager`
3. Tests CRUD (Create, Read, Update, Delete) operations on the `VehiclePosition` table

Let's look at the key table operations in this example:

```java
// Get table and record view
Table vehicleTable = client.tables().table(VEHICLE_TABLE);
RecordView<VehiclePosition> vehicleView = vehicleTable.recordView(VehiclePosition.class);

// Insert test record
System.out.println(">>> Inserting test vehicle: " + testVehicle.getVehicleId());
vehicleView.upsert(null, testVehicle);

// Retrieve the record
VehiclePosition keyVehicle = new VehiclePosition();
keyVehicle.setVehicleId(testVehicle.getVehicleId());
keyVehicle.setTimestamp(testVehicle.getTimestamp());

VehiclePosition retrievedVehicle = vehicleView.get(null, keyVehicle);
```

This code demonstrates:

1. **Obtaining a Table Reference**: `client.tables().table(VEHICLE_TABLE)`
2. **Creating a RecordView**: A typed interface for working with table records
3. **Upserting Data**: Adding a record to the table
4. **Retrieving Data**: Reading a record by its primary key
5. **Modifying and Deleting Data**: The example also shows updating and deleting records

The `RecordView` interface allows us to work with POJOs directly, providing a type-safe way to interact with the database. Ignite handles the mapping between Java objects and database records based on the annotations we defined.

### Working with Primary Keys

When retrieving or deleting records, you only need to set the primary key fields in your POJO. For our composite key:

```java
// Create an object with just the primary key fields
VehiclePosition keyVehicle = new VehiclePosition();
keyVehicle.setVehicleId(testVehicle.getVehicleId());
keyVehicle.setTimestamp(testVehicle.getTimestamp());

// Use it to retrieve a record
VehiclePosition retrievedVehicle = vehicleView.get(null, keyVehicle);
```

This pattern is common in Ignite applications - create a minimal object with just the key fields set.

## Using SQL with the Schema

While the POJO annotation approach provides a type-safe way to define and interact with our schema, we can also use SQL to query the data. The `SchemaSetupExample` demonstrates both approaches:

```java
// SQL query to verify deletion
var countResult = client.sql().execute(null,
        "SELECT COUNT(*) as cnt FROM " + VEHICLE_TABLE +
                " WHERE vehicle_id = ?", testVehicle.getVehicleId());

long count = 0;
if (countResult.hasNext()) {
    count = countResult.next().longValue("cnt");
}
```

This SQL query counts records matching a specific vehicle ID, demonstrating how we can combine the strongly-typed POJO approach with flexible SQL queries.

## Best Practices for Annotation Usage

Based on industry experience, here are some best practices for using Ignite 3 annotations:

1. **Use meaningful and consistent names**:
   - Follow a naming convention for tables, indexes, and columns
   - Consider using the same name for related columns across tables

2. **Always mark primary key fields with `@Id`**:
   - Primary keys should be marked as `nullable = false`
   - For composite keys, mark all component fields with `@Id`

3. **Choose appropriate data types**:
   - For nullable fields, use object types (Double) instead of primitives (double)
   - Use `java.time` classes for date/time fields (like LocalDateTime)

4. **Optimize index design**:
   - Only index columns used in WHERE clauses or joins
   - Create indexes on columns with high cardinality (many unique values)
   - Name indexes clearly to indicate their purpose

5. **Design distribution zones based on query patterns**:
   - Group related tables in the same zone
   - Choose appropriate replica counts based on data importance
   - Consider data size when setting partition counts

## Next Steps

Congratulations! You've now:

1. Understood the structure of GTFS transit data
2. Learned about Ignite 3's annotation system for schema definition
3. Examined the `VehiclePosition` model class with detailed annotation explanations
4. Seen how annotations are translated into database schema
5. Explored basic operations for working with the schema
6. Learned best practices for using annotations in your projects

This schema provides the foundation for our transit monitoring system. In the next module, we'll build a client to fetch real-time GTFS data from a transit agency and feed it into our Ignite database.

**Next Steps:** Continue to [Module 4: Building the GTFS Client](04-gtfs-client.md)
