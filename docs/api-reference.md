# Mely API Reference

This file is the living API reference for the backend currently exposed by this project.

When we add new endpoints later, we should extend this file instead of creating scattered API notes.

## Base URL

Local development example:

```text
http://localhost:8090
```

## Authentication Overview

- Public endpoints:
  - `POST /api/v1/system/provision`
  - `POST /api/v1/auth/register`
  - `POST /api/v1/auth/login`
- Protected endpoints:
  - `POST /api/v1/bank/employees`
  - `GET /api/v1/bank/employees`
  - `POST /api/v1/stocks/check`
  - `GET /api/v1/stocks/check/history`
  - `GET /api/v1/stocks/check/{requestId}`
  - `GET /api/v1/stocks/check/{requestId}/explanation`
  - `GET /api/v1/personal/coach/monthly-insights`
  - `POST /api/v1/personal/coach/chat`
  - `GET /api/v1/personal/coach/chat`

Protected endpoints require:

```http
Authorization: Bearer <jwt>
```

## Account Types And Roles

- `SAAS_USER`
  - normal Mely SaaS user
  - has no organization id
- `ADMIN`
  - bank admin
  - can create and list employees inside the bank organization
- `LOAN_OFFICER`
  - bank employee
- `SHARIA_AUDITOR`
  - bank employee

## Common Error Response

Most handled validation and business errors return this shape:

```json
{
  "message": "Human readable error message"
}
```

Examples:

- `400 Bad Request`
- `401 Unauthorized`
- `403 Forbidden`
- `422 Unprocessable Entity`

## API List

### 1. Provision Bank Workspace

`POST /api/v1/system/provision`

Creates a new bank workspace and its first bank admin account. This is the bootstrap endpoint for a bank tenant.

Auth:
- public endpoint
- requires the `X-Mely-System-Key` header

Headers:

```http
Content-Type: application/json
X-Mely-System-Key: your-system-secret
```

Request example:

```json
{
  "firstName": "Mohamed",
  "lastName": "Hafhouf",
  "roleType": "ADMIN",
  "bankName": "Zitouna Bank",
  "adminEmail": "admin@zitouna.tn",
  "tempPassword": "Password123!"
}
```

Success response example:

```text
Bank Workspace 'Zitouna Bank' created successfully.
```

Failure response example:

```text
Invalid System Key
```

Notes:
- Use this once per new bank tenant.
- The created admin can then log in using `/api/v1/auth/login`.

### 2. Register SaaS User

`POST /api/v1/auth/register`

Creates a normal Mely SaaS user account. This flow is intentionally simple for now: no OTP and no email verification.

Auth:
- public endpoint

Headers:

```http
Content-Type: application/json
```

Request example:

```json
{
  "firstName": "Mely",
  "lastName": "User",
  "email": "saas.user@mely.app",
  "password": "Password123!"
}
```

Success response example:

```json
{
  "message": "Registration successful",
  "userId": "f4fdddf9-f981-4bb0-b89e-bf10a1d44d4b",
  "email": "saas.user@mely.app",
  "sessionId": null
}
```

Validation error example:

```json
{
  "message": "Password must be at least 8 characters"
}
```

Notes:
- Email is normalized to lowercase.
- Duplicate email registration is rejected.

### 3. Login

`POST /api/v1/auth/login`

Shared login endpoint for:
- SaaS users
- bank admins
- bank employees

Auth:
- public endpoint

Headers:

```http
Content-Type: application/json
```

Request example:

```json
{
  "email": "admin@zitouna.tn",
  "password": "Password123!"
}
```

Success response example for a bank user:

```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9.example",
  "tokenType": "Bearer",
  "email": "admin@zitouna.tn",
  "role": "ADMIN",
  "organizationId": "0a2d3941-b676-4d2e-b1bb-f9b55d289f14",
  "accountType": "BANK",
  "message": "Login successful"
}
```

Success response example for a SaaS user:

```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9.example",
  "tokenType": "Bearer",
  "email": "saas.user@mely.app",
  "role": "SAAS_USER",
  "organizationId": null,
  "accountType": "SAAS",
  "message": "Login successful"
}
```

