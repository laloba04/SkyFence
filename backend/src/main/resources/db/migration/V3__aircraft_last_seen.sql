-- Marca temporal de última observación de cada aeronave: necesaria para la
-- política de retención (purga de aeronaves que llevan días sin verse).
ALTER TABLE aircraft ADD COLUMN last_seen TIMESTAMP(6);
UPDATE aircraft SET last_seen = now() WHERE last_seen IS NULL;
