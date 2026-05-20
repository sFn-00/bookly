# Implementation Plan — Bookly

## Faza 1: Backend Core

---

- [ ] **1. Konfiguracja projektu i infrastruktura**
  - [ ] 1.1 Inicjalizacja projektu Spring Boot 3 (Maven, Java 21) z zależnościami: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-mail`, `flyway-core`, `postgresql`, `jjwt`, `stripe-java`, `twilio`
  - [ ] 1.2 Konfiguracja `application.yml` — datasource PostgreSQL, mail SMTP, profile `dev` i `prod`
  - [ ] 1.3 Konfiguracja Docker Compose: `postgres`, `spring-boot-api`, `nginx`
  - [ ] 1.4 Konfiguracja Nginx subdomain routing (`nginx.conf`) — przekazywanie nagłówka `Host`
  - _Requirements: 2.1_

---

- [ ] **2. Model bazy danych i migracje Flyway**
  - [ ] 2.1 Utworzenie `V1__init_schema.sql` z tabelami: `tenants`, `users`, `services`, `staff`, `availability`, `appointments`, `notifications`, `subscriptions`
  - [ ] 2.2 Dodanie kolumn `tenant_id UUID NOT NULL` do wszystkich tabel (poza `tenants`)
  - [ ] 2.3 Utworzenie `V2__add_indexes.sql` — indeksy na `(tenant_id, start_time)`, `(status, scheduled_at)`, `(tenant_id, active)`
  - [ ] 2.4 Dodanie kolumny `version BIGINT` do tabeli `appointments` (optimistic locking)
  - _Requirements: 5.5_

---

- [ ] **3. TenantResolver — TenantContext i TenantFilter**
  - [ ] 3.1 Implementacja klasy `TenantContext` z `ThreadLocal<String>` — metody `setTenant()`, `getTenant()`, `clear()`
  - [ ] 3.2 Implementacja `TenantFilter` (`@Component`, `@Order(1)`) — wyodrębnij subdomenę z `Host`, wyszukaj tenant w bazie, ustaw TenantContext lub zwróć HTTP 404
  - [ ] 3.3 Test integracyjny: żądanie z nieznaną subdomeną → HTTP 404; żądanie z poprawną subdomeną → TenantContext ustawiony
  - _Requirements: 2.1, 2.2_

---

- [ ] **4. AuthModule — rejestracja i JWT**
  - [ ] 4.1 Implementacja encji `Tenant.java` i `User.java` z JPA + `TenantRepository`, `UserRepository`
  - [ ] 4.2 Implementacja `TenantService.createTenant()` — walidacja unikalności subdomeny i emaila
  - [ ] 4.3 Implementacja `JwtService` — generowanie access token (15 min) z claimami `sub`, `tenantId`, `role`; generowanie refresh token (7 dni)
  - [ ] 4.4 Implementacja `AuthController.register()` — wywołuje `TenantService.createTenant()` + `UserService.createOwner()` + `JwtService.generateTokens()`, zwraca HTTP 201
  - [ ] 4.5 Implementacja `AuthController.login()` — weryfikacja hasła bcrypt, zwrot tokenów JWT
  - [ ] 4.6 Implementacja `AuthController.refresh()` — walidacja refresh token, nowy access token
  - [ ] 4.7 Konfiguracja `SecurityConfig.java` — reguły dostępu per endpoint, wyłączenie CSRF dla API, dodanie `JwtAuthFilter`
  - [ ] 4.8 Implementacja `JwtAuthFilter` — weryfikacja tokenu, sprawdzenie `tenantId` z JWT vs `TenantContext`
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 2.3, 2.4, 2.5, 2.6_

---

- [ ] **5. ServiceModule — CRUD usług**
  - [ ] 5.1 Implementacja encji `Service.java` z polami `tenant_id`, `name`, `description`, `duration_minutes`, `price`, `active`
  - [ ] 5.2 Implementacja `ServiceRepository` — metoda `findAllByTenantIdAndActiveTrue(UUID tenantId)`
  - [ ] 5.3 Implementacja `ServiceService` — metody `findAll()`, `create()`, `update()`, `softDelete()` (wszystkie z `TenantContext.getTenant()`)
  - [ ] 5.4 Walidacja w `softDelete()`: sprawdzenie czy istnieją przyszłe appointments z tym service_id i statusem != CANCELLED → jeśli tak, HTTP 409
  - [ ] 5.5 Implementacja `ServiceAdminController` — GET, POST, PUT, DELETE z `@PreAuthorize("hasRole('OWNER') or hasRole('STAFF')")`
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

---

- [ ] **6. PlanEnforcer — egzekwowanie limitów planów**
  - [ ] 6.1 Implementacja `PlanEnforcer.checkStaffLimit(UUID tenantId)` — pobierz plan tenanta, policz aktywnych pracowników, rzuć `PlanLimitException` jeśli przekroczony
  - [ ] 6.2 Implementacja `PlanEnforcer.checkMonthlyBookingLimit(UUID tenantId)` — policz rezerwacje w bieżącym miesiącu dla tenanta (status != CANCELLED), rzuć `PlanLimitException` jeśli FREE i > 50
  - [ ] 6.3 Implementacja `PlanLimitException` → globalny `@ControllerAdvice` mapuje ją na HTTP 403 z body `{"error": "..."}`
  - _Requirements: 4.2, 5.3, 8.5, 8.6_

---

- [ ] **7. StaffModule — pracownicy i dostępność**
  - [ ] 7.1 Implementacja encji `Staff.java` i `Availability.java`
  - [ ] 7.2 Implementacja `StaffService.create()` — wywołuje `PlanEnforcer.checkStaffLimit()` przed zapisem
  - [ ] 7.3 Implementacja `StaffService.addAvailability()` — walidacja nakładania się godzin dla tego samego dnia i pracownika → HTTP 409 jeśli overlap
  - [ ] 7.4 Implementacja `StaffAdminController` i `AvailabilityAdminController`
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

---

- [ ] **8. BookingEngine — sloty i rezerwacje**
  - [ ] 8.1 Implementacja `BookingService.calculateAvailableSlots()` — pobierz harmonogram pracownika dla danego dnia, pobierz istniejące rezerwacje (status PENDING lub CONFIRMED), oblicz wolne sloty co `duration_minutes` usługi
  - [ ] 8.2 Implementacja `BookingService.book()` — wywołuje `PlanEnforcer.checkMonthlyBookingLimit()`, następnie zapis z `@Transactional` i obsługa `OptimisticLockingFailureException` → HTTP 409
  - [ ] 8.3 Implementacja `BookingService.cancel()` — walidacja `tenant_id` rezerwacji vs `TenantContext`
  - [ ] 8.4 Implementacja `BookingController` — endpointy publiczne (bez `@PreAuthorize`): GET `/services`, GET `/slots`, POST `/appointments`, PUT `/appointments/{id}/cancel`
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6_

---

- [ ] **9. AppointmentModule — panel admina**
  - [ ] 9.1 Implementacja `AppointmentService.findAll(AppointmentFilter filter)` — filtrowanie po dacie i statusie, tylko dla bieżącego tenanta
  - [ ] 9.2 Implementacja `AppointmentService.confirm(UUID id)` — zmiana statusu PENDING → CONFIRMED, wywołanie `createNotifications()` → tworzy rekordy w tabeli `notifications`
  - [ ] 9.3 Implementacja `createNotifications()` — tworzy EMAIL (scheduled_at = startTime - 24h) i opcjonalnie SMS (scheduled_at = startTime - 2h) jeśli plan PRO/ENTERPRISE
  - [ ] 9.4 Implementacja `AppointmentService.cancel(UUID id)` — zmiana statusu → CANCELLED, aktualizacja powiązanych notifications na status CANCELLED (jeśli PENDING)
  - [ ] 9.5 Implementacja `AppointmentAdminController` — GET `/appointments`, PUT `/{id}/confirm`, PUT `/{id}/cancel`
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 7.1, 7.2_

---

## Faza 2: Integracje zewnętrzne

---

- [ ] **10. NotificationScheduler — email i SMS**
  - [ ] 10.1 Konfiguracja `JavaMailSender` w `application.yml` — Gmail SMTP (host, port, auth, TLS)
  - [ ] 10.2 Implementacja `EmailService.sendReminder(Notification n)` — budowa treści maila z danymi wizyty, wysyłka przez `JavaMailSender`
  - [ ] 10.3 Konfiguracja Twilio w `application.yml` — `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_FROM_NUMBER` z env vars
  - [ ] 10.4 Implementacja `SmsService.sendReminder(Notification n)` — inicjalizacja `Twilio.init()`, wywołanie `Message.creator()` z biblioteki `twilio-java`
  - [ ] 10.5 Implementacja `NotificationScheduler.processNotifications()` z `@Scheduled(fixedDelay = 60_000)` — query `notifications` gdzie `status = PENDING` i `scheduled_at <= NOW()`, dispatch do `EmailService` lub `SmsService`, aktualizacja statusu na SENT lub FAILED
  - _Requirements: 7.3, 7.4, 7.5, 7.6_

---

- [ ] **11. SubscriptionModule — Stripe**
  - [ ] 11.1 Konfiguracja `StripeConfig.java` z `@PostConstruct Stripe.apiKey = stripeSecretKey`
  - [ ] 11.2 Implementacja `StripeService.createCheckoutSession()` — tworzy lub pobiera Stripe Customer dla tenanta, tworzy Checkout Session w trybie `SUBSCRIPTION`, zwraca URL
  - [ ] 11.3 Implementacja `StripeWebhookController.handleWebhook()` — weryfikacja `Stripe-Signature` headera przez `Webhook.constructEvent()`, dispatch do handlera
  - [ ] 11.4 Implementacja handlera `checkout.session.completed` — aktualizacja `tenants.plan` i tabeli `subscriptions`
  - [ ] 11.5 Implementacja handlera `customer.subscription.deleted` — degradacja planu do FREE
  - [ ] 11.6 Implementacja `SubscriptionController` — GET `/subscription`, POST `/upgrade`, POST `/cancel`
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.7_

---

## Faza 3: Frontend

---

- [ ] **12. ReactBookingSPA — publiczna strona rezerwacji**
  - [ ] 12.1 Setup projektu React + Vite + Tailwind CSS + React Query
  - [ ] 12.2 Implementacja widoku wyboru usługi (`GET /api/booking/services`)
  - [ ] 12.3 Implementacja wyboru pracownika i daty
  - [ ] 12.4 Implementacja widoku dostępnych slotów (`GET /api/booking/slots`)
  - [ ] 12.5 Implementacja formularza klienta i potwierdzenia (`POST /api/booking/appointments`)
  - _Requirements: 5.1, 5.4_

---

- [ ] **13. ReactAdminSPA — panel admina**
  - [ ] 13.1 Setup + routing (React Router) + zarządzanie tokenami JWT w `localStorage`
  - [ ] 13.2 Implementacja logowania (`POST /api/auth/login`) i automatycznego refresh tokenu
  - [ ] 13.3 Implementacja dashboardu — dzisiejsze wizyty
  - [ ] 13.4 Implementacja kalendarza (FullCalendar) — widok tygodniowy rezerwacji
  - [ ] 13.5 Implementacja widoków zarządzania usługami (CRUD)
  - [ ] 13.6 Implementacja widoków zarządzania pracownikami i harmonogramem
  - [ ] 13.7 Implementacja widoku subskrypcji — aktualny plan, przycisk upgrade → redirect do Stripe Checkout
  - _Requirements: 3.x, 4.x, 6.x, 8.x_

---

## Faza 4: Jakość i deployment

---

- [ ] **14. Testy integracyjne**
  - [ ] 14.1 Konfiguracja Testcontainers z PostgreSQL dla testów integracyjnych
  - [ ] 14.2 Test pełnego flow: rejestracja → login → dodaj usługę → dodaj pracownika → zarezerwuj wizytę → potwierdź → sprawdź notification PENDING
  - [ ] 14.3 Test izolacji tenantów: tenant A nie widzi danych tenanta B
  - [ ] 14.4 Test PlanEnforcer: FREE plan — 51. rezerwacja w miesiącu → HTTP 403
  - [ ] 14.5 Test optimistic locking: dwa równoległe żądania na ten sam slot → jedno HTTP 201, drugie HTTP 409
  - _Requirements: 2.3, 2.4, 5.5, 8.5, 8.6_

---

- [ ] **15. CI/CD i deployment**
  - [ ] 15.1 Konfiguracja `Dockerfile` dla Spring Boot API (multi-stage build, Java 21)
  - [ ] 15.2 Konfiguracja `docker-compose.prod.yml` z sekretami przez env vars
  - [ ] 15.3 GitHub Actions workflow: build → test → Docker build → push do registry → deploy
  - [ ] 15.4 Konfiguracja SSL (Let's Encrypt / certbot) dla `*.bookly.pl`
  - _Requirements: Deployment_
