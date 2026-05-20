# Requirements Document — Bookly

## Wprowadzenie

Dokument opisuje wymagania funkcjonalne systemu Bookly. Każde kryterium akceptacji jest przypisane do konkretnego komponentu zdefiniowanego w blueprint.md i sformułowane w stylu testable behaviour.

---

## Słownik

| Termin | Definicja |
|---|---|
| **Tenant** | Firma korzystająca z platformy Bookly (np. salon fryzjerski) |
| **Owner** | Właściciel konta tenanta — rola z pełnymi uprawnieniami administracyjnymi |
| **Staff** | Pracownik tenanta — rola z ograniczonym dostępem do panelu |
| **Client** | Klient końcowy tenanta — rezerwuje wizyty przez publiczną stronę |
| **Slot** | Dostępny termin wizyty obliczony z harmonogramu pracownika |
| **Plan** | Poziom subskrypcji: FREE / PRO / ENTERPRISE |

---

## Wymagania

---

### Wymaganie 1: Rejestracja i onboarding tenanta

**Opis**: Nowa firma może zarejestrować konto w Bookly, wybierając unikalną subdomenę.

#### Kryteria akceptacji

1. WHEN użytkownik wysyła `POST /api/auth/register` z polami `companyName`, `subdomain`, `email`, `password`, THE **AuthModule** SHALL zwalidować unikalność `subdomain` i `email` w tabeli `tenants` i `users`.
2. WHEN subdomena jest unikalna i dane są poprawne, THE **TenantModule** SHALL utworzyć rekord w tabeli `tenants` z `plan = FREE`, `active = true` i wygenerowanym UUID.
3. WHEN rejestracja się powiedzie, THE **AuthModule** SHALL zwrócić parę tokenów JWT (access + refresh) w odpowiedzi HTTP 201.
4. WHEN subdomena jest już zajęta, THE **AuthModule** SHALL zwrócić HTTP 409 z komunikatem `"subdomain already taken"`.
5. WHEN email jest już zarejestrowany, THE **AuthModule** SHALL zwrócić HTTP 409 z komunikatem `"email already registered"`.

---

### Wymaganie 2: Autoryzacja JWT i izolacja tenantów

**Opis**: Każde żądanie do chronionych endpointów musi być autoryzowane i powiązane z właściwym tenantem.

#### Kryteria akceptacji

1. WHEN żądanie HTTP trafia do NginxGateway, THE **TenantResolver** SHALL wyodrębnić subdomenę z nagłówka `Host` i ustawić `tenant_id` w `TenantContext`.
2. WHEN `tenant_id` nie odpowiada żadnemu aktywnemu tenantowi w bazie, THE **TenantResolver** SHALL zwrócić HTTP 404.
3. WHEN żądanie zawiera ważny JWT Bearer Token, THE **AuthModule** SHALL zweryfikować podpis tokenu i wyciągnąć claima `tenantId` oraz `role`.
4. WHEN `tenantId` z JWT nie zgadza się z `tenant_id` z `TenantContext`, THE **AuthModule** SHALL zwrócić HTTP 403.
5. WHEN access token wygasł, THE **AuthModule** SHALL odrzucić żądanie z HTTP 401 i klient powinien użyć `POST /api/auth/refresh`.
6. WHEN użytkownik o roli STAFF próbuje uzyskać dostęp do zasobu wymagającego roli OWNER, THE **AuthModule** SHALL zwrócić HTTP 403.

---

### Wymaganie 3: Zarządzanie usługami (ServiceModule)

**Opis**: Owner może dodawać, edytować, dezaktywować i usuwać usługi oferowane przez firmę.

#### Kryteria akceptacji

