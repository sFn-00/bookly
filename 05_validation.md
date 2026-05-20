# Validation Report — Bookly

## 1. Macierz Traceability: Wymagania → Zadania

| Wymaganie | Kryterium akceptacji | Implementujące zadania | Status |
|---|---|---|---|
| **1. Rejestracja tenanta** | 1.1 Walidacja unikalności | Task 4.2, 4.4 | ✅ Pokryte |
| | 1.2 Utworzenie rekordu tenanta | Task 4.2, 4.1 | ✅ Pokryte |
| | 1.3 Zwrot tokenów JWT (HTTP 201) | Task 4.3, 4.4 | ✅ Pokryte |
| | 1.4 HTTP 409 — subdomena zajęta | Task 4.2, 4.4 | ✅ Pokryte |
| | 1.5 HTTP 409 — email zajęty | Task 4.2, 4.4 | ✅ Pokryte |
| **2. Autoryzacja JWT i izolacja** | 2.1 Ekstrakcja subdomeny → TenantContext | Task 3.1, 3.2 | ✅ Pokryte |
| | 2.2 HTTP 404 dla nieznanego tenanta | Task 3.2 | ✅ Pokryte |
| | 2.3 Weryfikacja JWT + claima tenantId | Task 4.3, 4.8 | ✅ Pokryte |
| | 2.4 HTTP 403 — tenant mismatch | Task 4.8 | ✅ Pokryte |
| | 2.5 HTTP 401 — wygasły access token | Task 4.6, 4.7 | ✅ Pokryte |
| | 2.6 HTTP 403 — niewystarczająca rola | Task 4.7 | ✅ Pokryte |
| **3. ServiceModule** | 3.1 Zapis usługi z tenant_id | Task 5.3, 5.5 | ✅ Pokryte |
| | 3.2 Update tylko własnej usługi | Task 5.3, 5.5 | ✅ Pokryte |
| | 3.3 Soft delete (active = false) | Task 5.3, 5.5 | ✅ Pokryte |
| | 3.4 Zwrot tylko usług bieżącego tenanta | Task 5.2, 5.3 | ✅ Pokryte |
| | 3.5 HTTP 409 — usługa z przyszłymi wizytami | Task 5.4 | ✅ Pokryte |
| **4. StaffModule** | 4.1 Wywołanie PlanEnforcer przy dodawaniu | Task 7.2 | ✅ Pokryte |
| | 4.2 HTTP 403 — limit pracowników | Task 6.1, 6.3, 7.2 | ✅ Pokryte |
| | 4.3 Zapis harmonogramu dostępności | Task 7.3, 7.4 | ✅ Pokryte |
| | 4.4 HTTP 409 — overlap harmonogramu | Task 7.3 | ✅ Pokryte |
| | 4.5 Lista pracowników per tenant | Task 7.4 | ✅ Pokryte |
| **5. BookingEngine** | 5.1 Obliczanie wolnych slotów | Task 8.1, 8.4 | ✅ Pokryte |
| | 5.2 Wywołanie PlanEnforcer przed rezerwacją | Task 8.2 | ✅ Pokryte |
| | 5.3 HTTP 403 — limit miesięczny | Task 6.2, 6.3, 8.2 | ✅ Pokryte |
| | 5.4 Zapis rezerwacji (PENDING, HTTP 201) | Task 8.2, 8.4 | ✅ Pokryte |
| | 5.5 Optimistic locking → HTTP 409 | Task 2.4, 8.2 | ✅ Pokryte |
| | 5.6 Anulowanie rezerwacji z walidacją tenant_id | Task 8.3, 8.4 | ✅ Pokryte |
| **6. AppointmentModule** | 6.1 Lista rezerwacji z filtrami per tenant | Task 9.1, 9.5 | ✅ Pokryte |
| | 6.2 Confirm → CONFIRMED + Notifications | Task 9.2, 9.3, 9.5 | ✅ Pokryte |
| | 6.3 Cancel → CANCELLED + anuluj notifications | Task 9.4, 9.5 | ✅ Pokryte |
| | 6.4 HTTP 409 — niepoprawny status przy confirm | Task 9.2 | ✅ Pokryte |
| **7. NotificationScheduler** | 7.1 Tworzenie EMAIL notification (start-24h) | Task 9.3 | ✅ Pokryte |
| | 7.2 Tworzenie SMS notification (start-2h, PRO+) | Task 9.3 | ✅ Pokryte |
| | 7.3 @Scheduled — dispatch PENDING notifications | Task 10.5 | ✅ Pokryte |
| | 7.4 Email wysłany → status SENT | Task 10.2, 10.5 | ✅ Pokryte |
| | 7.5 Błąd wysyłki → status FAILED + log | Task 10.5 | ✅ Pokryte |
| | 7.6 SMS przez twilio-java z env vars | Task 10.3, 10.4 | ✅ Pokryte |
| **8. SubscriptionModule** | 8.1 Stripe Checkout Session → URL | Task 11.2, 11.6 | ✅ Pokryte |
| | 8.2 Webhook checkout.completed → plan update | Task 11.3, 11.4 | ✅ Pokryte |
| | 8.3 Webhook subscription.deleted → FREE | Task 11.3, 11.5 | ✅ Pokryte |
| | 8.4 Cancel subscription → Stripe + lokalny status | Task 11.6 | ✅ Pokryte |
| | 8.5 Limit 1 pracownika dla FREE | Task 6.1, 6.3 | ✅ Pokryte |
| | 8.6 Limit 50 rezerwacji/mies dla FREE | Task 6.2, 6.3 | ✅ Pokryte |
| | 8.7 GET /subscription → aktualny plan i status | Task 11.6 | ✅ Pokryte |

