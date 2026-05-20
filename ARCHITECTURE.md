# Bookly — Architektura systemu

## Opis projektu
Bookly to platforma SaaS do zarządzania rezerwacjami online. Umożliwia małym firmom (fryzjerzy, kosmetyczki, trenerzy) oraz klinikom/gabinetom medycznym przyjmowanie rezerwacji online, zarządzanie kalendarzem i wysyłanie przypomnień do klientów.

---

## Architektura systemu

```
                    bookly.pl (Landing)
                         ↓
              ┌──────────────────────┐
              │     Nginx / Gateway  │
              │  (subdomain routing) │
              └──────────────────────┘
                    ↓           ↓
        hairsalon.bookly.pl   dentist.bookly.pl
                    ↓           ↓
              ┌──────────────────────┐
              │   Spring Boot API    │
              │   (REST + Security)  │
              └──────────────────────┘
                         ↓
              ┌──────────────────────┐
              │     PostgreSQL       │
              │  (shared DB,         │
              │   tenant isolation)  │
              └──────────────────────┘
```

---

## Strategia multitenancy

**Wybrana opcja: Shared Database, Shared Schema**

Jedna baza danych, jedna tabela per encja, kolumna `tenant_id` w każdej tabeli.

```sql
SELECT * FROM appointments WHERE tenant_id = 'hairsalon';
```

**Zalety:**
- Najtańsze w utrzymaniu
- Łatwe skalowanie
- Prosta migracja schematu

**Ryzyko:**
- Wyciek danych jeśli zapomnisz o `tenant_id` w zapytaniu
- Mitygacja: TenantFilter + TenantContext pilnują tenant_id na poziomie aplikacji

---

## Model bazy danych

### tenants
| Kolumna | Typ | Opis |
|---|---|---|
| id | UUID | PK |
| name | VARCHAR | Nazwa firmy |
| subdomain | VARCHAR (unique) | Subdomena np. "hairsalon" |
| plan | ENUM | FREE / PRO / ENTERPRISE |
| stripe_customer_id | VARCHAR | ID klienta w Stripe |
| stripe_subscription_id | VARCHAR | ID subskrypcji w Stripe |
| active | BOOLEAN | Czy konto aktywne |
| created_at | TIMESTAMP | Data rejestracji |

### users
| Kolumna | Typ | Opis |
|---|---|---|
| id | UUID | PK |
| tenant_id | UUID (FK) | Przynależność do tenanta |
| email | VARCHAR (unique) | Email użytkownika |
| password | VARCHAR | Bcrypt hash |
| role | ENUM | OWNER / STAFF / CLIENT |
| first_name | VARCHAR | Imię |
| last_name | VARCHAR | Nazwisko |
| phone | VARCHAR | Numer telefonu |
| created_at | TIMESTAMP | Data rejestracji |

### services
| Kolumna | Typ | Opis |
|---|---|---|
| id | UUID | PK |
| tenant_id | UUID (FK) | Przynależność do tenanta |
| name | VARCHAR | Nazwa usługi np. "Strzyżenie" |
| description | TEXT | Opis usługi |
| duration_minutes | INT | Czas trwania w minutach |
| price | DECIMAL | Cena usługi |
| active | BOOLEAN | Czy usługa aktywna |

### staff
| Kolumna | Typ | Opis |
|---|---|---|
| id | UUID | PK |
| tenant_id | UUID (FK) | Przynależność do tenanta |
| user_id | UUID (FK) | Powiązany użytkownik |
| bio | TEXT | Opis pracownika |

### availability
| Kolumna | Typ | Opis |
|---|---|---|
| id | UUID | PK |
| staff_id | UUID (FK) | Powiązany pracownik |
| day_of_week | ENUM | MON / TUE / WED / THU / FRI / SAT / SUN |
| start_time | TIME | Początek pracy |
| end_time | TIME | Koniec pracy |

### appointments
| Kolumna | Typ | Opis |
|---|---|---|
| id | UUID | PK |
| tenant_id | UUID (FK) | Przynależność do tenanta |
| client_id | UUID (FK) | Klient który rezerwuje |
| staff_id | UUID (FK) | Pracownik obsługujący |
| service_id | UUID (FK) | Zarezerwowana usługa |
| start_time | TIMESTAMP | Początek wizyty |
| end_time | TIMESTAMP | Koniec wizyty |
| status | ENUM | PENDING / CONFIRMED / CANCELLED |
| notes | TEXT | Notatki do rezerwacji |
| created_at | TIMESTAMP | Data utworzenia |

### notifications
| Kolumna | Typ | Opis |
|---|---|---|
| id | UUID | PK |
| appointment_id | UUID (FK) | Powiązana rezerwacja |
| type | ENUM | EMAIL / SMS |
| scheduled_at | TIMESTAMP | Kiedy wysłać |
| sent_at | TIMESTAMP | Kiedy wysłano |
| status | ENUM | PENDING / SENT / FAILED |

### subscriptions
| Kolumna | Typ | Opis |
|---|---|---|
| id | UUID | PK |
| tenant_id | UUID (FK) | Przynależność do tenanta |
| plan | ENUM | FREE / PRO / ENTERPRISE |
| status | ENUM | ACTIVE / CANCELLED / PAST_DUE |
| current_period_start | TIMESTAMP | Początek okresu subskrypcji |
| current_period_end | TIMESTAMP | Koniec okresu subskrypcji |

---

## Plany subskrypcji