Failure response example:

```json
{
  "message": "Invalid credentials"
}
```

Notes:
- Save the returned JWT and send it as `Authorization: Bearer <token>`.
- Bank users receive an `organizationId`.
- SaaS users do not receive an `organizationId`.

### 4. Create Bank Employee

`POST /api/v1/bank/employees`

Creates a new employee account inside the authenticated bank admin's organization.

Auth:
- protected endpoint
- allowed role: `ADMIN` only

Headers:

```http
Authorization: Bearer <admin-jwt>
Content-Type: application/json
```

Request example:

```json
{
  "firstName": "Loan",
  "lastName": "Officer",
  "email": "loan.officer@zitouna.tn",
  "password": "Password123!",
  "role": "LOAN_OFFICER"
}
```

Success response example:

```json
{
  "id": "6d16028f-43e2-4d19-bfe2-631c517f18db",
  "firstName": "Loan",
  "lastName": "Officer",
  "email": "loan.officer@zitouna.tn",
  "role": "LOAN_OFFICER"
}
```

Failure response example:

```json
{
  "message": "Employee creation cannot assign the ADMIN role in v1."
}
```

Notes:
- Allowed employee roles in v1:
  - `LOAN_OFFICER`
  - `SHARIA_AUDITOR`
- `SAAS_USER` and `ADMIN` are rejected for this endpoint.
- The employee can log in immediately through `/api/v1/auth/login`.

### 5. List Bank Employees

`GET /api/v1/bank/employees`

Returns employees for the authenticated bank admin's organization.

Auth:
- protected endpoint
- allowed role: `ADMIN` only

Headers:

```http
Authorization: Bearer <admin-jwt>
```

Success response example:

```json
[
  {
    "id": "1a0cfbd5-a915-43b4-ac61-bcb332b33f32",
    "firstName": "Admin",
    "lastName": "User",
    "email": "admin@zitouna.tn",
    "role": "ADMIN"
  },
  {
    "id": "6d16028f-43e2-4d19-bfe2-631c517f18db",
    "firstName": "Loan",
    "lastName": "Officer",
    "email": "loan.officer@zitouna.tn",
    "role": "LOAN_OFFICER"
  }
]
```

Forbidden response example:

```json
{
  "message": "Access denied"
}
```

Notes:
- This endpoint is useful for frontend employee management screens.
- The list is scoped to the admin's organization only.

### 6. Submit Halal Stock Check

`POST /api/v1/stocks/check`

Creates a halal stock screening request. It supports both:
- manual synchronous screening
- asynchronous processing with uploaded PDF files

Auth:
- protected endpoint
- allowed roles:
  - `ADMIN`
  - `LOAN_OFFICER`
  - `SHARIA_AUDITOR`
  - `SAAS_USER`

Content type:

```http
multipart/form-data
```

Form fields:

- required:
  - `companyName`
  - `countryOrMarket`
  - `currency`
  - `businessDescription`
- optional context:
  - `ticker`
  - `sector`
  - `fiscalYear`
  - `reportingPeriod`
  - `mode` as `SYNC` or `ASYNC`
- optional manual metrics:
  - `interestBearingDebt`
  - `cashAndInterestBearingAssets`
  - `totalAssetsOrMarketCap`
  - `totalRevenue`
  - `nonCompliantIncome`
  - `sharesOwned`
  - `investedAmount`
- optional files:
  - `documents` repeated PDF files

Behavior:

- `SYNC`
  - allowed only when all core ratio inputs are sent manually
  - does not allow uploaded documents
- `ASYNC`
  - used when PDFs are uploaded
  - also allowed without files when the frontend prefers polling

Request example for sync/manual mode:

```bash
curl -X POST "http://localhost:8090/api/v1/stocks/check" \
  -H "Authorization: Bearer <jwt>" \
  -F "mode=SYNC" \
  -F "companyName=Sukuk Tech" \
  -F "ticker=SKTK" \
  -F "countryOrMarket=Tunisia" \
  -F "currency=TND" \
  -F "businessDescription=Core software infrastructure for financial institutions" \
  -F "sector=Technology" \
  -F "interestBearingDebt=20" \
  -F "cashAndInterestBearingAssets=10" \
  -F "totalAssetsOrMarketCap=100" \
  -F "totalRevenue=100" \
  -F "nonCompliantIncome=2" \
  -F "investedAmount=1000"
```

