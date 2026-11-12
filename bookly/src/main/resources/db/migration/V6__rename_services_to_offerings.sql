-- Rename the "services" domain concept to "offerings" (avoids clash with Spring's @Service).
ALTER TABLE services RENAME TO offerings;
ALTER TABLE appointments RENAME COLUMN service_id TO offering_id;
ALTER INDEX idx_services_tenant_active RENAME TO idx_offerings_tenant_active;
