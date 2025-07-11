# Real-Time Query Understanding (QoU) System - Proof of Concept

This project is a sophisticated proof-of-concept for a real-time e-commerce query understanding (QoU) system built with Java 21, Spring Boot, and Elasticsearch. It demonstrates how to build a smart search API from the ground up, including advanced features like autocomplete, faceted navigation, and a self-contained, reliable testing suite using Testcontainers.

## Features

* **Query Understanding & Rewriting:** Takes a simple text query (e.g., "organic avocados") and intelligently rewrites it into a powerful, structured Elasticsearch DSL query.
* **Faceted Navigation:** The main search endpoint uses Elasticsearch aggregations to return not just product results, but also the data needed to build "Filter by Brand" and "Filter by Category" UI components.
* **High-Performance Autocomplete:** A dedicated `/suggest` endpoint using an optimized `completion` suggester in a separate index provides instantaneous search-as-you-type suggestions.
* **Realistic Data Seeding:** Automatically seeds the database on first run using the well-known Instacart Kaggle dataset (~50,000 products).
* **Automated Integration Testing:** Includes a robust testing suite using **Testcontainers**, which programmatically spins up a dedicated Elasticsearch instance for each test run, ensuring 100% reliable and isolated tests.

## High-Level Architecture

The system is designed with a decoupled, microservice-based approach. The main Java application handles the core search logic, while a dedicated Python service handles the computationally expensive Machine Learning tasks.

```mermaid
graph TD
    subgraph User
        U["User's Browser/App"]
    end
    subgraph "Java Application (QoU System on port 8080)"
        C["SearchController"]
        S["QueryUnderstandingService"]
        N["NerApiClient"]
        E["ElasticsearchClient"]
    end
    subgraph "Python ML Service (on port 8000)"
        NER["ner-api on Docker"]
    end
    subgraph "Data Store"
        ES["Elasticsearch on Docker"]
    end

    U -- "HTTP API Calls" --> C
    C --> S
    S -- "Calls client to get entities" --> N
    S -- "Calls client to get results" --> E
    N -- "HTTP POST to :8000/ent" --> NER
    NER -- "JSON with entities" --> N
    E -- "Elasticsearch DSL Query" --> ES
    ES -- "Products, Facets, Suggestions" --> E
```

### Detailed Data Flow

Here is a sequence diagram illustrating the data flow for a typical search query:

```mermaid
sequenceDiagram
    participant User
    participant JavaApp as Java Application (:8080)
    participant NerAPI as Python NER API (:8000)
    participant Elasticsearch as Elasticsearch (:9200)

    User->>+JavaApp: POST /api/v1/search (rawQuery)
    Note over JavaApp: QueryUnderstandingService receives request
    JavaApp->>+NerAPI: POST /ent (text)
    NerAPI-->>-JavaApp: Returns JSON with entities
    Note over JavaApp: QueryRewriteService builds ES query
    JavaApp->>+Elasticsearch: Sends Search Request
    Elasticsearch-->>-JavaApp: Returns Search Response
    JavaApp-->>-User: Returns Final JSON (FacetedSearchResponse)
```

## Tech Stack

* Java 21
* Spring Boot 3
* Gradle
* Elasticsearch 8.x
* Docker
* JUnit 5 & AssertJ (for testing)
* Testcontainers (for integration testing)

## Prerequisites

* JDK 21 or higher
* Docker Desktop (must be running for Testcontainers and Docker Compose)
* Python 3.9+ and `pandas` (for generating `patterns.json`)
  ```bash
  pip install pandas
  ```
* Python 3.9+ and `spacy` (for training the fine-tuned NER model)
  ```bash
  pip install spacy
  ```
* A tool for making API requests, like `curl` or Postman.

## Project Setup

1.  **Clone/Set Up Project:** Ensure all project files are in place.