| Feature | FREE | PRO | ENTERPRISE |
|---|---|---|---|
| Pracownicy | 1 | 5 | Nielimitowani |
| Rezerwacje/mies | 50 | Nielimitowane | Nielimitowane |
| Email przypomnienia | ✅ | ✅ | ✅ |
| SMS przypomnienia | ❌ | ✅ | ✅ |
| Własne logo/kolory | ❌ | ✅ | ✅ |
| API access | ❌ | ❌ | ✅ |
| Dedykowana domena | ❌ | ❌ | ✅ |
| Cena | 0 zł | 99 zł/mies | 299 zł/mies |

---

## Struktura projektu Spring Boot

```
bookly/
├── config/
│   ├── SecurityConfig.java
│   ├── TenantContext.java
│   └── TenantFilter.java
├── domain/
│   ├── tenant/
│   │   ├── Tenant.java
│   │   ├── TenantRepository.java
│   │   └── TenantService.java
│   ├── user/
│   │   ├── User.java
│   │   ├── UserRepository.java
│   │   └── UserService.java
│   ├── service/
│   │   ├── Service.java
│   │   ├── ServiceRepository.java
│   │   └── ServiceService.java
│   ├── appointment/
│   │   ├── Appointment.java
│   │   ├── AppointmentRepository.java
│   │   └── AppointmentService.java
│   ├── availability/
│   │   ├── Availability.java
│   │   ├── AvailabilityRepository.java
│   │   └── AvailabilityService.java
│   └── notification/
│       ├── Notification.java
│       ├── NotificationRepository.java
│       └── NotificationService.java
├── api/
│   ├── auth/
│   │   └── AuthController.java
│   ├── admin/
│   │   ├── AppointmentAdminController.java
│   │   ├── ServiceAdminController.java
│   │   ├── StaffAdminController.java
│   │   └── AvailabilityAdminController.java
│   └── booking/
│       └── BookingController.java
└── infrastructure/
    ├── email/
    │   └── EmailService.java
    ├── sms/
    │   └── SmsService.java
    └── stripe/
        └── StripeService.java
```

---

## Endpointy REST API

### Auth
```
POST /api/auth/register        — rejestracja tenanta
POST /api/auth/login           — logowanie
POST /api/auth/refresh         — odświeżenie tokenu JWT
```

### Booking (klient końcowy)
```
GET  /api/booking/services           — lista usług tenanta
GET  /api/booking/slots              — dostępne terminy
POST /api/booking/appointments       — stwórz rezerwację
PUT  /api/booking/appointments/{id}/cancel  — anuluj rezerwację
```

### Admin (właściciel/pracownik)
```
GET  /api/admin/appointments         — lista rezerwacji
PUT  /api/admin/appointments/{id}/confirm  — potwierdź
PUT  /api/admin/appointments/{id}/cancel   — anuluj

GET  /api/admin/services             — lista usług
POST /api/admin/services             — dodaj usługę
PUT  /api/admin/services/{id}        — edytuj usługę
DELETE /api/admin/services/{id}      — usuń usługę

GET  /api/admin/availability         — godziny pracy
POST /api/admin/availability         — ustaw godziny
PUT  /api/admin/availability/{id}    — edytuj godziny

GET  /api/admin/staff                — lista pracowników
POST /api/admin/staff                — dodaj pracownika
```

### Subscription
```
GET  /api/subscription               — aktualny plan
POST /api/subscription/upgrade       — upgrade planu (Stripe)
POST /api/subscription/cancel        — anuluj subskrypcję
```

---

## Kluczowy mechanizm — TenantContext

```java
// TenantContext.java
public class TenantContext {
    private static ThreadLocal<String> currentTenant = new ThreadLocal<>();

    public static void setTenant(String tenantId) {
        currentTenant.set(tenantId);
    }

    public static String getTenant() {
        return currentTenant.get();
    }

    public static void clear() {
        currentTenant.remove();
    }
}

// TenantFilter.java
@Component
public class TenantFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        String subdomain = extractSubdomain(req.getServerName());
        // hairsalon.bookly.pl → "hairsalon"

        TenantContext.setTenant(subdomain);
        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private String extractSubdomain(String serverName) {
        String[] parts = serverName.split("\\.");
        if (parts.length > 2) {
            return parts[0];
        }
        return null;
    }
}
```

---

## Stack technologiczny

| Warstwa | Technologia |
|---|---|
| Backend | Spring Boot 3, Spring Security, Spring Data JPA |
| Baza danych | PostgreSQL |
| Autoryzacja | JWT (Access + Refresh token) |
| Frontend | React, React Query, Tailwind CSS |
| Kalendarz | FullCalendar |
| Email | JavaMailSender + Gmail SMTP |
| SMS | Twilio API (plan PRO+) |
| Płatności | Stripe |
| Deployment | Docker + Nginx |
| CI/CD | GitHub Actions |

---

## MVP — kolejność implementacji

```
Faza 1 — Backend core
├── Setup projektu Spring Boot
├── Model bazy danych + migracje (Flyway)
├── TenantFilter + TenantContext
├── Auth (JWT login/register)
├── Services CRUD
├── Availability logic
└── Appointments CRUD

Faza 2 — Frontend
├── Landing page bookly.pl
├── Onboarding (rejestracja tenanta)
├── Dashboard admina
└── Strona rezerwacji dla klientów

Faza 3 — Integracje
├── Email przypomnienia (JavaMail)
├── Stripe subskrypcje
└── SMS Twilio (plan PRO)

Faza 4 — Polish
├── Testy integracyjne
├── Docker + deployment
└── CI/CD GitHub Actions
```