1. WHEN Owner wysyła `POST /api/admin/services` z poprawnymi danymi, THE **ServiceModule** SHALL zapisać usługę z `tenant_id` z `TenantContext` i zwrócić HTTP 201.
2. WHEN Owner wysyła `PUT /api/admin/services/{id}`, THE **ServiceModule** SHALL zaktualizować wyłącznie usługę należącą do bieżącego tenanta.
3. WHEN Owner wysyła `DELETE /api/admin/services/{id}`, THE **ServiceModule** SHALL oznaczyć usługę jako `active = false` (soft delete), nie usuwając rekordu fizycznie.
4. WHEN `GET /api/admin/services` jest wywoływane, THE **ServiceModule** SHALL zwrócić wyłącznie usługi należące do tenanta z `TenantContext`.
5. WHEN usługa ma powiązane przyszłe rezerwacje i Owner próbuje ją usunąć, THE **ServiceModule** SHALL zwrócić HTTP 409 z komunikatem `"service has upcoming appointments"`.

---

### Wymaganie 4: Zarządzanie pracownikami i dostępnością (StaffModule)

**Opis**: Owner zarządza pracownikami i ich harmonogramem pracy.

#### Kryteria akceptacji

1. WHEN Owner wysyła `POST /api/admin/staff`, THE **StaffModule** SHALL zwalidować, że liczba pracowników nie przekracza limitu aktywnego planu przez wywołanie **PlanEnforcer**.
2. WHEN limit pracowników planu zostałby przekroczony, THE **PlanEnforcer** SHALL zwrócić HTTP 403 z komunikatem `"staff limit reached for current plan"`.
3. WHEN Owner wysyła `POST /api/admin/availability` dla pracownika, THE **StaffModule** SHALL zapisać harmonogram dnia tygodnia z godzinami `start_time` i `end_time`.
4. WHEN harmonogram pracownika nakłada się na istniejący wpis dla tego samego dnia, THE **StaffModule** SHALL zwrócić HTTP 409 z komunikatem `"availability overlap detected"`.
5. WHEN `GET /api/admin/staff` jest wywoływane, THE **StaffModule** SHALL zwrócić listę pracowników wyłącznie dla bieżącego tenanta.

---

### Wymaganie 5: Obliczanie wolnych slotów i rezerwacja (BookingEngine)

**Opis**: Klient końcowy może przeglądać dostępne terminy i złożyć rezerwację bez logowania.

#### Kryteria akceptacji

1. WHEN klient wywołuje `GET /api/booking/slots?serviceId={id}&staffId={id}&date={date}`, THE **BookingEngine** SHALL obliczyć listę dostępnych slotów na podstawie harmonogramu pracownika z **StaffModule** i istniejących rezerwacji ze statusem `CONFIRMED` lub `PENDING`.
2. WHEN klient wywołuje `POST /api/booking/appointments`, THE **BookingEngine** SHALL wywołać **PlanEnforcer** i sprawdzić, czy miesięczny limit rezerwacji tenanta nie zostałby przekroczony.
3. WHEN limit rezerwacji zostałby przekroczony, THE **PlanEnforcer** SHALL zwrócić HTTP 403 z komunikatem `"monthly booking limit reached"`.
4. WHEN slot jest dostępny i limit nie jest przekroczony, THE **BookingEngine** SHALL zapisać rezerwację ze statusem `PENDING` i zwrócić HTTP 201 z UUID rezerwacji.
5. WHEN dwie równoległe żądania próbują zarezerwować ten sam slot, THE **BookingEngine** SHALL obsłużyć konflikt przez optymistyczne blokowanie (optimistic locking) i drugie żądanie SHALL otrzymać HTTP 409.
6. WHEN klient wywołuje `PUT /api/booking/appointments/{id}/cancel`, THE **BookingEngine** SHALL zmienić status rezerwacji na `CANCELLED` tylko jeśli `tenant_id` rezerwacji zgadza się z `TenantContext`.

---

### Wymaganie 6: Panel administracyjny rezerwacji (AppointmentModule)

**Opis**: Owner i Staff mogą przeglądać, potwierdzać i anulować rezerwacje przez panel admina.

#### Kryteria akceptacji

