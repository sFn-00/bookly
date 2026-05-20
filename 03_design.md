# Design Document — Bookly

## Przegląd

Dokument opisuje szczegółową specyfikację każdego komponentu zdefiniowanego w blueprint.md. Każdy komponent zawiera cel, lokalizację w projekcie, interfejs publiczny i odwołania do wymagań.

---

## Zasady projektowania

1. **Tenant-first**: Każda operacja bazodanowa musi przejść przez TenantContext — brak tenant_id = wyjątek
2. **Fail-safe**: Brak tenanta w kontekście = odrzucenie żądania (HTTP 404), nie domyślny tenant
3. **Soft delete**: Encje nie są fizycznie usuwane — używamy flagi `active = false`
4. **Optimistic Locking**: Tabela `appointments` używa `@Version` dla kontroli współbieżności
5. **Plan enforcement**: PlanEnforcer jest wywoływany jako pierwsza operacja przed mutacją danych

---

## Specyfikacje komponentów

---

### Komponent: NginxGateway

**Cel**: Odbieranie ruchu HTTP/S i routing subdomenowy do Spring Boot API

**Lokalizacja**: `nginx/nginx.conf`

**Konfiguracja**:
```nginx
server {
    listen 80;
    server_name ~^(?<subdomain>.+)\.bookly\.pl$;

    location / {
        proxy_pass http://spring-boot-api:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

**Odwołania do wymagań**: 2.1

---

### Komponent: TenantResolver

**Cel**: Wyodrębnienie tenant_id z subdomeny i ustawienie TenantContext

**Lokalizacja**: `bookly/config/TenantFilter.java`, `bookly/config/TenantContext.java`

**Interfejs**:
```java
// TenantContext.java
public class TenantContext {
    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();
    public static void setTenant(String tenantId) { /* Implements Req 2.1 */ }
    public static String getTenant() { /* Implements Req 2.1, 2.4 */ }
    public static void clear() { /* Wywoływane w finally bloku */ }
}

// TenantFilter.java
@Component
@Order(1)
public class TenantFilter implements Filter {
    // Implements Req 2.1, 2.2
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain);
    private String extractSubdomain(String serverName); // hairsalon.bookly.pl → "hairsalon"
    private String resolveTenantId(String subdomain);   // subdomain → UUID z bazy
}
```

**Odwołania do wymagań**: 2.1, 2.2

---

### Komponent: AuthModule

**Cel**: Rejestracja tenantów, logowanie, wydawanie i odświeżanie JWT

**Lokalizacja**: `bookly/api/auth/AuthController.java`, `bookly/domain/user/AuthService.java`, `bookly/config/SecurityConfig.java`

**Interfejs**:
```java
// AuthController.java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    POST /register  → RegisterRequest  → TokenPair (HTTP 201)  // Implements Req 1.1, 1.2, 1.3, 1.4, 1.5
    POST /login     → LoginRequest     → TokenPair (HTTP 200)  // Implements Req 2.3
    POST /refresh   → RefreshRequest   → TokenPair (HTTP 200)  // Implements Req 2.5
}

// JwtService.java
public class JwtService {
    public String generateAccessToken(User user, String tenantId);  // Implements Req 2.3
    public String generateRefreshToken(User user);
    public Claims validateToken(String token);                       // Implements Req 2.4
}

// RegisterRequest DTO
public record RegisterRequest(
    String companyName,  // → tenants.name
    String subdomain,    // → tenants.subdomain (unique)
    String email,        // → users.email (unique)
    String password      // → bcrypt hash
) {}
```

**Odwołania do wymagań**: 1.1, 1.2, 1.3, 1.4, 1.5, 2.3, 2.4, 2.5, 2.6

---

### Komponent: TenantModule

**Cel**: Operacje CRUD na rekordach tenantów

**Lokalizacja**: `bookly/domain/tenant/Tenant.java`, `bookly/domain/tenant/TenantRepository.java`, `bookly/domain/tenant/TenantService.java`

**Interfejs**:
```java
// Tenant.java (JPA Entity)
@Entity @Table(name = "tenants")
public class Tenant {
    @Id UUID id;
    String name;
    @Column(unique = true) String subdomain;
    @Enumerated Plan plan;         // FREE / PRO / ENTERPRISE
    String stripeCustomerId;
    String stripeSubscriptionId;
    boolean active;
    LocalDateTime createdAt;
}