Success response example for sync/manual mode:

```json
{
  "requestId": "4b6b1d4b-46ae-4bc7-8d17-4fe17e5c1843",
  "status": "COMPLETED",
  "verdict": "HALAL",
  "needsTatheer": true,
  "tatheerAmount": 20.000000,
  "purificationRatio": 0.020000,
  "purificationPerShare": null,
  "ruleResults": [
    {
      "ruleCode": "activity-screen",
      "label": "Business activity screening",
      "status": "PASS",
      "passed": true,
      "message": "No prohibited business activity keywords were detected in the submitted company profile.",
      "details": null
    }
  ],
  "metricsUsed": {
    "interestBearingDebt": {
      "value": 20,
      "source": "manual",
      "sourceDocument": null,
      "pageNumber": null,
      "snippet": null
    }
  },
  "warnings": [],
  "userMessage": "Sukuk Tech passed the current Sharia screening profile. No prohibited business activity keywords were detected in the submitted company profile.",
  "createdAt": "2026-05-04T19:10:32.189169",
  "completedAt": "2026-05-04T19:10:32.384102"
}
```

Request example for async/PDF mode:

```bash
curl -X POST "http://localhost:8090/api/v1/stocks/check" \
  -H "Authorization: Bearer <jwt>" \
  -F "mode=ASYNC" \
  -F "companyName=Async Capital" \
  -F "countryOrMarket=Tunisia" \
  -F "currency=TND" \
  -F "businessDescription=Digital payments software" \
  -F "interestBearingDebt=20" \
  -F "cashAndInterestBearingAssets=15" \
  -F "totalAssetsOrMarketCap=100" \
  -F "totalRevenue=100" \
  -F "nonCompliantIncome=1" \
  -F "documents=@C:/temp/company-financials.pdf;type=application/pdf"
```

Accepted response example for async mode:

```json
{
  "requestId": "9f3167cf-a2a5-464f-a248-a34877b6d6a3",
  "status": "PROCESSING",
  "verdict": null,
  "needsTatheer": null,
  "tatheerAmount": null,
  "purificationRatio": null,
  "purificationPerShare": null,
  "ruleResults": null,
  "metricsUsed": null,
  "warnings": [],
  "userMessage": null,
  "createdAt": "2026-05-04T19:12:01.221011",
  "completedAt": null
}
```

Validation error example:

```json
{
  "message": "SYNC mode requires manual financial inputs for all core ratios and does not support uploaded documents."
}
```

Notes:
- Only PDF uploads are supported in v1.
- Manual values override extracted values.
- The current file-analysis pipeline supports uploads and async processing, but real LLM extraction is not plugged in yet.
- Bank users can access their organization's requests.
- SaaS users can access only the requests they created.

### 7. Get Stock Check Result

`GET /api/v1/stocks/check/{requestId}`

Returns the current state of a screening request. Use this endpoint to poll async jobs until they complete.

Auth:
- protected endpoint
- allowed roles:
  - `ADMIN`
  - `LOAN_OFFICER`
  - `SHARIA_AUDITOR`
  - `SAAS_USER`

Headers:

```http
Authorization: Bearer <jwt>
```

Success response example while processing:

```json
{
  "requestId": "9f3167cf-a2a5-464f-a248-a34877b6d6a3",
  "status": "PROCESSING",
  "verdict": null,
  "needsTatheer": null,
  "tatheerAmount": null,
  "purificationRatio": null,
  "purificationPerShare": null,
  "ruleResults": null,
  "metricsUsed": null,
  "warnings": [],
  "userMessage": null,
  "createdAt": "2026-05-04T19:12:01.221011",
  "completedAt": null
}
```

Success response example when completed:

