# stage_2
# Big Data Project - Stage 2: Service-Oriented Architecture

This repository contains the implementation of a Service-Oriented Architecture (SOA) for a search engine project, as part of the Big Data course at Universidad de Las Palmas de Gran Canaria (ULPGC). The system is built using Java 17 and the lightweight Javalin framework, following microservice principles with RESTful APIs and JSON data exchange.

The architecture consists of four independent services:
- **Ingestion Service**: Downloads books from Project Gutenberg and stores them in the datalake.
- **Indexing Service**: Extracts metadata and builds an inverted index from the raw text.
- **Search Service**: Provides keyword search and filtering capabilities over the indexed data.
- **Control Module**: Orchestrates the entire pipeline (Ingestion → Indexing → Search).

This stage focuses on transforming the static data layer from Stage 1 into a dynamic, distributed ecosystem capable of handling concurrent requests and providing insights into system performance through benchmarking.

---

## Getting Started

### Prerequisites

- Java 17 (OpenJDK or Eclipse Temurin)
- Maven 3.9+
- Docker (optional, for containerized deployment)

### Building the Services

Each service is a standalone Maven project. To build all services:

```bash
# Build all modules
mvn clean package

# Or build individual services
cd ingestion_service && mvn clean package
cd indexing_service && mvn clean package
cd search_service && mvn clean package
cd control_module && mvn clean package