// TenantService.java
public class TenantService {
    public Tenant createTenant(String name, String subdomain);  // Implements Req 1.2
    public Tenant findBySubdomain(String subdomain);             // Implements Req 2.2
    public void updatePlan(UUID tenantId, Plan plan);            // Implements Req 8.2, 8.3
}
```

**Odwołania do wymagań**: 1.2, 2.2, 8.2, 8.3

---

### Komponent: ServiceModule

**Cel**: CRUD usług oferowanych przez tenanta

**Lokalizacja**: `bookly/domain/service/`, `bookly/api/admin/ServiceAdminController.java`

**Interfejs**:
```java
// ServiceAdminController.java
@RestController @RequestMapping("/api/admin/services")
@PreAuthorize("hasRole('OWNER') or hasRole('STAFF')")
public class ServiceAdminController {
    GET    /              → List<ServiceDTO>  // Implements Req 3.4
    POST   /              → ServiceDTO        // Implements Req 3.1
    PUT    /{id}          → ServiceDTO        // Implements Req 3.2
    DELETE /{id}          → HTTP 204          // Implements Req 3.3, 3.5
}

// ServiceService.java
public class ServiceService {
    public List<Service> findAllForCurrentTenant();          // tenant_id from TenantContext
    public Service create(CreateServiceRequest req);
    public Service update(UUID id, UpdateServiceRequest req);
    public void softDelete(UUID id);                         // Implements Req 3.3
    private void validateNoUpcomingAppointments(UUID id);    // Implements Req 3.5
}
```

**Odwołania do wymagań**: 3.1, 3.2, 3.3, 3.4, 3.5

---

### Komponent: StaffModule

**Cel**: Zarządzanie pracownikami i harmonogramem dostępności

**Lokalizacja**: `bookly/domain/staff/`, `bookly/domain/availability/`, `bookly/api/admin/StaffAdminController.java`, `bookly/api/admin/AvailabilityAdminController.java`

**Interfejs**:
```java
// StaffAdminController.java
@RestController @RequestMapping("/api/admin/staff")
@PreAuthorize("hasRole('OWNER')")
public class StaffAdminController {
    GET  /         → List<StaffDTO>   // Implements Req 4.5
    POST /         → StaffDTO         // Implements Req 4.1, 4.2
}

// AvailabilityAdminController.java
@RestController @RequestMapping("/api/admin/availability")
public class AvailabilityAdminController {
    GET  /         → List<AvailabilityDTO>  // Lista harmonogramów
    POST /         → AvailabilityDTO         // Implements Req 4.3, 4.4
    PUT  /{id}     → AvailabilityDTO         // Edycja harmonogramu
}

// StaffService.java
public class StaffService {
    public Staff create(CreateStaffRequest req);              // wywołuje PlanEnforcer.checkStaffLimit()
    public List<Availability> getAvailability(UUID staffId);
    public void validateNoOverlap(UUID staffId, DayOfWeek day, LocalTime start, LocalTime end); // Req 4.4
}
```

**Odwołania do wymagań**: 4.1, 4.2, 4.3, 4.4, 4.5

---

### Komponent: BookingEngine

**Cel**: Obliczanie wolnych slotów i tworzenie/anulowanie rezerwacji przez klientów końcowych

**Lokalizacja**: `bookly/api/booking/BookingController.java`, `bookly/domain/appointment/BookingService.java`

**Interfejs**:
```java
// BookingController.java — PUBLICZNY (bez autoryzacji JWT)
@RestController @RequestMapping("/api/booking")
public class BookingController {
    GET  /services                        → List<ServiceDTO>     // Lista usług tenanta
    GET  /slots?serviceId&staffId&date    → List<SlotDTO>        // Implements Req 5.1
    POST /appointments                    → AppointmentDTO        // Implements Req 5.2, 5.3, 5.4, 5.5
    PUT  /appointments/{id}/cancel        → HTTP 204              // Implements Req 5.6
}

