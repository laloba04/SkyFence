-- AIRPORTS
INSERT INTO restricted_zone (name, type, latitude, longitude, radius_km)
SELECT 'Aeropuerto Madrid-Barajas', 'AIRPORT', 40.4983, -3.5676, 5.0
WHERE NOT EXISTS (SELECT 1 FROM restricted_zone WHERE name = 'Aeropuerto Madrid-Barajas');

INSERT INTO restricted_zone (name, type, latitude, longitude, radius_km)
SELECT 'Aeropuerto El Prat Barcelona', 'AIRPORT', 41.2974, 2.0833, 5.0
WHERE NOT EXISTS (SELECT 1 FROM restricted_zone WHERE name = 'Aeropuerto El Prat Barcelona');

INSERT INTO restricted_zone (name, type, latitude, longitude, radius_km)
SELECT 'Aeropuerto Bilbao', 'AIRPORT', 43.3011, -2.9106, 4.0
WHERE NOT EXISTS (SELECT 1 FROM restricted_zone WHERE name = 'Aeropuerto Bilbao');

INSERT INTO restricted_zone (name, type, latitude, longitude, radius_km)
SELECT 'Aeropuerto Valencia', 'AIRPORT', 39.4893, -0.4816, 4.0
WHERE NOT EXISTS (SELECT 1 FROM restricted_zone WHERE name = 'Aeropuerto Valencia');

INSERT INTO restricted_zone (name, type, latitude, longitude, radius_km)
SELECT 'Aeropuerto Sevilla', 'AIRPORT', 37.4180, -5.8931, 4.0
WHERE NOT EXISTS (SELECT 1 FROM restricted_zone WHERE name = 'Aeropuerto Sevilla');

INSERT INTO restricted_zone (name, type, latitude, longitude, radius_km)
SELECT 'Aeropuerto Málaga', 'AIRPORT', 36.6749, -4.4991, 4.0
WHERE NOT EXISTS (SELECT 1 FROM restricted_zone WHERE name = 'Aeropuerto Málaga');

-- MILITARY
INSERT INTO restricted_zone (name, type, latitude, longitude, radius_km)
SELECT 'Base Aérea de Torrejón', 'MILITARY', 40.4967, -3.4456, 4.0
WHERE NOT EXISTS (SELECT 1 FROM restricted_zone WHERE name = 'Base Aérea de Torrejón');

INSERT INTO restricted_zone (name, type, latitude, longitude, radius_km)
SELECT 'Base Naval de Rota', 'MILITARY', 36.6412, -6.3496, 5.0
WHERE NOT EXISTS (SELECT 1 FROM restricted_zone WHERE name = 'Base Naval de Rota');

INSERT INTO restricted_zone (name, type, latitude, longitude, radius_km)
SELECT 'Base Aérea de Morón', 'MILITARY', 37.1749, -5.6159, 4.0
WHERE NOT EXISTS (SELECT 1 FROM restricted_zone WHERE name = 'Base Aérea de Morón');

INSERT INTO restricted_zone (name, type, latitude, longitude, radius_km)
SELECT 'Base Aérea de Zaragoza', 'MILITARY', 41.6662, -1.0415, 4.0
WHERE NOT EXISTS (SELECT 1 FROM restricted_zone WHERE name = 'Base Aérea de Zaragoza');

-- NUCLEAR
INSERT INTO restricted_zone (name, type, latitude, longitude, radius_km)
SELECT 'Central Nuclear Cofrentes', 'NUCLEAR', 39.2503, -1.0636, 3.0
WHERE NOT EXISTS (SELECT 1 FROM restricted_zone WHERE name = 'Central Nuclear Cofrentes');

INSERT INTO restricted_zone (name, type, latitude, longitude, radius_km)
SELECT 'Central Nuclear Almaraz', 'NUCLEAR', 39.8070, -5.6980, 3.0
WHERE NOT EXISTS (SELECT 1 FROM restricted_zone WHERE name = 'Central Nuclear Almaraz');

INSERT INTO restricted_zone (name, type, latitude, longitude, radius_km)
SELECT 'Central Nuclear Ascó', 'NUCLEAR', 41.2003, 0.5681, 3.0
WHERE NOT EXISTS (SELECT 1 FROM restricted_zone WHERE name = 'Central Nuclear Ascó');

INSERT INTO restricted_zone (name, type, latitude, longitude, radius_km)
SELECT 'Central Nuclear Vandellós', 'NUCLEAR', 40.9247, 0.8769, 3.0
WHERE NOT EXISTS (SELECT 1 FROM restricted_zone WHERE name = 'Central Nuclear Vandellós');
