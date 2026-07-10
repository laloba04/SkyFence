# SkyFence

> Detección de intrusiones en zonas restringidas en tiempo real

Sistema de monitorización de aeronaves que consume datos reales de [adsb.fi](https://opendata.adsb.fi), aplica geofencing con la fórmula de Haversine sobre zonas sensibles (aeropuertos, bases militares, centrales nucleares) y emite alertas instantáneas al frontend vía WebSocket/STOMP.

![Java](https://img.shields.io/badge/Java_17-ED8B00?style=flat&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3-6DB33F?style=flat&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=flat&logo=postgresql&logoColor=white)
![React](https://img.shields.io/badge/React_18-20232A?style=flat&logo=react&logoColor=61DAFB)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat&logo=docker&logoColor=white)

[![CI/CD Pipeline](https://github.com/laloba04/SkyFence/actions/workflows/ci.yml/badge.svg)](https://github.com/laloba04/SkyFence/actions/workflows/ci.yml)
[![CodeQL](https://github.com/laloba04/SkyFence/actions/workflows/codeql.yml/badge.svg)](https://github.com/laloba04/SkyFence/actions/workflows/codeql.yml)
[![Secret Scan](https://github.com/laloba04/SkyFence/actions/workflows/secret-scan.yml/badge.svg)](https://github.com/laloba04/SkyFence/actions/workflows/secret-scan.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=laloba04_SkyFence&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=laloba04_SkyFence)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=laloba04_SkyFence&metric=bugs)](https://sonarcloud.io/summary/new_code?id=laloba04_SkyFence)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=laloba04_SkyFence&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=laloba04_SkyFence)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=laloba04_SkyFence&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=laloba04_SkyFence)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=laloba04_SkyFence&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=laloba04_SkyFence)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=laloba04_SkyFence&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=laloba04_SkyFence)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=laloba04_SkyFence&metric=coverage)](https://sonarcloud.io/summary/new_code?id=laloba04_SkyFence)

## Demo en producción

| Servicio | URL |
|----------|-----|
| **Frontend** | https://sky-fence.vercel.app |
| **Backend API** | https://skyfence-backend.onrender.com |
| **Swagger UI** | https://skyfence-backend.onrender.com/swagger-ui/index.html |
| **Health check** | https://skyfence-backend.onrender.com/actuator/health |
| **Grafana (métricas)** | https://skyfence-grafana.onrender.com/d/skyfence-backend/skyfence-backend |
| **Prometheus** | https://skyfence-prometheus.onrender.com |

> El backend en Render puede tardar ~30 s en responder si estuvo inactivo (free tier).

---

## Vista previa

![Mapa en tiempo real](docs/screenshot-map.png)

![Swagger API](docs/screenshot-swagger.png)

---

## Funcionalidades Principales

- **Monitorización en tiempo real:** Mapa interactivo con todas las aeronaves (actualizado vía adsb.fi).
- **Geofencing Dinámico (Haversine):** Cálculo de distancias a zonas restringidas como aeropuertos o bases de seguridad.
- **Gestor de Zonas:** Panel integrado en UI para crear y eliminar zonas al vuelo con efecto inmediato.
- **Simulador de Intrusiones:** Inyección de drones de prueba para validar el disparo visual y persistencia de alertas.
- **Alertas STOMP / WebSocket:** Notificaciones asíncronas de bajísima latencia sin recargar la web.
- **Histórico de alertas:** Consulta paginada con filtros por severidad y rango de horas, persistido en PostgreSQL.
- **Rate Limiting Anti-DoS:** Protección proactiva de la API limitando peticiones abusivas (Bucket4j).
- **Observabilidad completa:** Stack Prometheus + Grafana + Loki + Promtail + Dozzle con dashboards preconfigurados.

---

## Arquitectura y flujo del sistema

```
adsb.fi API
        │  (cada 10 segundos)
        ▼
  FlightDataService  ──► GeofenceService (Haversine)
                              │
                    aeronave dentro de zona?
                              │
                        AlertService
                              │
                    WebSocket /topic/alerts
                              │
                        Frontend React
                    (mapa actualizado en tiempo real)
```

1. El backend consulta adsb.fi cada 10 segundos filtrando el espacio aéreo de España.
2. Por cada aeronave, calcula la distancia a todas las zonas restringidas con la fórmula de Haversine.
3. Si una aeronave está dentro del radio de una zona, genera una alerta con severidad `HIGH` o `MEDIUM`.
4. La alerta se publica por WebSocket al frontend de forma instantánea.
5. El frontend actualiza el mapa en tiempo real: marcador rojo para aeronaves en alerta.
6. Los datos persisten en PostgreSQL entre reinicios.

---

## Stack tecnológico

| Capa | Tecnología |
|------|-----------|
| Lenguaje | Java 17 |
| Framework principal | Spring Boot 3.x |
| Dependencias | Maven |
| Base de datos | PostgreSQL 16 |
| ORM | Spring Data JPA + Hibernate |
| API HTTP | WebClient (WebFlux) |
| Alertas en tiempo real | Spring WebSocket + STOMP + SockJS |
| Rate Limiting | Bucket4j |
| Métricas | Micrometer + Prometheus |
| Documentación API | SpringDoc OpenAPI (Swagger) |
| Resiliencia | Resilience4j (retry + circuit breaker) |
| Tests unitarios | JUnit 5 + Mockito |
| Tests de integración | MockMvc |
| Frontend | React 18 + Vite |
| Mapa | Leaflet + React-Leaflet |
| Cliente WebSocket | @stomp/stompjs + sockjs-client |
| Contenedores | Docker + Docker Compose |
| Monitorización | Grafana + Prometheus + Loki + Promtail + Dozzle |

---

## Estructura del proyecto

```
SkyFence/
├── backend/
│   ├── src/main/java/com/skyfence/
│   │   ├── config/
│   │   │   ├── FlightDataHealthIndicator.java
│   │   │   ├── OpenApiConfig.java
│   │   │   ├── RateLimitInterceptor.java
│   │   │   ├── WebMvcConfig.java
│   │   │   ├── WebSocketConfig.java
│   │   │   └── WebSocketHealthIndicator.java
│   │   ├── controller/
│   │   │   ├── AircraftController.java
│   │   │   ├── AlertController.java
│   │   │   ├── RootController.java
│   │   │   ├── SimulationController.java
│   │   │   └── ZoneController.java
│   │   ├── service/
│   │   │   ├── AircraftService.java
│   │   │   ├── AlertService.java
│   │   │   ├── FlightDataService.java
│   │   │   └── GeofenceService.java
│   │   ├── repository/
│   │   │   ├── AircraftRepository.java
│   │   │   ├── AlertRepository.java
│   │   │   └── RestrictedZoneRepository.java
│   │   ├── model/
│   │   │   ├── Aircraft.java
│   │   │   ├── Alert.java
│   │   │   └── RestrictedZone.java
│   │   └── SkyFenceApplication.java
│   └── src/test/java/com/skyfence/
│       ├── service/
│       │   ├── GeofenceServiceTest.java          (Mockito — 7 casos)
│       │   ├── AircraftServiceTest.java           (Mockito — 4 casos)
│       │   └── FlightDataResilienceTest.java      (Resilience4j — 4 casos)
│       └── controller/
│           ├── AircraftControllerTest.java        (MockMvc — 3 casos)
│           └── ZoneControllerTest.java            (MockMvc — 4 casos)
├── frontend/
│   └── src/
│       ├── views/
│       │   ├── Dashboard.jsx
│       │   ├── Alerts.jsx
│       │   ├── Login.jsx
│       │   ├── System.jsx
│       │   ├── Users.jsx
│       │   └── Zones.jsx
│       ├── components/
│       │   ├── AlertPanel.jsx
│       │   ├── ConfirmModal.jsx
│       │   ├── DroneMap.jsx
│       │   ├── Layout.jsx
│       │   └── ZoneManagerModal.jsx
│       ├── hooks/
│       │   └── useWebSocket.js
│       ├── App.jsx
│       └── main.jsx
├── monitoring/
│   ├── docker-compose.yml
│   ├── prometheus.yml
│   └── provisioning/
│       ├── dashboards/
│       │   └── json/skyfence.json
│       └── datasources/
├── docker-compose.yml
└── README.md
```

---

## Ejecución con Docker

Levanta toda la aplicación (PostgreSQL + backend + frontend) con un solo comando:

```bash
docker-compose up --build
```

| Servicio | URL |
|---------|-----|
| Backend API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Frontend | http://localhost:3000 |

```bash
# Parar conservando datos de PostgreSQL
docker-compose stop

# Parar y eliminar contenedores (datos persistidos en volumen)
docker-compose down
```

---

## Ejecución local (sin Docker)

**Requisitos:** Java 17, Maven, Node 20, PostgreSQL 16 en ejecución.

```bash
# Backend
cd backend
mvn spring-boot:run

# Frontend (en otra terminal)
cd frontend
npm install
npm run dev
```

---

## API REST

Documentación interactiva disponible en `http://localhost:8080/swagger-ui.html`.

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/api/aircraft` | Aeronaves persistidas en BD |
| `GET` | `/api/aircraft/live` | Aeronaves en tiempo real desde adsb.fi |
| `GET` | `/api/aircraft/flying` | Solo aeronaves en vuelo (no en tierra) |
| `GET` | `/api/zones` | Zonas restringidas configuradas |
| `POST` | `/api/zones` | Añadir nueva zona restringida |
| `DELETE` | `/api/zones/{id}` | Eliminar zona por ID |
| `GET` | `/api/alerts` | Histórico paginado de alertas (`?page`, `?size`, `?severity`, `?hours`) |
| `POST` | `/api/simulate` | Simular una intrusión en una zona (`?zoneId`) |

> **Nota Anti-DoS:** Todos los endpoints de la API clásica están protegidos por Rate Limiting. Superar el límite establecido devolverá un código HTTP `429 Too Many Requests`.

### WebSocket

- Endpoint STOMP: `ws://localhost:8080/ws`
- Topic de alertas: `/topic/alerts`

### Actuator (monitorización)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/actuator/health` | Estado global de la aplicación (agregado) |
| `GET` | `/actuator/health/adsbfi` | (Custom) Conectividad con la API de adsb.fi |
| `GET` | `/actuator/health/db` | Integridad de la conexión con PostgreSQL |
| `GET` | `/actuator/health/websocket` | (Custom) Estado del mensaje Broker STOMP y sesiones en vivo |
| `GET` | `/actuator/info` | Información de la aplicación |
| `GET` | `/actuator/metrics` | Métricas del sistema (JVM, HTTP, etc.) |
| `GET` | `/actuator/prometheus` | Métricas en formato Prometheus (scrapeadas por Grafana) |

> Se han implementado *Health Checks* personalizados para emitir diagnósticos en formato JSON puro. La arquitectura permite extenderlos o monitorizarlos directamente con Prometheus + Grafana.

---

## Lógica de geofencing

La detección usa la **fórmula de Haversine**, que mide la distancia geodésica entre dos puntos sobre la superficie terrestre:

```
a = sin²(Δlat/2) + cos(lat1) · cos(lat2) · sin²(Δlon/2)
distancia = R · 2 · atan2(√a, √(1−a))     (R = 6371 km)
```

Clasificación de severidad:
- `HIGH` — aeronave a menos del 50 % del radio de la zona
- `MEDIUM` — aeronave dentro del radio pero a más del 50 %

---

## Zonas restringidas por defecto

| Nombre | Tipo | Coordenadas | Radio |
|--------|------|-------------|-------|
| Aeropuerto Madrid-Barajas | AIRPORT | 40.4983, -3.5676 | 5 km |
| Aeropuerto El Prat Barcelona | AIRPORT | 41.2974, 2.0833 | 5 km |
| Aeropuerto Bilbao | AIRPORT | 43.3011, -2.9106 | 4 km |
| Aeropuerto Valencia | AIRPORT | 39.4893, -0.4816 | 4 km |
| Aeropuerto Sevilla | AIRPORT | 37.4180, -5.8931 | 4 km |
| Aeropuerto Málaga | AIRPORT | 36.6749, -4.4991 | 4 km |
| Base Aérea de Torrejón | MILITARY | 40.4967, -3.4456 | 4 km |
| Base Naval de Rota | MILITARY | 36.6412, -6.3496 | 5 km |
| Base Aérea de Morón | MILITARY | 37.1749, -5.6159 | 4 km |
| Base Aérea de Zaragoza | MILITARY | 41.6662, -1.0415 | 4 km |
| Central Nuclear Cofrentes | NUCLEAR | 39.2503, -1.0636 | 3 km |
| Central Nuclear Almaraz | NUCLEAR | 39.8070, -5.6980 | 3 km |
| Central Nuclear Ascó | NUCLEAR | 41.2003, 0.5681 | 3 km |
| Central Nuclear Vandellós | NUCLEAR | 40.9247, 0.8769 | 3 km |

---

## Tests

```bash
cd backend
mvn test
```

Cobertura incluida:

**GeofenceServiceTest** (Mockito):
- Aeronave dentro de zona genera alerta
- Aeronave fuera de zona no genera alerta
- Aeronave sin coordenadas devuelve lista vacía
- Aeronave muy cercana al centro → severidad `HIGH`
- Aeronave en varias zonas simultáneamente → múltiples alertas
- Distancia Madrid-Barcelona ≈ 505 km (validación Haversine)
- Sin zonas configuradas → sin alertas

**AircraftServiceTest** (Mockito):
- `getAllAircraft` devuelve todas las aeronaves
- `getAllAircraft` devuelve lista vacía si no hay datos
- `getAircraftInFlight` devuelve solo aeronaves en vuelo
- `getAircraftInFlight` devuelve vacío si todas en tierra

**AircraftControllerTest** (MockMvc):
- `GET /api/aircraft` devuelve 200 con datos
- `GET /api/aircraft/live` devuelve 200
- `GET /api/aircraft/flying` devuelve 200

**ZoneControllerTest** (MockMvc):
- `GET /api/zones` devuelve 200 con zonas
- `GET /api/zones` devuelve lista vacía si no hay zonas
- `POST /api/zones` crea y devuelve la zona guardada
- `DELETE /api/zones/{id}` elimina la zona por ID

**FlightDataResilienceTest** (Resilience4j):
- Retry ejecuta exactamente 3 intentos antes de rendirse
- Fallback retorna caché local cuando la API no responde
- Circuit breaker en estado OPEN no realiza nuevas llamadas
- Circuit breaker en estado HALF_OPEN permite llamada de sondeo

> Los tests usan un perfil `test` con H2 en memoria — no requieren PostgreSQL.

---

## Análisis de calidad con SonarCloud

El proyecto está integrado con [SonarCloud](https://sonarcloud.io/summary/new_code?id=laloba04_SkyFence) para análisis estático continuo del código. El escaneo se ejecuta automáticamente en cada push a `main` mediante GitHub Actions.

| Métrica | Qué mide |
|---------|----------|
| **Quality Gate** | Aprueba o bloquea el código según umbrales mínimos de calidad |
| **Bugs** | Errores lógicos detectados estáticamente que pueden causar fallos en producción |
| **Vulnerabilities** | Fallos de seguridad explotables (inyección, exposición de datos, etc.) |
| **Security Rating** | Nota de seguridad de A (sin vulnerabilidades) a E (crítico) |
| **Maintainability** | Deuda técnica acumulada: código duplicado, funciones complejas, etc. |
| **Code Smells** | Patrones de código problemáticos que dificultan el mantenimiento |
| **Coverage** | Porcentaje de líneas de código cubiertas por los tests |

El análisis cubre el módulo `backend/` (Java 17). La configuración del workflow se encuentra en `.github/workflows/sonar.yml`.

---

## Roadmap y futuras mejoras

El plan de mejoras vive en el [tablero Mejoras](https://github.com/users/laloba04/projects/3) del repositorio. El modelo de seguridad del proyecto (headers, auditoría de endpoints, perfiles, gestión de secretos) está documentado en [SECURITY.md](SECURITY.md).

---

## Stack de Observabilidad

SkyFence incluye un stack completo de monitorización y observabilidad para asegurar el rendimiento y la seguridad del sistema.

```bash
docker-compose -f monitoring/docker-compose.yml up
```

| Servicio | URL | Descripción |
|----------|-----|-------------|
| **Grafana** | http://localhost:3001 | Dashboards de métricas (JVM, CPU, HTTP) y Logs de Seguridad. |
| **Prometheus** | http://localhost:9090 | Almacenamiento de métricas temporales de Actuator. |
| **Loki** | http://localhost:3100 | Gestión centralizada de logs (formato JSON). |
| **Dozzle** | http://localhost:8081 | Visualización de logs de contenedores en tiempo real. |

### Dashboards Incluidos:
- **SpringBoot APM:** Visualización detallada de la salud de la JVM, uso de memoria, hilos y latencia de peticiones.
- **Security Alerts:** Panel basado en Loki para filtrar y visualizar alertas de intrusión (ej. Rate Limiting superado).
- **SkyFence Business:** Fila superior del dashboard con las métricas de negocio del dominio.

### Métricas de negocio personalizadas (Micrometer)

| Métrica | Tipo | Qué mide | Utilidad |
|---------|------|----------|----------|
| `skyfence_alerts_total{severity, zone_type}` | Counter | Alertas de intrusión generadas, etiquetadas por severidad (`HIGH`/`MEDIUM`) y tipo de zona (`AIRPORT`/`MILITARY`/`NUCLEAR`) | Detecciones/hora (`increase(...[1h])`), distribución por severidad y qué tipos de zona concentran más intrusiones |
| `skyfence_alert_publish_seconds` | Timer | Latencia de persistir una alerta y publicarla por WebSocket al frontend | Salud del pipeline de alertas en tiempo real; detectar degradación de BD o del broker STOMP |
| `skyfence_aircraft_tracked` | Gauge | Aeronaves rastreadas actualmente sobre España (caché de adsb.fi) | Cobertura del sistema; una caída a 0 delata problemas con la API externa |
| `http_server_requests_seconds_count{status=~"4..\|5.."}` | Counter (Spring) | Solicitudes fallidas por endpoint y código de estado | Endpoints problemáticos, abuso (429 del rate limiting) y errores 5xx |

### Mejores Prácticas de Logs:
- Los logs de seguridad se emiten con el prefijo `SECURITY ALERT:` y nivel `WARN` para facilitar el filtrado en Loki.
- Se utiliza el formato estructurado de SLF4J para asegurar que el contenido sea indexable.

### Correlation IDs (trazabilidad de peticiones)

Cada petición HTTP recibe un **correlation ID** único que viaja por todo el backend:

- **Header `X-Request-ID`**: si el cliente lo envía se respeta (saneado: solo alfanuméricos, máx. 64 chars); si no, se genera un UUID. Siempre se devuelve en la respuesta.
- **En los logs**: todos los logs de una petición comparten el mismo `requestId` — campo JSON en producción, entre corchetes en dev (`[uuid] [event] [userId] [ip]`).
- **Ciclos del scheduler**: cada barrido de aeronaves (adsb.fi → geofencing → alertas) usa un ID `sched-<uuid>`, de modo que todos los logs de un mismo ciclo se pueden agrupar.

**Cómo depurar un flujo:**
1. Reproduce el problema y copia el `X-Request-ID` de la respuesta (visible en las DevTools del navegador, pestaña Network).
2. En Grafana → Explore → Loki, filtra: `{container="skyfence-backend-1"} |= "<request-id>"` — aparecen todos los logs de esa petición en orden.
3. Sin Loki: `docker logs skyfence-backend-1 | grep "<request-id>"`.

### Observabilidad en producción (Render)

Además del stack local, Grafana y Prometheus están desplegados en Render (free tier) mediante el blueprint `render.yaml`:

- **Prometheus** (`monitoring/render/prometheus/`) scrapea el backend público (`skyfence-backend.onrender.com/actuator/prometheus`) cada 30 s.
- **Grafana** (`monitoring/render/grafana/`) sirve el dashboard *SkyFence Backend* con acceso anónimo de solo lectura (sin panel de Loki, que solo existe en local).
- El dashboard se embebe en la vista **Salud del Sistema** del frontend vía `VITE_GRAFANA_URL`.
- El workflow `.github/workflows/keep-alive.yml` hace ping cada 10 min para evitar que los servicios free se duerman y pierdan el histórico de métricas.

### Integración MQTT para sensores IoT

Además del polling a adsb.fi, el backend puede recibir posiciones de aeronaves publicadas por **sensores IoT vía MQTT** (Eclipse Paho). Cada mensaje pasa por el mismo pipeline que la API: upsert de la aeronave → geofencing → alerta en tiempo real.

**Configuración** (variables de entorno, todas con valores por defecto):

| Variable | Por defecto | Descripción |
|----------|-------------|-------------|
| `SKYFENCE_MQTT_ENABLED` | `false` | Activa la ingesta MQTT |
| `SKYFENCE_MQTT_BROKER_URL` | `tcp://localhost:1883` | URL del broker (`tcp://mosquitto:1883` en Docker) |
| `SKYFENCE_MQTT_TOPIC` | `skyfence/sensors/aircraft` | Topic de suscripción |
| `SKYFENCE_MQTT_USERNAME` / `_PASSWORD` | *(vacío)* | Credenciales si el broker las exige |
| `SKYFENCE_MQTT_QOS` | `1` | Calidad de servicio de la suscripción |
| `FLIGHTDATA_API_ENABLED` | `true` | Con `false` apaga el polling de adsb.fi (modo solo-MQTT) |

Las dos fuentes pueden convivir (API + sensores) o usarse por separado. La ingesta expone la métrica `skyfence_mqtt_messages_total{result="processed"|"invalid"}` y cada mensaje lleva su correlation ID `mqtt-<uuid>` en los logs.

**Formato de mensaje esperado** (JSON; los campos extra se ignoran):

```json
{
  "icao24": "342266",        // obligatorio, alfanumérico (si es hex ICAO se deduce el país)
  "callsign": "DRON01",      // opcional, por defecto "SENSOR"
  "latitude": 40.4983,       // obligatorio, [-90, 90]
  "longitude": -3.5676,      // obligatorio, [-180, 180]
  "altitude": 120.5,         // opcional, metros
  "velocity": 12.3,          // opcional, m/s
  "onGround": false          // opcional, por defecto false
}
```

Los mensajes con JSON inválido, `icao24` ausente/malicioso o coordenadas fuera de rango se descartan (contador `result="invalid"`) sin afectar al servicio.

**Prueba local** con el broker Mosquitto incluido:

```bash
# 1. Arrancar el broker (perfil opcional del compose)
docker compose --profile mqtt up -d mosquitto

# 2. Arrancar el backend con MQTT activado
SKYFENCE_MQTT_ENABLED=true docker compose up -d backend

# 3. Publicar la posición de un sensor (dron dentro de la zona de Barajas)
docker exec skyfence-mosquitto mosquitto_pub -t skyfence/sensors/aircraft \
  -m '{"icao24":"342266","callsign":"DRON01","latitude":40.4983,"longitude":-3.5676,"altitude":120.5}'
```

En segundos aparece la alerta `HIGH — Aeropuerto Madrid-Barajas` en el dashboard y por WebSocket.

> ⚠️ El `mosquitto.conf` incluido permite conexiones anónimas: es solo para desarrollo. En producción usa un broker gestionado con autenticación y TLS (p. ej. HiveMQ Cloud free tier) y define las credenciales por variables de entorno.

### Alertas automáticas ante caídas y errores críticos

El sistema de alertas funciona sin Alertmanager (el free tier de Render no admite otro servicio) combinando dos piezas:

**1. Reglas de alerta en Prometheus** (`monitoring/alerts.yml`, desplegadas también en Render): Prometheus las evalúa continuamente y expone su estado en [`/alerts`](https://skyfence-prometheus.onrender.com/alerts) y vía la API (`/api/v1/alerts`).

| Alerta | Severidad | Se dispara cuando... |
|--------|-----------|----------------------|
| `BackendDown` | critical | El backend lleva 3 min sin responder al scrape |
| `FlightApiDown` | critical | El circuit breaker de adsb.fi lleva 2 min abierto |
| `FlightApiSlow` | warning | >50% de llamadas lentas a adsb.fi durante 10 min |
| `NoAircraftTracked` | warning | 0 aeronaves rastreadas durante 15 min |
| `HighHttpErrorRate` | critical | >5% de respuestas 5xx durante 5 min |
| `DatabaseConnectionTimeouts` | critical | Timeouts de HikariCP al pedir conexiones de BD |
| `DatabasePoolSaturated` | warning | >90% del pool de conexiones en uso durante 5 min |
| `HighErrorLogRate` | warning | >20 logs `ERROR` en 10 min |
| `SlowAlertPublish` | warning | Publicar una alerta tarda >500 ms de media (normal: ~2 ms) |
| `HighJvmHeapAfterGc` | warning | Heap >90% tras GC durante 10 min (riesgo de OOM en 512 MB) |

Los umbrales están parametrizados en el propio YAML (`expr` y `for`) para ajustarlos según madure el proyecto.

**2. Notificaciones vía GitHub Actions** (`.github/workflows/health-alert.yml`): cada 15 min comprueba backend, frontend, Grafana y Prometheus, y consulta las alertas *firing*. Si detecta una caída o una alerta **critical**:

- Abre un issue con la etiqueta `outage` (GitHub envía correo automáticamente) con el detalle y enlaces a Prometheus/Grafana/Render.
- **Anti-spam**: nunca hay más de un issue de caída abierto; las alertas *warning* solo quedan en el log del workflow.
- Cuando todo se recupera, el issue se cierra solo con un comentario de resolución.