// BookingService.java
public class BookingService {
    public List<Slot> calculateAvailableSlots(UUID serviceId, UUID staffId, LocalDate date); // Req 5.1
    public Appointment book(BookingRequest req);   // Req 5.2-5.5, wywołuje PlanEnforcer
    public void cancel(UUID appointmentId);         // Req 5.6
}

// Appointment.java (JPA Entity) — Optimistic Locking
@Entity @Table(name = "appointments")
public class Appointment {
    @Id UUID id;
    @Column(nullable = false) UUID tenantId;
    UUID clientId; UUID staffId; UUID serviceId;
    LocalDateTime startTime; LocalDateTime endTime;
    @Enumerated AppointmentStatus status;   // PENDING / CONFIRMED / CANCELLED
    String notes;
    @Version Long version;                  // Implements Req 5.5 — optimistic locking
    LocalDateTime createdAt;
}
```

**Odwołania do wymagań**: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6

---

### Komponent: AppointmentModule

**Cel**: Panel admina — zarządzanie rezerwacjami przez OWNER/STAFF

**Lokalizacja**: `bookly/api/admin/AppointmentAdminController.java`, `bookly/domain/appointment/AppointmentService.java`

**Interfejs**:
```java
// AppointmentAdminController.java
@RestController @RequestMapping("/api/admin/appointments")
@PreAuthorize("hasRole('OWNER') or hasRole('STAFF')")
public class AppointmentAdminController {
    GET /                        → Page<AppointmentDTO>  // Implements Req 6.1 (filtr date/status)
    PUT /{id}/confirm            → AppointmentDTO         // Implements Req 6.2, 6.4
    PUT /{id}/cancel             → HTTP 204               // Implements Req 6.3
}

// AppointmentService.java
public class AppointmentService {
    public Page<Appointment> findAll(AppointmentFilter filter);
    public Appointment confirm(UUID id);   // Req 6.2: status PENDING→CONFIRMED + tworzy Notifications
    public void cancel(UUID id);           // Req 6.3: status→CANCELLED + anuluje Notifications
}
```

**Odwołania do wymagań**: 6.1, 6.2, 6.3, 6.4

---

### Komponent: NotificationScheduler

**Cel**: Planowanie i wysyłanie przypomnień email i SMS

**Lokalizacja**: `bookly/domain/notification/NotificationScheduler.java`, `bookly/infrastructure/email/EmailService.java`, `bookly/infrastructure/sms/SmsService.java`

**Interfejs**:
```java
// NotificationScheduler.java
@Component
public class NotificationScheduler {
    @Scheduled(fixedDelay = 60_000)   // co minutę
    public void processNotifications();   // Implements Req 7.3, 7.4, 7.5
}

// EmailService.java
public class EmailService {
    public void sendReminder(Notification notification);  // JavaMailSender + Gmail SMTP, Req 7.4
}

// SmsService.java
public class SmsService {
    // twilio-java SDK, Req 7.6
    public void sendReminder(Notification notification);
}

// Notification.java (JPA Entity)
@Entity @Table(name = "notifications")
public class Notification {
    @Id UUID id;
    UUID appointmentId;
    @Enumerated NotificationType type;    // EMAIL / SMS
    LocalDateTime scheduledAt;
    LocalDateTime sentAt;
    @Enumerated NotificationStatus status; // PENDING / SENT / FAILED
}

// AppointmentService.createNotifications() — wywoływane po confirm()
// Implements Req 7.1: tworzy EMAIL notification scheduled_at = startTime - 24h
// Implements Req 7.2: jeśli plan PRO/ENTERPRISE, tworzy SMS notification scheduled_at = startTime - 2h
```

**Odwołania do wymagań**: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6

---

### Komponent: SubscriptionModule

**Cel**: Integracja ze Stripe — subskrypcje planów i obsługa webhooków

**Lokalizacja**: `bookly/api/subscription/SubscriptionController.java`, `bookly/infrastructure/stripe/StripeService.java`, `bookly/api/stripe/StripeWebhookController.java`

**Interfejs**:
```java
// SubscriptionController.java
@RestController @RequestMapping("/api/subscription")
@PreAuthorize("hasRole('OWNER')")
public class SubscriptionController {
    GET  /           → SubscriptionDTO       // Implements Req 8.7
    POST /upgrade    → StripeCheckoutDTO     // Implements Req 8.1
    POST /cancel     → HTTP 204              // Implements Req 8.4
}

