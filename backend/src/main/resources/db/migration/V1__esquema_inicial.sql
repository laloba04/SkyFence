-- Esquema inicial de SkyFence (baseline).
-- Refleja el esquema que Hibernate venía generando con ddl-auto=update; a
-- partir de ahora el esquema lo gobierna Flyway y Hibernate solo valida.
-- Las BD existentes se baselinen en la versión 1 (baseline-on-migrate), así
-- que esta migración solo se ejecuta en bases de datos vacías.

CREATE TABLE aircraft (
    id             BIGSERIAL PRIMARY KEY,
    altitude       DOUBLE PRECISION,
    callsign       VARCHAR(255),
    icao24         VARCHAR(255),
    latitude       DOUBLE PRECISION,
    longitude      DOUBLE PRECISION,
    on_ground      BOOLEAN,
    origin_country VARCHAR(255),
    velocity       DOUBLE PRECISION
);

CREATE TABLE alerts (
    id                BIGSERIAL PRIMARY KEY,
    aircraft_callsign VARCHAR(255),
    aircraft_icao     VARCHAR(255),
    detected_at       TIMESTAMP(6) NOT NULL,
    distance_km       DOUBLE PRECISION,
    severity          VARCHAR(255),
    zone_name         VARCHAR(255),
    zone_type         VARCHAR(255)
);

CREATE TABLE restricted_zone (
    id        BIGSERIAL PRIMARY KEY,
    latitude  DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    name      VARCHAR(255),
    radius_km DOUBLE PRECISION,
    type      VARCHAR(255)
);

CREATE TABLE users (
    id                  BIGSERIAL PRIMARY KEY,
    username            VARCHAR(255) NOT NULL,
    password            VARCHAR(255) NOT NULL,
    role                VARCHAR(255) NOT NULL,
    email               VARCHAR(255),
    stripe_customer_id  VARCHAR(255),
    subscription_status VARCHAR(255) NOT NULL DEFAULT 'FREE',
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT users_role_check CHECK (role IN ('ADMIN', 'OPERATOR'))
);
