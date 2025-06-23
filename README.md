# Real-Time Query Understanding (QoU) System - Proof of Concept (Java 21)

This project is a single-module Spring Boot application demonstrating a lean, real-time query understanding (QoU) layer for e-commerce search, built with Gradle and **Java 21**. It consolidates the microservice architecture discussed into a single, runnable application for ease of testing and demonstration.

## Architecture Overview

The application simulates the microservice flow internally:
1.  **API Endpoint (`/api/v1/search/understand`)**: Receives the raw user query.
2.  **Query Ingestion & Pre-processing**: Cleans and normalizes the input.
3.  **Intent & Entity Service**: Stubs the detection of intent and extraction of entities using simple keyword matching.
4.  **Query Enrichment & Rewriting Service**: Augments the query and constructs a final Elasticsearch DSL query.

## Prerequisites

* **JDK 21** or higher
* **Docker** and **Docker Compose**
* An API testing tool like [Postman](https://www.postman.com/) or a command-line tool like `curl`.

## How to Run

### 1. Start Elasticsearch

The project includes a `docker-compose.yml` file to easily start an Elasticsearch container.

From the project root directory, run:
```bash
docker-compose up -d