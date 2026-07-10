# Seguridad de SkyFence

Modelo de seguridad del proyecto, decisiones tomadas y cómo actuar ante un problema.

## Reportar una vulnerabilidad

Abre un issue en GitHub con la etiqueta `security` (o contacta con la propietaria del repositorio si el detalle no debe ser público). Se agradece un reporte con pasos de reproducción e impacto estimado.

## Autenticación y autorización

- **JWT stateless**: la API no usa sesiones ni cookies. El token (HS256, expiración 24 h) viaja en el header `Authorization: Bearer` y el frontend lo guarda en `localStorage`.
- **Roles**: `ADMIN` y `OPERATOR`, aplicados con `@EnableMethodSecurity` y reglas por endpoint.
- **Contraseñas**: BCrypt.
- **CSRF desactivado a propósito**: al no haber cookies de autenticación, una petición cross-site forjada llega sin credencial y recibe 401. Justificado en `SecurityConfig` (regla CodeQL `java/spring-disabled-csrf-protection`).

## Headers HTTP de seguridad

**Backend** (Spring Security, `SecurityConfig`):

| Header | Valor |
|--------|-------|
| `Content-Security-Policy` | `default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; frame-ancestors 'none'; base-uri 'self'; form-action 'self'` (compatible con Swagger UI) |
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` (requiere `server.forward-headers-strategy=framework` en prod, porque Render termina TLS en su proxy) |
| `X-Content-Type-Options` | `nosniff` (por defecto de Spring Security) |
| `X-Frame-Options` | `DENY` (por defecto) |
| `Referrer-Policy` | `strict-origin-when-cross-origin` |
| `Permissions-Policy` | `camera=(), microphone=(), geolocation=(), payment=()` |

**Frontend** (Vercel, `frontend/vercel.json`): mismos headers adaptados al SPA. El CSP permite exactamente los orígenes que la app necesita: tiles de OpenStreetMap (imágenes), el backend de Render (fetch + WebSocket) y el Grafana embebido (iframe).

## Auditoría de endpoints expuestos

| Endpoint | Acceso | Justificación |
|----------|--------|---------------|
| `POST /api/auth/**` | Público | Login/registro; protegido por rate limiting |
| `GET /api/**` | Público | Datos de lectura (aeronaves, zonas, alertas) — la app es de consulta pública |
| Escrituras `/api/**` | JWT + rol | Crear/editar/borrar zonas requiere autenticación |
| `POST /api/stripe/webhook` | Público | La firma del webhook se verifica dentro del handler |
| `/ws/**` | Público | WebSocket de alertas en tiempo real (solo emite datos ya públicos por GET) |
| `/swagger-ui/**`, `/api-docs/**` | Público | Documentación de la API; decisión consciente al ser un proyecto demostrativo |
| `/actuator/*` | Público, **limitado a** `health`, `info`, `metrics`, `prometheus` | `health` lo consume la vista Salud del Sistema y los health checks; `prometheus` lo scrapea el Prometheus de Render. El resto de endpoints de Actuator (env, beans, mappings…) **no está expuesto** |

Riesgo aceptado y documentado: `health` con `show-details=always` revela el estado de componentes (BD, circuit breaker) porque la vista pública de salud del sistema lo muestra; no expone credenciales ni direcciones internas.

## Protecciones en runtime

- **Rate limiting** (Bucket4j) por IP en lectura y escritura; los excesos devuelven 429 y se registran como `SECURITY ALERT`.
- **Validación de entrada** en la ingesta MQTT (icao24 alfanumérico, coordenadas en rango, callsign saneado) y sanitización del header `X-Request-ID` contra log injection.
- **Resiliencia**: retry + circuit breaker en la API externa; timeouts en todas las llamadas salientes.
- **Logs**: estructurados en JSON con correlation IDs; los eventos de seguridad llevan el prefijo `SECURITY ALERT:`.

## Perfiles dev/prod

| Aspecto | dev | prod |
|---------|-----|------|
| Logs | Consola legible, nivel DEBUG | JSON (Logstash encoder), nivel WARN/INFO |
| Esquema BD | `ddl-auto=update` | `ddl-auto=validate` |
| CORS | Solo `localhost:3000/5173` | Solo `https://sky-fence.vercel.app` |
| HSTS | No aplica (http) | Activo tras el proxy TLS de Render |

## Gestión de secretos

- Nunca subir credenciales reales a git.
- En local, usar `.env` (ya en `.gitignore`); en `.env.example` solo placeholders.
- En despliegues, usar los gestores de secretos de Render/Vercel y GitHub Secrets (BD, JWT, Stripe, Sonar).
- `JWT_SECRET` tiene un valor de desarrollo por defecto que **debe** sobreescribirse en producción.

### Si se expone un secreto

1. Revocar y rotar la credencial inmediatamente.
2. Reescribir el historial de git para eliminar los commits comprometidos.
3. Hacer force-push de la rama saneada y reabrir/re-escanear el PR.
4. Documentar el incidente y su remediación.

## Escaneo automático (CI/CD)

Cada push y PR a `main` dispara:

| Herramienta | Workflow | Qué comprueba |
|-------------|----------|---------------|
| **Gitleaks** | `secret-scan.yml` | Secretos y credenciales subidos por accidente |
| **CodeQL** | `codeql.yml` | SAST — vulnerabilidades Java y riesgos de inyección |
| **SonarCloud** | `sonar.yml` | Calidad, security hotspots y cobertura (omitido en PRs de Dependabot, que no reciben secrets) |
| **npm audit** | `ci.yml` | Vulnerabilidades de dependencias del frontend (SCA) |
| **Dependabot** | `dependabot.yml` | Actualizaciones semanales de Maven, npm y GitHub Actions |

### Escaneo local de secretos

El repositorio incluye hooks de pre-commit para `gitleaks` y `detect-secrets`:

```bash
pip install pre-commit
pre-commit install
pre-commit run --all-files
```

## Mejoras previstas

Ver issues abiertos: refresh tokens con expiración corta (#46), verificación de email y recuperación de contraseña (#47), backups de BD (#39).
