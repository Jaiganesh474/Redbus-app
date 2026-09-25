# CLAUDE.md

This file provides guidance to Claude Code (or any AI coding assistant) when working on this repository. It defines the project scope, architecture, tech stack, and build order for a RedBus-style bus ticket booking platform.

## Project Overview

A full-stack, multi-operator bus ticket booking marketplace (similar to RedBus) with **no seeded inventory** — operators log in, add their fleet, and publish schedules that immediately become live, bookable, and searchable to users. Includes a real operator analytics dashboard driven by live bookings, AI-powered search, a support chatbot backed by RAG, an AI recommendation engine, Razorpay payments, and SEO-optimized, server-rendered route pages.

## Tech Stack

- **Backend**: Java 17+, Spring Boot 3.x (Spring Web, Spring Data JPA, Spring Security, Spring AI), Maven
- **Database**: MySQL 8 (+ vector storage for RAG — pgvector/MySQL vector support or a dedicated vector DB like Chroma/Weaviate)
- **Frontend**: Next.js (App Router) + Redux Toolkit (RTK) + RTK Query + Tailwind CSS
  - Next.js is required (not a plain Vite SPA) because route/search pages must be server-rendered or statically generated for SEO
- **Auth**: JWT (Spring Security + jjwt), BCrypt password hashing
- **AI**: Google Gemini API (via Spring AI or direct REST) for NLP query parsing, chatbot function calling, RAG-grounded answers, and recommendation re-ranking
- **Payments**: Razorpay (test mode by default)
- **Caching / Locking**: Redis preferred for seat locks and chat session state (MySQL-based fallback with a scheduled cleanup job if Redis is unavailable)

## Project Structure

**Backend** — layered architecture:
```
controller/   → REST endpoints
service/      → business logic
repository/   → Spring Data JPA repositories
entity/       → JPA entities
dto/          → request/response payloads (never expose entities directly)
config/       → security, CORS, beans
exception/    → @ControllerAdvice global handler
ai/           → chatbot, recommendation engine, RAG pipeline
```

**Frontend** — component-based, Next.js App Router:
```
app/          → pages/routes (SSR/SSG/ISR)
components/   → UI components
store/        → Redux slices + RTK Query api slice
hooks/        → custom hooks
ai/           → chat widget
```

## Core Booking Features

