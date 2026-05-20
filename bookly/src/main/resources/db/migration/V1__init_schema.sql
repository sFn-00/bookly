-- ============================================================
-- TENANTS
-- ============================================================

CREATE TABLE tenants (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                    VARCHAR(255) NOT NULL,
    subdomain               VARCHAR(100) NOT NULL,
    plan                    VARCHAR(20)  NOT NULL DEFAULT 'FREE'
                                CHECK (plan IN ('FREE', 'PRO', 'ENTERPRISE')),
    stripe_customer_id      VARCHAR(255),
    stripe_subscription_id  VARCHAR(255),
    active                  BOOLEAN NOT NULL DEFAULT true,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_tenants_subdomain UNIQUE (subdomain)
);

-- ============================================================
-- USERS  (OWNER / STAFF accounts — not end clients)
-- ============================================================

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(10)  NOT NULL CHECK (role IN ('OWNER', 'STAFF')),
    active          BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_users_email UNIQUE (email)
);

-- ============================================================
-- SERVICES
-- ============================================================

CREATE TABLE services (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id),
    name                VARCHAR(255) NOT NULL,
    description         TEXT,
    duration_minutes    INT NOT NULL CHECK (duration_minutes > 0),
    price               DECIMAL(10, 2) NOT NULL CHECK (price >= 0),
    active              BOOLEAN NOT NULL DEFAULT true,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- STAFF
-- ============================================================

CREATE TABLE staff (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL REFERENCES tenants(id),
    user_id     UUID REFERENCES users(id),
    first_name  VARCHAR(100) NOT NULL,
    last_name   VARCHAR(100) NOT NULL,
    email       VARCHAR(255) NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT true,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- AVAILABILITY  (weekly schedule per staff member)
-- day_of_week: 1=MONDAY ... 7=SUNDAY  (matches Java DayOfWeek)
-- ============================================================

CREATE TABLE availability (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL REFERENCES tenants(id),
    staff_id    UUID NOT NULL REFERENCES staff(id),
    day_of_week INT NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    start_time  TIME NOT NULL,
    end_time    TIME NOT NULL,

    CONSTRAINT chk_availability_times CHECK (start_time < end_time)
);

-- ============================================================
-- CLIENTS  (end-customers who book appointments, no login)
-- ============================================================

CREATE TABLE clients (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL REFERENCES tenants(id),
    first_name  VARCHAR(100) NOT NULL,
    last_name   VARCHAR(100) NOT NULL,
    email       VARCHAR(255) NOT NULL,
    phone       VARCHAR(20),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- APPOINTMENTS
-- version column supports @Version optimistic locking (Req 5.5)
-- ============================================================

CREATE TABLE appointments (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL REFERENCES tenants(id),
    client_id   UUID REFERENCES clients(id),
    staff_id    UUID NOT NULL REFERENCES staff(id),
    service_id  UUID NOT NULL REFERENCES services(id),
    start_time  TIMESTAMP NOT NULL,
    end_time    TIMESTAMP NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                    CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED')),
    notes       TEXT,
    version     BIGINT NOT NULL DEFAULT 0,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_appointment_times CHECK (start_time < end_time)
);

-- ============================================================
-- NOTIFICATIONS
-- ============================================================

CREATE TABLE notifications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id  UUID NOT NULL REFERENCES appointments(id),
    type            VARCHAR(10)  NOT NULL CHECK (type IN ('EMAIL', 'SMS')),
    scheduled_at    TIMESTAMP NOT NULL,
    sent_at         TIMESTAMP,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'CANCELLED'))
);

-- ============================================================
-- SUBSCRIPTIONS
-- ============================================================

CREATE TABLE subscriptions (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL REFERENCES tenants(id),
    stripe_subscription_id  VARCHAR(255),
    stripe_customer_id      VARCHAR(255),
    plan                    VARCHAR(20) NOT NULL CHECK (plan IN ('FREE', 'PRO', 'ENTERPRISE')),
    status                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                                CHECK (status IN ('ACTIVE', 'CANCELLED', 'PAST_DUE')),
    current_period_start    TIMESTAMP,
    current_period_end      TIMESTAMP,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW()
);