```json
{
  "requestId": "9f3167cf-a2a5-464f-a248-a34877b6d6a3",
  "status": "COMPLETED",
  "verdict": "HALAL",
  "needsTatheer": true,
  "tatheerAmount": 10.000000,
  "purificationRatio": 0.010000,
  "purificationPerShare": null,
  "ruleResults": [
    {
      "ruleCode": "non-compliant-income",
      "label": "Non-compliant income ratio",
      "status": "PASS",
      "passed": true,
      "message": "The non-compliant income ratio is within the allowed threshold.",
      "details": {
        "threshold": 0.05,
        "actual": 0.01
      }
    }
  ],
  "metricsUsed": {},
  "warnings": [
    "Document extraction currently uses a placeholder implementation."
  ],
  "userMessage": "Async Capital passed the current Sharia screening profile.",
  "createdAt": "2026-05-04T19:12:01.221011",
  "completedAt": "2026-05-04T19:12:04.082194"
}
```

Not found or not owned example:

```json
{
  "message": "Stock screening request not found"
}
```

Notes:
- Poll every 1 to 3 seconds for async jobs.
- Use the returned payload directly in the frontend result page when `status` becomes `COMPLETED`.

### 8. Get Stock Check History

`GET /api/v1/stocks/check/history`

Returns the authenticated user's paginated stock screening history.

Auth:
- protected endpoint
- allowed roles:
  - `ADMIN`
  - `LOAN_OFFICER`
  - `SHARIA_AUDITOR`
  - `SAAS_USER`

Query params:
- `page` optional, default `0`
- `size` optional, default `10`
- `status` optional
- `verdict` optional
- `query` optional

Success response example:

```json
{
  "content": [
    {
      "requestId": "6eb65478-6118-48ee-bf16-79f598fe2ef1",
      "companyName": "Sukuk Tech",
      "ticker": "SKTK",
      "status": "COMPLETED",
      "verdict": "HALAL",
      "needsTatheer": true,
      "tatheerAmount": 20.000000,
      "purificationRatio": 0.020000,
      "userMessage": "Sukuk Tech passed the current Sharia screening profile.",
      "createdAt": "2026-05-04T19:10:32.189169",
      "completedAt": "2026-05-04T19:10:32.384102",
      "requestedMode": "SYNC"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1,
  "hasNext": false
}
```

Processing-item example:

```json
{
  "content": [
    {
      "requestId": "9f3167cf-a2a5-464f-a248-a34877b6d6a3",
      "companyName": "Async Capital",
      "ticker": null,
      "status": "PROCESSING",
      "verdict": null,
      "needsTatheer": null,
      "tatheerAmount": null,
      "purificationRatio": null,
      "userMessage": null,
      "createdAt": "2026-05-04T19:12:01.221011",
      "completedAt": null,
      "requestedMode": "ASYNC"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1,
  "hasNext": false
}
```

Notes:
- v1 history is "my screenings only"
- use it for the screening history page or recent activity widget
- click into each item with the existing detail endpoints

### 9. Get Stock Check Explanation

`GET /api/v1/stocks/check/{requestId}/explanation`

Returns a lighter explanation-focused response for showing the final reasoning in a separate tab or panel.

Auth:
- protected endpoint
- allowed roles:
  - `ADMIN`
  - `LOAN_OFFICER`
  - `SHARIA_AUDITOR`
  - `SAAS_USER`

Headers:

```http
Authorization: Bearer <jwt>
```

Success response example:

```json
{
  "requestId": "9f3167cf-a2a5-464f-a248-a34877b6d6a3",
  "status": "COMPLETED",
  "verdict": "HALAL",
  "explanation": "The company passed the business activity screen and stayed within the configured financial thresholds. A small purification amount is still required because a limited portion of revenue was non-compliant.",
  "userMessage": "Async Capital passed the current Sharia screening profile.",
  "warnings": [
    "Document extraction currently uses a placeholder implementation."
  ],
  "completedAt": "2026-05-04T19:12:04.082194"
}
```

Notes:
- Use this if your frontend wants a dedicated explanation drawer, tab, or modal.
- If the request is still processing, `verdict` and `explanation` can still be `null`.

### 10. Get AI Finance Coach Monthly Insights

