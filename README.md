# Securities-Trade-Processing-System
The Trade Post Processing System is a Spring Boot microservice that simulates trade execution, processes trades using Kafka, and stores them in PostgreSQL for retrieval and analysis. It includes real-time trade validation, logging, and settlement processing.

## Observability

- Logs go through SLF4J only and are written as JSON (logstash-logback-encoder, `src/main/resources/logback-spring.xml`) with `service`, `level`, `logger`, `message`, `correlationId` and `traceId` fields.
- `CorrelationIdFilter` reads `X-Correlation-Id` from the request (or generates a UUID), stores it in the MDC as `correlationId` and echoes it on the response.
- Spring Boot Actuator exposes `/actuator/health` (with `/liveness` and `/readiness` probes), `/actuator/info` and `/actuator/metrics`.
