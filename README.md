# Text-to-SQL Converter using Spring Boot, MySQL, and Ollama

This project is a Java backend application that converts natural language queries into executable SQL queries using an LLM (like Mistral or LLaMA3) through the [Ollama](https://ollama.com) inference server.

It includes:

* Spring Boot REST API
* Ollama-powered LLM integration for text-to-SQL conversion
* MySQL for schema and query storage
* Flyway for DB migrations
* Hibernate for ORM
* Redis Caffeine for caching

---

## ✨ Features

* 🔎 **Schema Introspection**: Reads table & column metadata using `INFORMATION_SCHEMA`
* ✍️ **Natural Language to SQL**: Prompts the model to return SQL
* ✅ **Validation Layer**: Executes SQL safely and returns results
* 🎯 **Query History**: Stores query + response + execution metadata
* ⚡ **Fast**: Cached schema analysis, model base URL config, multi-profile Spring setup
* ⚙️ **Configurable**: Easy to swap model or prompt

---

## 🌐 API Endpoints

| Endpoint                           | Method | Description                                 |
| ---------------------------------- | ------ | ------------------------------------------- |
| `/api/v1/query/recent`             | GET    | Get recent queries                          |
| `/api/v1/query/history`            | GET    | Get query history                           |
| `/api/v1/query/explain/{queryId}`  | GET    | Get explanation for a specific query        |
| `/api/v1/query/text-to-sql`        | POST   | Convert natural language to SQL             |
| `/api/v1/query/execute`            | POST   | Execute a SQL query                         |
| `/api/v1/schema/tables`            | GET    | Get all table names in the database         |
| `/api/v1/schema/table/{tableName}` | GET    | Get schema information for a specific table |
| `/actuator/health`                 | GET    | Spring Actuator health check                |

---

## ⚖️ Technologies Used

* **Java 21**, **Spring Boot**
* **Spring Data JPA**, **Hibernate**
* **Flyway** for schema migration
* **MySQL 8** as RDBMS
* **Ollama** (Dockerized LLM runtime)
* **Caffeine Cache** for schema metadata
* **OpenAPI 3.0 (Springdoc)** for API documentation

---

## 🚀 Project Structure

```
text-to-sql
├── src/main/java/com/ai/texttosql
│   ├── config          # DB, security, Ollama client
│   ├── controller      # API endpoints
│   ├── model           # DTOs, schema models
│   ├── repository      # JPA interfaces
│   ├── service         # Business logic
│   └── exception       # Global error handler
│
├── src/main/resources
│   ├── application.yml
│   ├── application-local.yml
│   ├── application-prod.yml
│   └── db/migration/V1__init_schema.sql
│
├── docker-compose.yml             # For local dev (MySQL only)
├── docker-compose.prod.yml        # Full stack in Docker (Ollama + MySQL + App)
├── Dockerfile                     # Multi-stage Spring Boot image
├── pom.xml
└── README.md
```

---

## 🚧 How to Run

### 1. Local Dev Setup

```bash
# Start MySQL only (Ollama runs outside Docker)
docker compose up -d

# Run Ollama on host manually
ollama serve
ollama pull llama3.2

# Run Spring Boot from IDE with profile=local
```

### 2. Production Setup (All in Docker)

```bash
docker compose -f docker-compose.prod.yml up --build
```

---

## 🔊 Sample Request

**POST** `/api/v1/query/text-to-sql`

```json
{
  "naturalLanguageQuery": "Get all users created after 2022",
  "explainQuery": false,
  "includeSchemaContext": true
}
```

**Sample Prompt to Model:**

```txt
Given the schema: <...>, write a MySQL query for:
"Get all users created after 2022"
```

**Sample Response:**

```json
{
  "naturalLanguageQuery": "Get all users created after 2022",
  "generatedSql": "SELECT * FROM users WHERE created_at > '2022-01-01';",
  "explanation": "This query selects all users whose creation date is after January 1, 2022. It's written in SQL for relational databases like MySQL or PostgreSQL.",
  "results": null,
  "executionMetrics": {
    "executionTimeMillis": 7860,
    "resultCount": 0,
    "status": "COMPLETED"
  },
  "timestamp": "2025-06-25T15:13:09.5534426Z",
  "queryId": null,
  "error": null
}
```

---


---

## ⛏ TODO

* Add security (JWT + role-based access control)
* Integrate Slack bot to interact with backend via natural language
* Optimize prompts and query response logic
* Add more endpoints (e.g., saved queries, query editing)
* Add rate limiting and caching at API level
* Improve logging and monitoring (e.g., ELK, Prometheus/Grafana)



---

## 🌟 Credits

Built with ❤ by Anand Adhikari

---

## 👁️ License

MIT License
