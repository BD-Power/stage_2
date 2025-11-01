# stage_2
# Big Data Project - Stage 2: Service-Oriented Architecture

This repository contains the implementation of a **Service-Oriented Architecture (SOA)** for a distributed search engine, developed as part of the **Big Data course** (Grado en Ciencia e Ingeniería de Datos) at **Universidad de Las Palmas de Gran Canaria (ULPGC)**.

The system is built using **Java 17** and the lightweight **Javalin** framework, following microservice principles with **RESTful APIs** and **JSON** as the data exchange format. It transforms the static datalake and datamart from Stage 1 into a dynamic, scalable, and modular ecosystem.

---

## System Architecture

The architecture is composed of **four independent microservices**, each responsible for a specific stage of the data pipeline:

- **Ingestion Service**: Downloads raw books from [Project Gutenberg](https://www.gutenberg.org/) and stores them in the `datalake` using the hierarchical structure defined in Stage 1.
- **Indexing Service**: Processes raw text, extracts metadata (title, author), tokenizes content, and builds an inverted index stored in the `datamart`.
- **Search Service**: Exposes a query API to search and filter indexed books by keyword, author, language, or year.
- **Control Module**: Orchestrates the full pipeline: triggers ingestion → indexing → makes data available for search.

All services communicate **exclusively via HTTP/JSON**, enabling loose coupling, independent deployment, and fault isolation.

---

## Getting Started

### Prerequisites

- **Java 17** (OpenJDK, Eclipse Temurin, etc.)
- **Maven 3.9+**
- **Git**
- (Optional) **Docker** and **Docker Compose** for containerized execution

### Project Structure