1. **Auth & Authorization** — email+password register/login, JWT access + refresh tokens, roles: `ROLE_USER`, `ROLE_OPERATOR`, `ROLE_ADMIN`.
2. **Bus & Route Management — Dynamic, Operator-Driven Inventory (no seed data)**
   - **No hardcoded/seeded buses, routes, or trips anywhere.** All inventory is created by operators through the Operator Dashboard, persisted to MySQL, and surfaced to users purely from live data. Search results are empty for any city pair no operator has actually scheduled — this is intentional, not a bug.
   - **Route Templates vs Trip Instances** (this distinction is core to how RedBus actually works — don't collapse it into one table):
     - `Route` = a template: source city, destination city, boarding points, dropping points, distance/duration, default base fare — created once by an operator
     - `Schedule` = recurring service definition on top of a route: which bus, departure time, days of operation (e.g. Mon/Wed/Fri, or daily), effective date range
     - `TripInstance` = the actual bookable unit for a specific calendar date, generated from a `Schedule`. Each `TripInstance` has its own independent seat inventory (`trip_seats`), its own price (can differ from base fare on holidays/demand), and its own status (`SCHEDULED`, `DEPARTED`, `CANCELLED`, `COMPLETED`)
   - **Trip generation job**: a scheduled backend job (Spring `@Scheduled`) rolls forward a configurable window (e.g. next 60 days) and materializes `TripInstance` rows from each active `Schedule`, cloning the bus's seat layout into fresh `trip_seats` rows per instance. When an operator edits a schedule (time, price, days), only *future, not-yet-booked* trip instances are regenerated/updated — trips with existing bookings are left untouched and any change to them requires an explicit operator action (with passenger notification)
   - Operator CRUD: buses (registration number, operator, bus type, total seats, amenities, photos), custom seat layouts per bus (2x1/2x2 sleeper/seater builder, not just a fixed template), routes, boarding/dropping points with pickup times, schedules
   - Seat layout stored per bus (`Seat` entities as a template), cloned into `trip_seats` per trip instance so seat status is always per-date, never shared across dates
3. **Search** — `GET /api/trips/search?source=&destination=&date=` queries `TripInstance` (status `SCHEDULED`, matching date) joined to its `Schedule`/`Route`/`Bus`, with filters (bus type, price range, departure window) and sorting. Index on `(source_city, destination_city, travel_date)` at the trip-instance level. Because everything reads from live `TripInstance` rows, any bus/schedule an operator adds is searchable by users the moment the trip-generation job materializes it — no redeploy, no manual sync step.
4. **Seat Selection & Locking** — `GET /api/trips/{tripInstanceId}/seats` returns the seat map for that specific date's trip with live status. `POST /api/seats/lock` applies a TTL-based lock (Redis preferred, or MySQL table + scheduled cleanup job) to prevent double booking.
5. **Booking & Passengers** — `POST /api/bookings` accepts trip instance id, seat ids, passenger details, contact info. Unique PNR generated. Status: `PENDING_PAYMENT` / `CONFIRMED` / `CANCELLED`. Lock-check + booking creation wrapped in `@Transactional`, decrementing that trip instance's available-seat count.
6. **Real-Time Sync Between Operator Actions and User-Facing Pages**
   - When an operator publishes a new schedule/trip, it must appear in user search results without any manual cache clear — RTK Query cache tags (`Trip`, `SeatMap`) are invalidated/refetched on relevant mutations
   - When a user books a seat, other users viewing the same trip's seat map should see it go from `AVAILABLE` to `LOCKED`/`BOOKED` promptly — implement via short-poll refetch (RTK Query `pollingInterval` on the seat map endpoint while the seat selection screen is open) or, for a more real one, a WebSocket/STOMP channel per `tripInstanceId` that pushes seat status deltas
   - When an operator cancels a trip or changes its departure time, every affected booking's user must be notified (email/SMS mock is fine) and, for cancellations, automatically queued for refund
7. **Payment — Razorpay**
   - `POST /api/payments/create-order` — creates a Razorpay order server-side
   - React/Next checkout opens Razorpay Checkout.js modal
   - `POST /api/payments/verify` — verifies `razorpay_order_id` + `razorpay_payment_id` + `razorpay_signature` via HMAC SHA256 **on the backend** (never trust the frontend alone)
   - `POST /api/payments/webhook` — handles async `payment.captured` / `payment.failed` events
   - On verified success: booking → `CONFIRMED`, seats → `BOOKED`. On failure/timeout: release seat locks
   - Refunds via Razorpay Refunds API on cancellation
8. **My Bookings** — paginated booking history, e-ticket PDF with QR code (iText + Zxing), cancellation with refund status tracking.

9. **Operator Onboarding & Identity**
   - Separate operator registration flow: company/operator name, contact person, phone, email, business/KYC documents (upload, stored as file references — mock verification is fine for a demo), bank account details for settlements
   - New operators start in `PENDING_APPROVAL`; `ROLE_ADMIN` approves/rejects via the admin panel before the operator can publish buses/schedules — mirrors real marketplace operator vetting
   - Operator login is the same JWT auth flow, scoped by `ROLE_OPERATOR` and an `operator_id` claim/lookup so every operator only ever sees and manages their own buses, schedules, trips, and bookings

10. **Operator Dashboard — Core Management**
    - Fleet: add/edit/deactivate buses, build custom seat layouts, upload bus photos
    - Routes: create routes with boarding/dropping points and pickup times
    - Schedules: create recurring schedules (bus + route + departure time + operating days + date range + base fare), edit/pause/resume a schedule (pausing stops future trip generation without touching already-generated or booked trips)
    - Trip instance overrides: for a specific date, an operator can override price (festival/demand pricing), swap the assigned bus, or cancel that single instance without touching the recurring schedule
    - Manual trip cancellation flow: operator cancels a `TripInstance` → system auto-cancels all its bookings → triggers Razorpay refunds → notifies affected passengers → frees the bus for other schedules

11. **Operator Analytics Dashboard**
    - `GET /api/operator/analytics/overview?from=&to=` — total revenue, total tickets sold, total trips run, average occupancy %, cancellation rate, for the operator's own inventory only
    - Revenue breakdown: by route, by bus, by day/week/month (time series for charts), by seat type (sleeper vs seater)
    - Occupancy breakdown: seats sold vs total capacity per trip, per route, heat-map style by day-of-week/time-slot to show demand patterns
    - Every ticket booked and paid for by a user is written against that trip's `operator_id` in real time (via the `bookings`/`payments` tables), so the analytics queries are just aggregations over the operator's own live booking data — **no separate "sync" step, no seeded numbers**; the dashboard is always a reflection of real `bookings`/`payments` rows
    - Top-performing routes/buses, low-occupancy trips flagged (candidates to cancel or re-price)
    - Cancellation & refund report: count/value of cancellations by route/date, refund turnaround
    - Exportable reports (CSV/Excel) for a date range, for the operator's accountant/back-office use
    - Platform commission line item shown per booking and summarized per settlement period (see Settlements below)
    - Charts rendered with a charting lib (e.g. Recharts) fed by these aggregation endpoints — RTK Query with cache tags per date-range/operator so the dashboard refreshes as new bookings land

12. **Settlements & Payouts** (how the operator actually gets paid — a real backend concern RedBus-style platforms handle)
    - Platform takes a configurable commission % (or flat fee) per booking, stored on the `Payment`/`Booking` record at time of transaction
    - `Settlement` records generated on a schedule (e.g. weekly) per operator: sum of gross ticket revenue for the period, minus commission, minus refunds issued, = net payable
    - Admin can mark a settlement `PAID` once the actual bank transfer happens (manual or via a payout API) — this ledger is what the operator sees under "Payouts" in their dashboard

13. **Conductor / Staff Sub-Accounts & Boarding**
    - Operator can create limited `ROLE_CONDUCTOR` sub-accounts tied to a specific bus/trip
    - Conductor app/page: scan a passenger's e-ticket QR code to mark them `BOARDED`, catches duplicate/invalid tickets, gives the operator a live manifest of who has actually boarded vs no-shows

14. **Ratings & Reviews**
    - After a `TripInstance` reaches `COMPLETED`, the user is prompted to rate the trip (bus cleanliness, punctuality, driver behavior, overall)
    - Aggregate rating stored per bus/operator, surfaced on search results and route pages, and feeds the `AggregateRating` structured data (SEO) and the recommendation engine's content-based scoring

15. **Notifications**
    - Booking confirmation, trip reminder (X hours before departure), schedule change, cancellation/refund — email/SMS (mocked provider interface is fine for a demo, structured so a real provider like Twilio/SendGrid can be swapped in)
    - Operator-side notifications: new booking received, low-occupancy alert close to departure, settlement processed

16. **Admin (Platform-Level, distinct from Operator)**
    - Approve/reject operator onboarding
    - Global view across all operators: platform-wide revenue, top operators, dispute/refund oversight
    - Manage commission rates per operator (some marketplaces negotiate different commission tiers)
    - Content moderation: flagged reviews, suspended operators/buses

## AI Features (Gemini-powered)

17. **NLP Search / Conversational Query Parsing**
    - `POST /api/ai/parse-query` sends free text (e.g. *"AC sleeper bus from Chennai to Bangalore tomorrow night under 800 rupees"*) to Gemini using structured output (function calling / JSON mode), extracting: source, destination, date, time preference, bus type, max price
   - Parsed result maps into the existing validated search API — the LLM never queries the DB directly

18. **AI Chatbot (Booking Assistant)**
    - Floating chat widget → `POST /api/ai/chat`
    - Handles FAQs, PNR status lookup, cancellation help, route recommendations, policy questions
    - Uses Gemini function calling / tool use to invoke backend tools: `searchRoutes()`, `getBookingStatus(pnr)`, `getCancellationPolicy()`, `recommendBuses(userId)`
    - Conversation history stored per session (DB or Redis) and replayed to Gemini for context
    - Guardrails: validate/sanitize all model-produced parameters before calling real services; require explicit user confirmation before any destructive action (cancel/pay)

19. **RAG Pipeline** (policy/FAQ grounding)
    - Ingest operator policies, cancellation/refund rules, FAQs, ToS into a document store
    - Chunk → embed (Gemini `text-embedding-004`) → store vectors + metadata (pgvector table or Chroma)
    - Retrieval service: embed user question → top-k similarity search → inject retrieved chunks into Gemini prompt → grounded answer with source citations
    - Exposed via `POST /api/ai/support-query`, used by the chatbot specifically for policy/FAQ questions
    - Admin re-ingestion endpoint/job for when policies are updated

20. **AI Recommendation Engine**
    - `POST /api/ai/recommendations/{userId}` (or anonymous, using session signals)
    - Hybrid approach:
      a. Collaborative/behavioral — past searches, bookings, preferred routes/times/bus type
      b. Content-based — match route/bus attributes (price band, amenities, rating)
      c. LLM re-ranking — Gemini produces "Recommended for you because…" explanations and re-ranks top picks
    - Surfaced on: landing page ("Popular routes for you"), post-search ("You might also consider…"), post-booking ("Book your return trip")
    - Candidate generation stays in SQL; Gemini is used only for final ranking/explanation to control cost and latency

## SEO Optimization

21. **Programmatic SEO for Routes**
    - Indexable landing pages per popular route: `/bus-tickets/{source-city}-to-{destination-city}`
    - SSR/ISR-rendered, pre-populated with upcoming buses, price range, popular operators, journey duration
    - Auto-generated sitemap entries for every active source–destination pair (scheduled job over `DISTINCT source_city, destination_city`)
    - Operator pages (`/bus-operator/{operator-name}`) and city hub pages (`/buses-from-{city}`)

22. **Meta Tags & Structured Data**
    - Dynamic `<title>` / `<meta description>` per page (e.g. *"Chennai to Bangalore Bus Tickets | 50+ Buses | Starting ₹450"*)
    - Open Graph + Twitter Card tags
    - Canonical URLs (filter/sort query params canonicalize to the base route URL)
    - JSON-LD: `Trip`/`Reservation` schema on route pages, `BreadcrumbList`, `FAQPage` (chatbot/help sections), `AggregateRating`, `Organization` on homepage

23. **URL & Content Structure**
    - Clean, keyword-rich URLs for primary landing pages (no query-string-only routes)
    - Semantic HTML with proper heading hierarchy (h1 = page title, h2 = sections)
    - Descriptive alt text on images
    - Internal linking between related/nearby routes

24. **Technical SEO Infrastructure**
    - `robots.txt` allowing route/search pages, disallowing `/admin`, `/my-bookings`, `/api`
    - Dynamically generated `sitemap.xml` (split by type if large), submitted via Google Search Console
    - Core Web Vitals: Next.js `<Image>` for optimized/lazy images, code-split chatbot widget, prioritize LCP (search form)
    - `hreflang` tags if multi-language/region support is added
    - 301 redirects table for renamed/merged route pages

25. **AI/Chatbot + SEO Synergy**
    - Same RAG knowledge base powers both the chatbot and a public, crawlable SSR FAQ page
    - Key route facts (price, timing, operator count) rendered as visible SSR content, not only behind the chatbot/API

## Redux Toolkit — State Management

- `store/` with `configureStore`, organized into slices:
  - `authSlice` — user, token, role (persisted via redux-persist or localStorage)
  - `searchSlice` — source, destination, date, filters, sort
  - `bookingSlice` — selected seats, locked seat ids + lock expiry countdown, passenger form state
  - `chatSlice` — chat messages, open/closed state, loading
- **RTK Query** for all server state (routes, seats, bookings, payments, recommendations, chat) — single `apiSlice` with `injectEndpoints` per domain; automatic caching/invalidation (e.g., invalidate seat map cache on lock/unlock)
- Plain slices only for client-only UI state, never for server data
- Seat-lock countdown timer synced with backend `lock_expiry`, via RTK listener middleware or a custom hook, auto-releasing expired locks client-side too

## Database Schema (MySQL — key tables)

```
users(id, name, email, password_hash, phone, role, created_at)

-- Operator identity & marketplace onboarding
operators(id, company_name, contact_person, email, phone, kyc_doc_url, bank_account_ref, commission_rate, status[PENDING_APPROVAL/APPROVED/SUSPENDED], created_at)
operator_staff(id, operator_id FK, user_id FK, staff_role[CONDUCTOR], assigned_bus_id FK nullable, created_at)

-- Fleet & route templates (operator-managed, never seeded)
buses(id, operator_id FK, registration_number, bus_type, total_seats, amenities, photo_urls, active, created_at)
seats(id, bus_id FK, seat_number, seat_type, deck)                                  -- template layout per bus
routes(id, operator_id FK, source_city, destination_city, distance_km, duration_minutes, created_at)
boarding_points(id, route_id FK, point_type[BOARDING/DROPPING], name, address, pickup_offset_minutes)

-- Recurring schedules → generated trip instances (the "no static seed data" core)
schedules(id, route_id FK, bus_id FK, departure_time, operating_days[bitmask/CSV], base_price, valid_from, valid_to, status[ACTIVE/PAUSED])
trip_instances(id, schedule_id FK, travel_date, departure_datetime, arrival_datetime, price, bus_id FK, status[SCHEDULED/DEPARTED/CANCELLED/COMPLETED], created_at)
trip_seats(id, trip_instance_id FK, seat_id FK, status[AVAILABLE/LOCKED/BOOKED], gender_restriction, lock_expiry, locked_by_user_id)

-- Bookings & payments (always written against a specific trip_instance + operator)
bookings(id, user_id FK, trip_instance_id FK, operator_id FK, pnr, total_amount, commission_amount, status, created_at)
booking_passengers(id, booking_id FK, trip_seat_id FK, name, age, gender)
payments(id, booking_id FK, amount, status, method, razorpay_order_id, razorpay_payment_id, razorpay_signature, created_at)

-- Operator finance
settlements(id, operator_id FK, period_start, period_end, gross_revenue, commission_deducted, refunds_deducted, net_payable, status[PENDING/PAID], paid_at)

-- Ratings, notifications, AI/recommendation support
trip_ratings(id, trip_instance_id FK, user_id FK, bus_rating, punctuality_rating, driver_rating, overall_rating, comment, created_at)
notifications(id, user_id FK nullable, operator_id FK nullable, type, channel[EMAIL/SMS], payload, status, created_at)
user_search_history(id, user_id, source_city, destination_city, search_date, created_at)
chat_sessions(id, user_id nullable, started_at)
chat_messages(id, session_id FK, role, content, created_at)
kb_documents(id, title, source_type, content, updated_at)
kb_chunks(id, document_id FK, chunk_text, embedding VECTOR, chunk_index)
```

Add foreign keys throughout (especially `operator_id` on every operator-owned table, for row-level scoping in queries). Index `trip_instances(travel_date)` jointly with its route's `(source_city, destination_city)` via a join or a denormalized search index; unique constraint on `bookings.pnr`; unique constraint on `(trip_instance_id, seat_id)` in `trip_seats` to make double-booking impossible at the DB level, not just the app-lock level.

## Non-Functional Requirements

- `application.yml` with dev/prod profiles; secrets (Gemini API key, Razorpay key/secret) externalized via env vars — never commit keys
- Flyway or Liquibase for DB migrations/versioning
- Bean Validation (`@Valid`) on all DTOs; global `@ControllerAdvice` exception handling
- CORS config for the Next.js frontend
- Pagination (`Pageable`) on list endpoints
- Rate-limit AI endpoints (chat, recommendations) to control token cost/abuse
- Log AI prompts/responses (PII-redacted) for debugging; fallback path (plain search/UI) if Gemini API is unavailable
- Unit tests (JUnit + Mockito) for services; mock Gemini and Razorpay clients in tests; basic controller integration tests
- Responsive, mobile-first UI

## Build/Delivery Order

1. Next.js + Spring Boot scaffold, MySQL + Flyway schema (routes/schedules/trip_instances model from the start — do not build a flattened routes-with-a-date table and migrate later)
2. Auth (backend + Next.js login/signup, `authSlice`), including operator registration + admin approval flow
3. Operator Dashboard — Fleet, Routes, Schedules CRUD + trip-generation scheduled job
4. Search API over `trip_instances` + SSR search/results pages (`searchSlice` + RTK Query, SSR-hydrated) — verify a bus added via the operator dashboard is actually searchable end-to-end before moving on
5. Seat map (`trip_seats`) + locking + seat selection UI (`bookingSlice`) + real-time seat status sync
6. Booking + Razorpay payment flow end-to-end, writing `operator_id`/`commission_amount` onto each booking
7. My Bookings + e-ticket + cancellation/refunds (user side); trip cancellation + auto-refund flow (operator side)
8. Operator Analytics Dashboard — revenue/occupancy aggregation endpoints + charts, backed purely by real `bookings`/`payments` rows
9. Settlements/payouts, conductor sub-accounts + QR boarding, ratings & reviews, notifications
10. Programmatic SEO route pages (SSG/ISR) + sitemap generation + meta tags + JSON-LD
11. Gemini NLP query parsing on the search bar
12. AI chatbot with function calling
13. RAG pipeline for policy/FAQ (also rendered as a public SSR FAQ page for SEO)
14. AI recommendation engine (landing page + post-search + post-booking)
15. Admin (platform-level) dashboard — operator approvals, commission management, dispute oversight
16. Technical SEO pass: Core Web Vitals audit, `robots.txt`, redirects, structured data validation (Google Rich Results Test)

## Open Questions to Resolve Before/During Implementation

- Gemini access: Google AI Studio API key vs Vertex AI
- Vector store choice: MySQL/pgvector vs a dedicated vector DB (Chroma/Weaviate/Pinecone)
- Redis availability for seat locking + chat session storage, or MySQL-only fallback
- Razorpay: sandbox/test keys only, or live keys available
- Expected scale of route pages (dozens vs thousands of city pairs) — affects SSG vs ISR vs pure SSR choice
- Multi-language SEO support, or single-language (English) for now
- Overall scope: portfolio/demo build (mocked data, smaller RAG corpus) vs production-oriented build

## Conventions for Claude When Working in This Repo

- **Never write seed data** for buses, routes, schedules, or trips — no `data.sql`, no Flyway seed migration, no hardcoded demo inventory. Flyway migrations define schema only. Any inventory in the running app must come from an operator using the Operator Dashboard (or a real API call an operator would make) — if you need data to test a flow, create it through the operator endpoints/UI, not by inserting rows directly
- It's fine (and expected) for the reference/config data that *isn't* inventory to exist without an operator action — e.g. a fixed list of cities for autocomplete, or role enums — but never buses/routes/schedules/trip_instances/seats-with-status
- Never expose JPA entities directly in API responses — always map to DTOs
- Every operator-scoped query (`buses`, `routes`, `schedules`, `trip_instances`, `bookings`, `analytics`) must filter by the authenticated operator's `operator_id` from the JWT — never let one operator read or aggregate another operator's data
- Treat `Route`/`Schedule`/`TripInstance` as three distinct entities, not one flattened table — this is what makes per-date seat inventory, price overrides, and single-trip cancellation possible
- Analytics/dashboard endpoints are read-only aggregations over `bookings`/`payments`/`trip_instances` — never a separately maintained "stats" table that could drift from real transactions
- All payment verification logic lives server-side; the frontend never determines payment success on its own
- LLM outputs (parsed queries, chatbot tool calls) are inputs to validated service methods, never raw DB queries
- Destructive actions (cancellations, payments, trip cancellations) require an explicit confirmation step before execution, and any user-facing cancellation must cascade to refunds and notifications
- New route/city landing pages must be added to the sitemap generation job, not just the search index
- Follow existing package/module structure (`controller/service/repository/entity/dto/config/exception/ai`) for any new backend code