---

## 2. Analiza pokrycia

### Podsumowanie

| Metryka | Wartość |
|---|---|
| **Wymagania** | 8 |
| **Łączna liczba kryteriów akceptacji** | 40 |
| **Kryteria pokryte przez zadania** | 40 |
| **Procent pokrycia** | **100%** |
| **Zadania implementacyjne** | 15 głównych (54 podzadania) |

### Status szczegółowy

**Pokryte kryteria** (wszystkie 40):
1.1, 1.2, 1.3, 1.4, 1.5,
2.1, 2.2, 2.3, 2.4, 2.5, 2.6,
3.1, 3.2, 3.3, 3.4, 3.5,
4.1, 4.2, 4.3, 4.4, 4.5,
5.1, 5.2, 5.3, 5.4, 5.5, 5.6,
6.1, 6.2, 6.3, 6.4,
7.1, 7.2, 7.3, 7.4, 7.5, 7.6,
8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7

**Brakujące kryteria**: Brak ✅

**Nieważne referencje w zadaniach**: Brak ✅

---

## 3. Mapa zależności między zadaniami

```
Task 1 (Setup)
    └── Task 2 (DB Schema)
            └── Task 3 (TenantResolver)
                    └── Task 4 (AuthModule)
                            ├── Task 5 (ServiceModule)
                            ├── Task 6 (PlanEnforcer) ← współdzielone przez Tasks 7, 8
                            │       └── Task 7 (StaffModule)
                            │       └── Task 8 (BookingEngine)
                            └── Task 9 (AppointmentModule)
                                    └── Task 10 (NotificationScheduler)
                                    └── Task 11 (SubscriptionModule)
Tasks 12-13 (Frontend) — równolegle po Task 4+
Tasks 14-15 (Tests + Deploy) — na końcu
```

---

## 4. Ryzyka i mitygacje

| Ryzyko | Prawdopodobieństwo | Mitygacja |
|---|---|---|
| Wyciek danych między tenantami | Wysokie (shared DB) | TenantFilter + TenantContext w każdym repozytorium; test izolacji 14.3 |
| Race condition przy rezerwacji | Średnie | Optimistic locking (`@Version`) + Task 14.5 |
| Webhook Stripe nie dotarł | Niskie | Weryfikacja `Stripe-Signature`; retry po stronie Stripe |
| SMTP limit Gmail | Średnie (produkcja) | Migracja do SendGrid/Mailgun dla PRO/ENTERPRISE |
| Twilio dostępność | Niskie | Retry w `NotificationScheduler` przy statusie FAILED |

---

## 5. Finalna Walidacja

Wszystkie **40 kryteriów akceptacji** z `02_requirements.md` są w pełni przetracowane do zadań implementacyjnych w `04_tasks.md`. Plan jest zwalidowany i gotowy do wykonania.

**Wpisz `execute` żeby rozpocząć implementację.**