2.  **Download Kaggle Dataset:** This project uses the [Instacart Market Basket Analysis dataset](https://www.kaggle.com/c/instacart-market-basket-analysis/data).
    * Download the zip file from Kaggle.
    * Unzip it and find the `products.csv` and `aisles.csv` files.
    * Place these two files inside the `src/main/resources/data/` directory. The application is configured to read from this exact location.

### Generating the fine-tuned NER model

If you want to use the custom-trained spaCy NER model, run:
```bash
python train_ner.py
```
This will produce the `fine_tuned_ner_model/` directory that is baked into the Docker image.

## Running the Application (Manual Mode)

This describes how to run the application and connect to a persistent Elasticsearch container that you manage yourself.

1.  **Start Elasticsearch and NER API containers:**
    ```bash
    docker-compose up --build -d
    ```

2.  **Run the Spring Boot App:**
    ```bash
    ./gradlew bootRun
    ```
    On startup, the application will automatically seed the `products_index` and `suggestions_index` if they do not already exist, using the CSV data in `src/main/resources/data/`.

## Running the Automated Tests

This is the recommended way to verify all functionality. You do **not** need to run `docker-compose` for this.

1.  **Ensure Docker is running.**
2.  Execute the following command in your terminal:
    ```bash
    ./gradlew test
    ```
    Testcontainers will automatically start a fresh, temporary Elasticsearch container, the `DataSeeder` will populate it, all unit and integration tests will run against it, and then the container will be destroyed. This provides a completely clean and reliable test run every time.

## API Endpoints

The application exposes three distinct API endpoints.

### 1. Main Search (with Facets)

This is the primary endpoint for searching for products.

* **Endpoint:** `POST /api/v1/search`
* **Description:** Takes a raw query and returns a list of matching products along with faceted data for filtering.
* **Example Request:**
    ```bash
    curl --location 'http://localhost:8080/api/v1/search' \
    --header 'Content-Type: application/json' \
    --data '{
        "rawQuery": "organic milk"
    }'
    ```
* **Sample Response:**
    ```json
    {
      "products": [
        {
          "productId": "instacart-38689",
          "name": "Organic Whole Milk",
          "brand": "Horizon Organic",
          "categories": ["milk"],
          ...
        }
      ],
      "facets": [
        {
          "name": "Category",
          "values": [
            { "value": "milk", "count": 25 },
            { "value": "cream", "count": 10 },
            ...
          ]
        },
        {
          "name": "Brand",
          "values": [
            { "value": "Horizon Organic", "count": 8 },
            { "value": "Private Label", "count": 5 },
            ...
          ]
        }
      ]
    }
    ```

### 2. Autocomplete Suggestions

This endpoint provides fast search-as-you-type suggestions.

* **Endpoint:** `GET /api/v1/suggest`
* **Description:** Takes a `prefix` query parameter and returns a list of matching product name suggestions.
* **Example Request:**
    ```bash
    curl 'http://localhost:8080/api/v1/suggest?prefix=avo'
    ```
* **Sample Response:**
    ```json
    [
        "Avocado & Greens Juice",
        "Avocado Oil Canyon Cut Kettle Cooked Potato Chips ",
        "Avocado Roll",
        "Avocado, Jack and Tomato Sandwich"
    ]
    ```

### 3. Debugging Endpoint

This special endpoint is for developers to inspect the raw Elasticsearch query being generated.

* **Endpoint:** `POST /api/v1/understand-query`
* **Description:** Returns the complete Elasticsearch DSL query object that is constructed from the raw query.
* **Example Request:**
    ```bash
    curl --location 'http://localhost:8080/api/v1/understand-query' \
    --header 'Content-Type: application/json' \
    --data '{
        "rawQuery": "organic avocados"
    }'
    ```

## API Documentation (Swagger / OpenAPI)

Once the application is running, you can view the generated OpenAPI definition and interactive docs:

- **OpenAPI JSON spec:** `http://localhost:8080/v3/api-docs`
- **Swagger UI:** `http://localhost:8080/swagger-ui.html` or `http://localhost:8080/swagger-ui/index.html`
