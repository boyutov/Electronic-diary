# Electronic Diary (School Management System)

Backend service for an electronic diary / school management system.

## Tech stack
- Java 17, Spring Boot
- Spring Security
- Spring Data JPA, PostgreSQL
- Flyway
- Swagger (springdoc-openapi)

## Requirements
- Java 17
- Docker + Docker Compose

## Configuration
The application uses environment variable:

- `DB_PASSWORD` — PostgreSQL user password

Create `.env` file in the project root based on `.env.example`.

## Run database
```bash
docker compose up -d
```

Run app:
```bash
mvn spring-boot:run
```

Swagger: http://localhost:8088/swagger-ui/index.html