# Stage_2

## Big Data Project - Stage 2: Service-Oriented Architecture

This repository contains the implementation of a **Service-Oriented Architecture (SOA)** for a distributed search engine, developed as part of the *Big Data* course (*Grado en Ciencia e Ingeniería de Datos*) at **Universidad de Las Palmas de Gran Canaria (ULPGC)**.

The system is built using **Java 17** and the lightweight **Javalin** framework, following microservice principles with RESTful APIs and JSON as the data exchange format.  
It transforms the static datalake and datamart from Stage 1 into a dynamic, scalable, and modular ecosystem.

---

##  System Architecture

The architecture is composed of four independent microservices, each responsible for a specific stage of the data pipeline:

- **Ingestion Service:** Downloads raw books from Project Gutenberg and stores them in the datalake using the hierarchical structure defined in Stage 1.  
- **Indexing Service:** Processes raw text, extracts metadata (title, author), tokenizes content, and builds an inverted index stored in the datamart.  
- **Search Service:** Exposes a query API to search and filter indexed books by keyword, author, language, or year.  
- **Control Module:** Orchestrates the full pipeline: triggers ingestion → indexing → makes data available for search.

All services communicate exclusively via **HTTP/JSON**, enabling loose coupling, independent deployment, and fault isolation.

---

##  Getting Started

### Prerequisites

- Java 17 (OpenJDK, Eclipse Temurin, etc.)
- Maven 3.9+
- Git
- *(Optional)* Docker and Docker Compose for containerized execution

---

##  Project Structure


Each service is a standalone **Maven module** with its own `pom.xml` and `App.java`.

---

##  Building and Running

### Manual Execution (Recommended for Development)

**Build all services:**
```bash
mvn clean package
```
**Run each service in a separate terminal:**
```bash
# Terminal 1: Ingestion Service
java -jar ingestion_service/target/ingestion_service-1.0-jar-with-dependencies.jar

# Terminal 2: Indexing Service
java -jar indexing_service/target/indexing_service-1.0-jar-with-dependencies.jar

# Terminal 3: Search Service
java -jar search_service/target/search_service-1.0-jar-with-dependencies.jar

# Terminal 4: Control Module
java -jar control_module/target/control_module-1.0-jar-with-dependencies.jar
```
### Docker Compose (Production-like Environment)

**Build and start all services:**
```bash
docker-compose up --build
```
**Stop services:**
```bash
docker-compose down
```

## API Specification

### Ingestion Service
| Method | Endpoint | Description |
|--------|-----------|--------------|
| **POST** | `/ingest/{book_id}` | Downloads book from Project Gutenberg |
| **GET** | `/ingest/status/{book_id}` | Checks if book is in datalake |
| **GET** | `/ingest/list` | Lists all downloaded books |

---

### Indexing Service
| Method | Endpoint | Description |
|--------|-----------|--------------|
| **POST** | `/index/update/{book_id}` | Indexes a specific book |
| **POST** | `/index/rebuild` | Rebuilds index for all books |
| **GET** | `/index/status` | Returns indexing statistics |

---

### Search Service
| Method | Endpoint | Description |
|--------|-----------|--------------|
| **GET** | `/search?q={term}` | Searches by keyword |
| **GET** | `/search?q={term}&author={name}` | Filters by author |
| **GET** | `/search?q={term}&language={code}` | Filters by language *(ISO 639-1)* |
| **GET** | `/search?q={term}&year={YYYY}` | Filters by publication year |

---

###  Control Module
| Method | Endpoint | Description |
|--------|-----------|--------------|
| **POST** | `/pipeline/{book_id}` | Executes full pipeline: ingest → index |
| **GET** | `/status` | Health check |

## Benchmarking

The project includes **JMH (Java Microbenchmark Harness)** benchmarks to evaluate performance of core operations:

| Benchmark Area | Description |
|----------------|--------------|
| **Text tokenization** | Splits raw text into analyzable tokens. |
| **Metadata extraction** | Extracts structured information (title, author, language, year). |
| **Index lookup** | Tests the speed and efficiency of inverted index searches. |
| **Query filtering** | Evaluates the performance of applying filters (author, language, year). |

**Run benchmarks:**
```bash
mvn clean package
java -jar target/benchmarks.jar
```
## Design Decisions

| Decision | Rationale |
|-----------|------------|
| **Microservices over Monolith** | Enables independent scaling, deployment, and technology evolution. |
| **Javalin** | Chosen for its simplicity, low overhead, and native JSON support. |
| **File-based Storage** | Uses `datalake/` and `datamart/` directories for persistence (Stage 1 compatibility). |
| **Orchestration over Event-Driven** | The Control Module simplifies debugging and benchmarking. |
| **No External Databases** | Keeps the system lightweight and self-contained for academic purposes. |

---

## Team

| Role | Information |
|------|--------------|
| **Group Name** | BD-Power |
| **Repository** | [https://github.com/BD-Power/stage_2](https://github.com/BD-Power/stage_2) |
| **Course** | Big Data – GCID, ULPGC (2025) |



