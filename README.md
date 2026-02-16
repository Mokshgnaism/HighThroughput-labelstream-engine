# High Throughput Label Ingestion Pipeline

## Overview

This project implements a high-performance hierarchical label generation and ingestion pipeline using Java structured concurrency and PostgreSQL bulk loading.


The system generates and processes a hierarchical logistics structure:

- Pallets
- Cartons (children of pallets)
- Units (children of cartons)

## NOTE : 
- enter through Main3 in org.example

Key goals:

- High throughput generation
- Parallel processing
- Efficient database ingestion
- Minimal transactional overhead
- Scalable producer-consumer architecture

The pipeline uses:

- Java Structured Concurrency (`StructuredTaskScope`)
- Multi-threaded workload partitioning
- Producer-consumer queues
- PostgreSQL `COPY` API for bulk inserts

---

## Architecture

### Stage 1 — Pallet Generation

Pallet identifiers are generated sequentially.

Output:

- List of pallet objects containing identifiers and hashes.

---

### Stage 2 — Carton Generation (Parallel)

Work is divided among multiple workers.

Each worker:

- Processes a chunk of pallets.
- Generates cartons associated with each pallet.

Parallel execution is handled using structured concurrency.

---

### Stage 3 — Pallet Database Insert

Batch insert using JDBC prepared statements.

Features:

- Batched inserts
- Transaction control
- Conflict handling (`ON CONFLICT DO NOTHING`)

---

### Stage 4 — Carton Bulk Insert

Cartons are inserted using PostgreSQL COPY API:

- Data buffered into CSV format.
- Sent to database via `CopyManager`.
- Each worker uses its own connection.

This significantly reduces insert latency compared to individual inserts.

---

### Stage 5 — Unit Generation and Ingestion (Producer-Consumer)

Pipeline model:

Producers:

- Generate units for cartons.
- Push results into a bounded blocking queue.

Consumers:

- Pull units from queue.
- Batch data into CSV buffer.
- Perform COPY ingestion when batch threshold reached.

Termination:

- Poison pill pattern used to signal shutdown.

Benefits:

- Decouples generation from database IO.
- Smooths load spikes.
- Improves throughput.

---

## Technologies

- Java (Structured Concurrency / Virtual Threads compatible design)
- PostgreSQL
- JDBC
- PostgreSQL CopyManager
- Concurrent Collections
- Producer-Consumer Pattern

---

## Database Schema

### pallets

```sql
CREATE TABLE pallets (
    ssic TEXT PRIMARY KEY,
    employee_id TEXT NOT NULL,
    factory_id TEXT NOT NULL,
    hash TEXT NOT NULL,
    hash_prefix TEXT NOT NULL
);
CREATE TABLE cartons (
    serial_id TEXT PRIMARY KEY,
    parent_pallet_id TEXT NOT NULL,
    hash TEXT NOT NULL,
    hash_prefix TEXT NOT NULL,
    FOREIGN KEY (parent_pallet_id) REFERENCES pallets(ssic)
);
CREATE TABLE units (
    serial_id TEXT PRIMARY KEY,
    parent_carton_id TEXT NOT NULL,
    hash TEXT NOT NULL,
    hash_prefix TEXT NOT NULL,
    FOREIGN KEY (parent_carton_id) REFERENCES cartons(serial_id)
);
```
## Benchmark

### Test Scenario

Hierarchical label generation and ingestion benchmark:

- Total labels processed: **5.51 million**

#### Structure

- Pallets → Cartons → Units

#### Configuration

- Hash generation enabled for all labels
- Multi-threaded generation using structured concurrency
- Bulk ingestion using PostgreSQL COPY API
- Producer-consumer pipeline for unit processing

---

### Result

- Total time: **under 32 seconds**

Includes:

- Label generation
- Hash computation
- Bulk database insertion

---

### Key Performance Factors

- COPY-based ingestion instead of row-by-row inserts
- Parallel workload partitioning
- Structured concurrency reducing thread management overhead
- Producer-consumer pipeline decoupling CPU and IO workloads
- Large batch sizes minimizing transaction overhead

