-- One client per (tenant, email), case-insensitive. Prevents duplicates created by concurrent bookings.
-- NOTE: fails if duplicates already exist in clients; merge them (and re-point appointments) before applying.
CREATE UNIQUE INDEX uq_clients_tenant_email ON clients (tenant_id, lower(email));
