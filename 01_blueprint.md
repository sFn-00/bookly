# Architectural Blueprint — Bookly

## 1. Core Objective

Bookly to wielotenantowa platforma SaaS umożliwiająca małym firmom usługowym (fryzjerzy, kosmetyczki, trenerzy, kliniki) przyjmowanie rezerwacji online przez dedykowaną subdomenę. System obsługuje zarządzanie kalendarzem pracowników, konfiguracją usług, automatyczne przypomnienia (email/SMS) oraz subskrypcje planów (FREE / PRO / ENTERPRISE) rozliczane przez Stripe. Sukces oznacza: tenant może zarejestrować konto, skonfigurować usługi i pracowników, a jego klienci mogą rezerwować wizyty bez logowania — wszystko w czasie < 2 sekund odpowiedzi API.

---

## 2. System Scope and Boundaries

### In Scope
- Rejestracja i onboarding tenantów (firm)
- Routing subdomenowy (`hairsalon.bookly.pl`) z izolacją danych per tenant
- Zarządzanie usługami (CRUD), pracownikami i harmonogramem dostępności
- Publiczne API rezerwacji dla klientów końcowych (bez logowania)
- Zarządzanie wizytami przez panel admina (OWNER / STAFF)
- Automatyczne przypomnienia email (wszyscy) i SMS (plan PRO+)
- Integracja Stripe — subskrypcje planów z webhookami
- JWT autoryzacja z rolami (OWNER / STAFF / CLIENT)
- Egzekwowanie limitów planów (liczba pracowników, liczba rezerwacji/miesiąc)
- Panel admina (React SPA)
- Strona rezerwacji klienta (React SPA per tenant)
- Docker + Nginx deployment

### Out of Scope
- Własna domena (dedykowana domena — tylko ENTERPRISE, konfiguracja manualna)
- Aplikacja mobilna (iOS / Android)
- Wideokonferencje / teleporady online
- Wielojęzyczność (i18n) — tylko język polski w MVP
- Marketplace tenantów (katalog firm)
- Raportowanie i analityka zaawansowana
- Import/export danych (CSV, Excel)
- Integracja z zewnętrznymi kalendarzami (Google Calendar, Outlook)
- Live chat z klientami

---

## 3. Core System Components

| Nazwa komponentu | Pojedyncza odpowiedzialność |
|---|---|
| **NginxGateway** | Odbiera ruch HTTP/S, routuje żądania wg subdomeny do Spring Boot API; terminuje SSL |
| **TenantResolver** | Wyciąga identyfikator tenanta z subdomeny requesta i ustawia go w TenantContext (ThreadLocal) |
| **AuthModule** | Obsługuje rejestrację tenantów, logowanie użytkowników, wydawanie i odświeżanie tokenów JWT |
| **TenantModule** | Zarządza danymi tenantów — tworzenie konta, konfiguracja, aktywacja/deaktywacja |
| **ServiceModule** | CRUD usług oferowanych przez tenanta (nazwa, czas trwania, cena) |
| **StaffModule** | Zarządzanie pracownikami tenanta i ich harmonogramem dostępności |
| **BookingEngine** | Oblicza dostępne sloty czasowe, tworzy i anuluje rezerwacje klientów końcowych |
| **AppointmentModule** | Panel admina — przeglądanie, potwierdzanie i anulowanie rezerwacji przez OWNER/STAFF |
| **NotificationScheduler** | Planuje i wysyła przypomnienia email (JavaMail) i SMS (Twilio) przed wizytami |
| **SubscriptionModule** | Integracja ze Stripe — tworzenie sesji checkout, obsługa webhooków, egzekwowanie limitów planów |
| **PlanEnforcer** | Weryfikuje limity aktywnego planu tenanta (max pracownicy, max rezerwacje/miesiąc) |
| **PostgresDB** | Utrwala wszystkie dane systemu z izolacją per tenant przez kolumnę `tenant_id` |
| **ReactAdminSPA** | Interfejs webowy dla OWNER/STAFF — dashboard, zarządzanie, kalendarz |
| **ReactBookingSPA** | Publiczny interfejs rezerwacji dla klientów końcowych danego tenanta |

---

## 4. High-Level Data Flow

```mermaid
graph TD
    CLIENT_END[Klient końcowy\nhairsalon.bookly.pl] --> NGINX[NginxGateway]
    ADMIN_USER[Owner / Staff\nhairsalon.bookly.pl/admin] --> NGINX

    NGINX --> TENANT_RES[TenantResolver\nsubdomain → tenant_id]

    TENANT_RES --> AUTH[AuthModule\nJWT]
    TENANT_RES --> BOOKING[BookingEngine\nPublic API]
    TENANT_RES --> APPT[AppointmentModule\nAdmin API]
    TENANT_RES --> SVC[ServiceModule\nAdmin API]
    TENANT_RES --> STAFF[StaffModule\nAdmin API]
    TENANT_RES --> SUB[SubscriptionModule\nStripe]

    AUTH --> DB[(PostgresDB\ntenant_id isolation)]
    BOOKING --> PE[PlanEnforcer]
    BOOKING --> DB
    APPT --> DB
    SVC --> DB
    STAFF --> DB
    SUB --> DB

    PE --> DB

    DB --> NS[NotificationScheduler\n@Scheduled]
    NS --> EMAIL[JavaMailSender\nGmail SMTP]
    NS --> SMS[Twilio API\nplan PRO+]

    SUB --> STRIPE[Stripe API\nwebhooks]
    STRIPE --> SUB

    style NginxGateway fill:#fff3e0
    style TenantResolver fill:#e3f2fd
    style BookingEngine fill:#e8f5e9
    style NotificationScheduler fill:#fce4ec
    style SubscriptionModule fill:#f3e5f5
    style PlanEnforcer fill:#fff9c4
    style PostgresDB fill:#efebe9
```

---

## 5. Key Integration Points

- **NginxGateway ↔ TenantResolver**: HTTP Header `Host` — Nginx przekazuje pełny hostname, TenantResolver parsuje subdomenę
- **TenantResolver ↔ wszystkie moduły**: ThreadLocal `TenantContext` — ustawiony raz per request, czyszczony w `finally`
- **AuthModule ↔ wszystkie Admin API**: JWT Bearer Token — claim `tenantId` + `role` weryfikowany przez Spring Security filter
- **BookingEngine ↔ PlanEnforcer**: synchroniczne wywołanie przed zapisem rezerwacji — sprawdza limit miesięczny
- **StaffModule ↔ BookingEngine**: REST-internal — BookingEngine odpytuje dostępność pracowników przy obliczaniu slotów
- **NotificationScheduler ↔ PostgresDB**: `@Scheduled` co minutę — odpytuje tabelę `notifications` o statusie `PENDING` i `scheduled_at <= NOW()`
- **NotificationScheduler ↔ Twilio**: REST API (`twilio-java` SDK) — ACCOUNT_SID + AUTH_TOKEN z zmiennych środowiskowych
- **SubscriptionModule ↔ Stripe**: REST API (`stripe-java` v25.6.0) — Checkout Session + webhook `POST /api/stripe/webhook`
- **ReactAdminSPA ↔ Spring Boot API**: REST/JSON — React Query zarządza cache i refetch
- **ReactBookingSPA ↔ Spring Boot API**: REST/JSON — publiczne endpointy bez autoryzacji
- **Autentykacja**: Access Token (15 min) + Refresh Token (7 dni), JWT z claimami `sub`, `tenantId`, `role`
- **Format danych**: JSON między wszystkimi komponentami; UUID jako klucze główne