// StripeWebhookController.java — bez autoryzacji JWT, weryfikacja Stripe-Signature
@RestController @RequestMapping("/api/stripe")
public class StripeWebhookController {
    POST /webhook → HTTP 200   // Implements Req 8.2, 8.3
}

// StripeService.java
public class StripeService {
    public String createCheckoutSession(UUID tenantId, Plan targetPlan); // Req 8.1
    public void handleWebhook(String payload, String signature);          // Req 8.2, 8.3
    public void cancelSubscription(UUID tenantId);                        // Req 8.4
}
```

**Odwołania do wymagań**: 8.1, 8.2, 8.3, 8.4, 8.7

---

### Komponent: PlanEnforcer

**Cel**: Weryfikacja limitów aktywnego planu przed mutacją danych

**Lokalizacja**: `bookly/domain/subscription/PlanEnforcer.java`

**Interfejs**:
```java
// PlanEnforcer.java
@Component
public class PlanEnforcer {
    // Implements Req 4.2, 8.5
    public void checkStaffLimit(UUID tenantId);

    // Implements Req 5.3, 8.6
    public void checkMonthlyBookingLimit(UUID tenantId);

    // Stałe limitów per plan
    private static final Map<Plan, Integer> STAFF_LIMITS = Map.of(
        Plan.FREE, 1, Plan.PRO, 5, Plan.ENTERPRISE, Integer.MAX_VALUE
    );

    private static final Map<Plan, Integer> MONTHLY_BOOKING_LIMITS = Map.of(
        Plan.FREE, 50, Plan.PRO, Integer.MAX_VALUE, Plan.ENTERPRISE, Integer.MAX_VALUE
    );
}
```

**Odwołania do wymagań**: 4.2, 5.3, 8.5, 8.6

---

### Komponent: PostgresDB

**Cel**: Utrwalanie wszystkich danych z izolacją per tenant przez kolumnę `tenant_id`

**Lokalizacja**: `bookly/resources/db/migration/` (Flyway)

**Kluczowe indeksy**:
```sql
-- Krytyczne indeksy dla wydajności multitenancy
CREATE INDEX idx_appointments_tenant_date ON appointments(tenant_id, start_time);
CREATE INDEX idx_appointments_staff_time ON appointments(staff_id, start_time, end_time);
CREATE INDEX idx_notifications_pending ON notifications(status, scheduled_at) WHERE status = 'PENDING';
CREATE INDEX idx_users_tenant ON users(tenant_id);
CREATE INDEX idx_services_tenant ON services(tenant_id, active);
```

**Flyway migrations**: `V1__init_schema.sql`, `V2__add_indexes.sql`, `V3__add_notifications.sql`

**Odwołania do wymagań**: Wszystkie (warstwa persystencji)

---

### Komponent: ReactAdminSPA

**Cel**: Interfejs webowy dla OWNER/STAFF

**Lokalizacja**: `frontend/admin/`

**Kluczowe widoki**:
- Dashboard — podsumowanie dzisiejszych wizyt
- Kalendarz (`FullCalendar`) — widok tygodniowy/miesięczny
- Zarządzanie usługami — lista + formularz CRUD
- Zarządzanie pracownikami — lista + harmonogram dostępności
- Subskrypcja — aktualny plan + przycisk upgrade

**Odwołania do wymagań**: 3.x, 4.x, 6.x, 8.x

---

### Komponent: ReactBookingSPA

**Cel**: Publiczny interfejs rezerwacji dla klientów końcowych danego tenanta

**Lokalizacja**: `frontend/booking/`

**Kluczowe widoki**:
- Wybór usługi
- Wybór pracownika i daty
- Wybór dostępnego slotu (z `GET /api/booking/slots`)
- Formularz danych klienta (imię, email, telefon)
- Potwierdzenie rezerwacji

**Odwołania do wymagań**: 5.x