`GET /api/v1/personal/coach/monthly-insights?yearMonth=YYYY-MM`

Returns cached or newly generated Islamic finance coach insights for the authenticated SaaS user. The coach uses the user's monthly cash flow, budgets, goals, zakat portfolio, hawl status, and recent halal stock screenings.

Auth:
- protected endpoint
- allowed account type: `SAAS` only

Headers:

```http
Authorization: Bearer <jwt>
```

Success response example:

```json
{
  "yearMonth": "2026-05",
  "summary": "Your cash flow is positive this month. Main focus: FOOD budget is close to cap.",
  "healthScore": 82,
  "cards": [
    {
      "type": "BUDGET_ALERT",
      "severity": "WARN",
      "title": "FOOD budget is close to cap",
      "message": "You used about 90% of this category's monthly budget.",
      "actionLabel": "Review budgets",
      "actionRoute": "/wealth"
    }
  ],
  "generatedAt": "2026-05-20T22:15:00"
}
```

Notes:
- Groq is the default provider when `MELY_GROQ_API_KEY` or `mely.ai.groq.api-key` is configured.
- If no LLM key is configured, the endpoint still returns deterministic guidance from the user's data.
- Results are cached per user and month.

### 11. Chat With AI Finance Coach

`POST /api/v1/personal/coach/chat`

Sends a user question to the Islamic finance coach. The reply is grounded in the user's Mely finance context and previous coach messages.

Auth:
- protected endpoint
- allowed account type: `SAAS` only

Request example:

```json
{
  "yearMonth": "2026-05",
  "message": "Am I ready for zakat?"
}
```

Success response example:

```json
{
  "assistantMessage": {
    "id": "8f5db5ab-c921-4791-9868-1a37b8361d9b",
    "role": "ASSISTANT",
    "content": "Based on your current Mely data for 2026-05, your financial health score is 78/100...",
    "yearMonth": "2026-05",
    "createdAt": "2026-05-20T22:16:20"
  }
}
```

### 12. Get AI Finance Coach Chat History

`GET /api/v1/personal/coach/chat`

Returns the authenticated SaaS user's coach conversation history.

Success response example:

```json
{
  "messages": [
    {
      "id": "7102e8b8-3b48-4ad8-b4ef-bb03e1d6e1fc",
      "role": "USER",
      "content": "Am I ready for zakat?",
      "yearMonth": "2026-05",
      "createdAt": "2026-05-20T22:16:18"
    }
  ]
}
```

## Frontend Usage Flow

### SaaS user flow

1. Register with `POST /api/v1/auth/register`
2. Login with `POST /api/v1/auth/login`
3. Save JWT
4. Call protected APIs with `Authorization: Bearer <jwt>`
5. Use `/api/v1/stocks/check` for manual halal stock screening

### Bank flow

1. Provision bank with `POST /api/v1/system/provision`
2. Login as bank admin with `POST /api/v1/auth/login`
3. Create employee accounts with `POST /api/v1/bank/employees`
4. Employees log in with `POST /api/v1/auth/login`
5. Admins and employees can use `/api/v1/stocks/check`

## Related Files

- Main API reference: [api-reference.md](/D:/tekup/ing%204/sem2/projet%20dev/project/api/BankSystemAPI/docs/api-reference.md)
- Stocks-focused reference: [halal-stocks-api.md](/D:/tekup/ing%204/sem2/projet%20dev/project/api/BankSystemAPI/docs/halal-stocks-api.md)
- Auth HTTP examples: [auth-test.http](/D:/tekup/ing%204/sem2/projet%20dev/project/api/BankSystemAPI/src/test/java/com/example/mely/auth/auth-test.http)
- Provisioning HTTP examples: [provisioning.http](/D:/tekup/ing%204/sem2/projet%20dev/project/api/BankSystemAPI/src/test/java/com/example/mely/provisioning.http)
- Stocks HTTP examples: [halal-stocks.http](/D:/tekup/ing%204/sem2/projet%20dev/project/api/BankSystemAPI/src/test/java/com/example/mely/stocks/halal-stocks.http)
