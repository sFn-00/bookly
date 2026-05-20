# Verifiable Research and Technology Proposal — Bookly

## 1. Core Problem Analysis

Bookly to platforma SaaS do rezerwacji online obsługująca wielu tenantów (fryzjerzy, kosmetyczki, kliniki). Kluczowe wyzwania techniczne to: izolacja danych między tenantami w modelu shared-database, zarządzanie dostępnością pracowników w czasie rzeczywistym, integracja z systemem płatności subskrypcyjnych oraz niezawodne wysyłanie przypomnień (email/SMS).

---

## 2. Verifiable Technology Recommendations

| Technologia / Pattern | Uzasadnienie i Dowody |
|---|---|
| **Spring Boot 3 + Spring Data JPA** | Sprawdzony stack dla aplikacji SaaS multitenantowych. Produkcyjne wzorce implementacji (TenantContext, TenantFilter, ThreadLocal) są dobrze udokumentowane i battle-tested [cite:4]. Spring Boot 3 wymaga Java 17+ i jest aktualnym standardem. [cite:2] |
| **Shared Database, Shared Schema (tenant_id)** | Najtańsza i najłatwiejsza w utrzymaniu strategia multitenancy. Każda tabela zawiera kolumnę `tenant_id`, ryzyko wycieku danych jest mitygowane przez Hibernate Filters i TenantContext na poziomie aplikacji [cite:8]. Dla MVP z ograniczoną liczbą tenantów jest to optymalne rozwiązanie [cite:3]. |
| **TenantContext via ThreadLocal** | Standardowy mechanizm w Spring Boot: filtr HTTP wyciąga subdomenę z requesta, ustawia ją w `ThreadLocal`, czyszcząc po zakończeniu requesta. Wzorzec jest produkcyjnie stosowany i opisany w wielu źródłach [cite:4][cite:6]. |
| **PostgreSQL** | Rekomendowany dla multitenantowych SaaS ze względu na elastyczność, Row-Level Security, i dojrzałość. Wspierany przez wszystkie frameworki ORM. Jest standardowym wyborem dla Spring Boot aplikacji [cite:11]. |
| **JWT (Access + Refresh Token)** | Bezstanowe uwierzytelnianie idealne dla REST API. Token JWT powinien zawierać claim `tenantId` aby umożliwić weryfikację tenanta bez dodatkowych zapytań do bazy [cite:9]. |
| **Stripe (subskrypcje)** | Najlepiej udokumentowana platforma płatnicza z dedykowanym Java SDK (`stripe-java` v25.6.0). Obsługuje modele subskrypcyjne, webhooks i Checkout Sessions. Integracja z Spring Boot jest prosta i dobrze opisana [cite:15]. Stripe wymaga użycia webhooków do synchronizacji statusu subskrypcji. |
| **Twilio (SMS)** | Standardowe API dla SMS w aplikacjach Java/Spring Boot. Biblioteka `twilio-java` umożliwia wysyłkę SMS i zaplanowanych wiadomości. Integracja jest prosta — wymaga ACCOUNT_SID i AUTH_TOKEN [cite:22]. |
| **JavaMailSender + SMTP** | Wbudowany mechanizm Spring Boot do wysyłki emaili. Wystarczający dla przypomnień email bez konieczności zewnętrznych zależności ponad standard `spring-boot-starter-mail`. |
| **Spring `@Scheduled` + Quartz** | Do planowania przypomnień o wizytach. Spring `@Scheduled` wystarczy dla MVP, Quartz dla produkcji z persistence i cluster support. |
| **React + Tailwind CSS + React Query** | Nowoczesny stack frontendowy. React Query obsługuje cache i synchronizację stanu z API. FullCalendar integruje się natywnie z React. |
| **Flyway** | Zarządzanie migracjami schematu bazy danych. Standard dla Spring Boot + PostgreSQL. Umożliwia kontrolę wersji schematu i automatyczne migracje przy deployu. |
| **Docker + Nginx (subdomain routing)** | Nginx jako reverse proxy do routingu subdomen (`hairsalon.bookly.pl → Spring Boot`). Docker zapewnia powtarzalność środowiska. |

---

## 3. Przeglądane Źródła

- [1] https://medium.com/@shahharsh172/building-secure-multi-tenant-applications-with-spring-boot-a-complete-implementation-guide-3e5857bc7f7f
- [2] https://medium.com/@ShantKhayalian/best-practices-for-implementing-multi-tenancy-in-spring-boot-saas-apps-32bc5ed34680
- [3] https://baliansblog.com/best-practices-for-implementing-multi-tenancy-in-spring-boot-saas-apps/
- [4] https://oneuptime.com/blog/post/2026-01-25-multi-tenant-saas-apps-spring-boot/view
- [5] https://medium.com/javarevisited/a-guide-to-multi-tenancy-in-spring-boot-ea1fb92fd787
- [6] https://jomatt.io/how-to-build-a-multi-tenant-saas-solution-sample-app/
- [7] https://medium.com/@erikyryan/from-zero-to-saas-building-a-multi-tenant-api-with-spring-boot-and-ddd-708510187d3d
- [8] https://www.romexsoft.com/blog/multi-tenancy-in-saas-unlocking-its-potential-with-aws/
- [9] https://www.springfuse.com/multi-tenancy-in-spring-boot-microservices/
- [10] https://medium.com/@konstde00/spring-boot-multi-tenant-architecture-overview-88198ea3991f
- [11] https://kinsta.com/blog/stripe-java-api/
- [12] https://www.codingshuttle.com/blogs/integrating-stripe-payments-in-spring-boot-step-by-step-beginners-guide-2025/
- [13] https://docs.stripe.com/billing/subscriptions/build-subscriptions
- [14] https://www.twilio.com/docs/sms/tutorials/server-notifications-java-spring
- [15] https://www.twilio.com/en-us/blog/developers/tutorials/integrations/pomodoro-timer-java-spring-boot-sms