1. WHEN Owner lub Staff wywołuje `GET /api/admin/appointments`, THE **AppointmentModule** SHALL zwrócić listę rezerwacji wyłącznie dla bieżącego tenanta, z możliwością filtrowania po dacie i statusie.
2. WHEN Owner lub Staff wysyła `PUT /api/admin/appointments/{id}/confirm`, THE **AppointmentModule** SHALL zmienić status rezerwacji z `PENDING` na `CONFIRMED` i wyzwolić zaplanowanie powiadomienia przez **NotificationScheduler**.
3. WHEN Owner lub Staff wysyła `PUT /api/admin/appointments/{id}/cancel`, THE **AppointmentModule** SHALL zmienić status na `CANCELLED` i anulować zaplanowane powiadomienia.
4. WHEN rezerwacja ma status inny niż `PENDING` przy próbie potwierdzenia, THE **AppointmentModule** SHALL zwrócić HTTP 409 z aktualnym statusem rezerwacji.

---

### Wymaganie 7: Powiadomienia o wizytach (NotificationScheduler)

**Opis**: System automatycznie wysyła przypomnienia o wizytach przez email (wszyscy) i SMS (plan PRO+).

#### Kryteria akceptacji

1. WHEN rezerwacja zmienia status na `CONFIRMED`, THE **AppointmentModule** SHALL utworzyć rekord w tabeli `notifications` z `type = EMAIL`, `scheduled_at = start_time - 24h`, `status = PENDING`.
2. WHEN tenant ma plan PRO lub ENTERPRISE i rezerwacja jest potwierdzona, THE **AppointmentModule** SHALL dodatkowo utworzyć rekord `notifications` z `type = SMS`, `scheduled_at = start_time - 2h`.
3. WHEN **NotificationScheduler** (uruchamiany co minutę `@Scheduled`) wykryje rekord z `status = PENDING` i `scheduled_at <= NOW()`, THE **NotificationScheduler** SHALL wysłać wiadomość przez odpowiedni kanał (email lub SMS).
4. WHEN wysyłka email się powiedzie, THE **NotificationScheduler** SHALL zaktualizować rekord z `status = SENT`, `sent_at = NOW()`.
5. WHEN wysyłka się nie powiedzie (błąd SMTP lub Twilio API), THE **NotificationScheduler** SHALL zaktualizować rekord z `status = FAILED` i zalogować błąd.
6. WHEN wysyłka SMS jest wykonywana, THE **NotificationScheduler** SHALL użyć biblioteki `twilio-java` z danymi z zmiennych środowiskowych `TWILIO_ACCOUNT_SID` i `TWILIO_AUTH_TOKEN`.

---

### Wymaganie 8: Zarządzanie subskrypcją i egzekwowanie planów (SubscriptionModule + PlanEnforcer)

**Opis**: Tenant może zmieniać plan subskrypcji przez Stripe, a system egzekwuje limity aktywnego planu.

#### Kryteria akceptacji

1. WHEN Owner wysyła `POST /api/subscription/upgrade` z nowym planem, THE **SubscriptionModule** SHALL utworzyć Stripe Checkout Session i zwrócić URL do strony płatności Stripe.
2. WHEN Stripe wyśle webhook `checkout.session.completed`, THE **SubscriptionModule** SHALL zaktualizować plan tenanta w tabeli `tenants` i `subscriptions`.
3. WHEN Stripe wyśle webhook `customer.subscription.deleted`, THE **SubscriptionModule** SHALL zdegradować plan tenanta do FREE.
4. WHEN Owner wysyła `POST /api/subscription/cancel`, THE **SubscriptionModule** SHALL wywołać Stripe API żeby anulować subskrypcję i zaktualizować lokalny status na `CANCELLED`.
5. WHEN **PlanEnforcer** sprawdza limit pracowników dla planu FREE, THE **PlanEnforcer** SHALL odrzucić operację jeśli tenant ma już 1 aktywnego pracownika.
6. WHEN **PlanEnforcer** sprawdza miesięczny limit rezerwacji dla planu FREE, THE **PlanEnforcer** SHALL odrzucić operację jeśli tenant przekroczył 50 rezerwacji w bieżącym miesiącu kalendarzowym.
7. WHEN `GET /api/subscription` jest wywoływane, THE **SubscriptionModule** SHALL zwrócić aktualny plan, status i datę końca okresu subskrypcji.
