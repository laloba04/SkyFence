# 🚁 DroneTrack

Sistema de monitorización de aeronaves en tiempo real con detección automática de intrusiones en zonas restringidas.

## Stack

- Backend: Java 17, Spring Boot, JPA/Hibernate, WebSocket, Swagger/OpenAPI
- Frontend: React 18, Vite, Leaflet, STOMP/SockJS
- Datos: PostgreSQL
- Testing: JUnit 5, Mockito, MockMvc
- Deploy local: Docker + docker-compose

## Configuración de secretos

1. Copia `.env.example` a `.env`.
2. Define un valor fuerte para `POSTGRES_PASSWORD`.

```bash
cp .env.example .env
```

## Arranque rápido

```bash
docker-compose up --build
```

- API: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html
- Frontend: http://localhost:3000

## Tests

```bash
cd backend
mvn test
```
