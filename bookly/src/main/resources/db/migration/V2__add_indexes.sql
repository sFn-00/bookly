-- ============================================================
-- PERFORMANCE INDEXES
-- ============================================================

-- Appointments: tenant-scoped date range queries (admin panel, slot calculation)
CREATE INDEX idx_appointments_tenant_date   ON appointments(tenant_id, start_time);

-- Appointments: staff availability check during slot calculation
CREATE INDEX idx_appointments_staff_time    ON appointments(staff_id, start_time, end_time);

-- Appointments: tenant + status filtering (admin panel)
CREATE INDEX idx_appointments_tenant_status ON appointments(tenant_id, status);

-- Notifications: scheduler polls this every minute — partial index on PENDING only
CREATE INDEX idx_notifications_pending      ON notifications(scheduled_at) WHERE status = 'PENDING';

-- Users: tenant-scoped lookup
CREATE INDEX idx_users_tenant               ON users(tenant_id);

-- Services: active services per tenant
CREATE INDEX idx_services_tenant_active     ON services(tenant_id, active);

-- Staff: active staff per tenant
CREATE INDEX idx_staff_tenant_active        ON staff(tenant_id, active);

-- Availability: staff weekly schedule lookup
CREATE INDEX idx_availability_staff_day     ON availability(staff_id, day_of_week);

-- Clients: tenant-scoped client lookup
CREATE INDEX idx_clients_tenant             ON clients(tenant_id);

-- Subscriptions: active subscription per tenant
CREATE INDEX idx_subscriptions_tenant       ON subscriptions(tenant_id);
