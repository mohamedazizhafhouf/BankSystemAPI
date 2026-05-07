# Mely — agent handoff (manual halal screening)

## Repositories

- **Backend:** `D:\tekup\ing 4\sem2\projet dev\project\api\BankSystemAPI` — Spring Boot, JWT, PostgreSQL.
- **Frontend:** `D:\tekup\ing 4\sem2\projet dev\project\webFinal\sakai-ng` — Angular, PrimeNG (Sakai).

## Zakat calculator (MVP)

- **Stateless API** under `/api/v1/zakat` (no DB). **Public** (`SecurityConfig`: `permitAll` for `/api/v1/zakat/**`).
- **`GET /api/v1/zakat/defaults`** — gold/silver niṣāb gram constants, default gold/silver price per gram, configured Zakat rate (`mely.zakat.*` in `application.properties`).
- **`POST /api/v1/zakat/calculate`** — body: non-negative asset/debt/metal fields, optional `goldPricePerGram` / `silverPricePerGram` overrides, `hawlAcknowledged` boolean.
- **Niṣāb (MVP):** threshold = `goldNisabGrams × goldPricePerGram` (request override or default). Silver niṣāb grams are returned in defaults for reference only; the due logic uses gold-based niṣāb.
- **Amounts:** `hypotheticalZakat` = 2.5% of positive net zakatable wealth. **`zakatDue`** is **0** unless `hawlAcknowledged && meetsNisab`; otherwise `zakatDue` equals `hypotheticalZakat` when net ≥ niṣāb.
- **Disclaimer:** response includes fixed educational disclaimer; UI repeats prominent “not a fatwa” copy.
- **Frontend:** `sakai-ng` route `/zakat` runs the same math **in the browser** (`computeZakatClient` + `ZAKAT_BUILTIN_DEFAULTS`) for instant results; defaults should match `mely.zakat.*`. `ZakatService` remains available for direct API use if needed.

## Stocks feature (current scope)

- **Manual screening only:** JSON `POST /api/v1/stocks/check` with company context + five core financial metrics. Processing is synchronous; response is `COMPLETED` with verdict, explanation, rules, tatheer fields.
- **Frontend (sakai-ng):** the new-screening wizard runs the **same rules client-side** (`src/app/core/halal-stocks/stock-screening.client.ts`) and stores the payload under `sessionStorage` for `local-*` result URLs—no submit spinner and no polling on the result page. **History** still uses the API for persisted server-side screenings. Keep `DEFAULT_RULE_PROFILE` aligned with `RuleProfileSeeder` in the API.
- **No LLM / chat / automatic PDF path** in the codebase: conversation entities and chat DTOs were removed. `StockScreeningMode.AUTOMATIC_LLM` may still appear on legacy DB rows.
- **API surface:** `GET /api/v1/stocks/check/{id}`, `GET .../explanation`, `GET .../history`.

## Metals / gold-api.com (spot + chart)

- **Proxy** under `/api/v1/metals` (public in demo `SecurityConfig`). UI must **not** embed upstream tokens.
- **`GET /api/v1/metals/spot?currency=USD`** — current XAU + XAG from `https://api.gold-api.com/price/{XAU|XAG}/{currency}`: `pricePerTroyOunce`, **`priceGram24k`** (per gram), **`pricePerKilogram`**, `timestamp` (from `updatedAt`).
- **`GET /api/v1/metals/history-chart?currency=USD&days=10`** — per-gram series when **`GOLD_API_KEY`** / **`MELY_GOLD_API_KEY`** or **`mely.goldapi.api-key`** is set (e.g. gitignored **`application-local.properties`** with **`--spring.profiles.active=local`** — see **`application-local.properties.example`**); upstream **`GET /history`** with query **`symbol`**, **`startTimestamp`**, **`endTimestamp`** (Unix seconds), **`groupBy=day`**, **`aggregation=avg`**, **`orderBy=asc`**, and HTTP header **`x-api-key`**. (Official history API does not take a currency parameter; series may be in the provider’s default unit, often USD per troy oz, then converted to per gram.) Without a key the response returns empty series and a **`note`**. Capped by `mely.goldapi.max-history-days`; optional `mely.goldapi.request-delay-ms` between the gold and silver history calls.
- **Frontend:** sakai-ng route `/gold-prices` calls the backend `MetalsApiService` (not gold-api.com directly).

## Local backend

- Default port **8090** (`application.properties`).
- PostgreSQL URL/credentials in `src/main/resources/application.properties`.
- Run tests: Maven wrapper `mvnw.cmd test` (requires JDK 23).

## Frontend

- `StockScreeningService` calls `http://localhost:8090/api/v1/stocks/check` (consider `environment.ts` for deployments).
- Wizard: `/halal-stocks/new` → submit → `/halal-stocks/result/:requestId`.
