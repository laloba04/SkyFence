# 🚁 DroneTrack

Sistema de monitorización de aeronaves en tiempo real con detección automática de intrusiones en zonas restringidas.

## Stack

- Backend: Java 17, Spring Boot, JPA/Hibernate, WebSocket, Swagger/OpenAPI
- Frontend: React 18, Vite, Leaflet, STOMP/SockJS
- Datos: PostgreSQL
- Testing: JUnit 5, Mockito, MockMvc
- Deploy local: Docker + docker-compose

## Configuración segura de entorno

1. Copia `.env.example` a `.env`.
2. Completa **todos** los campos con valores reales en tu entorno local.
3. No subas `.env` al repositorio.

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


## Prevención de secretos en commits

Se añadieron hooks de `pre-commit` para escanear secretos antes de cada commit.

```bash
pip install pre-commit
pre-commit install
pre-commit run --all-files
```


## CI de seguridad

El repositorio incluye un workflow de GitHub Actions (`.github/workflows/secret-scan.yml`) que ejecuta **gitleaks** en cada push/PR.


## PR limpio tras remediación de secretos

Si vas a cerrar y reabrir el PR, puedes limpiar el historial para evitar que GitGuardian vuelva a escanear commits antiguos:

```bash
./scripts/prepare-clean-pr.sh origin/main
git push --force-with-lease
```
