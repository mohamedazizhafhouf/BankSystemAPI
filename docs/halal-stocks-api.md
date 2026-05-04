# Halal Stocks API

Base path: `/api/v1/stocks/check`

Auth:
- Every endpoint requires `Authorization: Bearer <jwt>`
- Bank users authenticate with a bank-scoped JWT that contains their organization id
- SaaS users authenticate with a SaaS JWT that has no organization id and can access only their own stock checks

## Endpoints

### `POST /api/v1/stocks/check`
Creates a new halal stock screening request.

Content type:
- `multipart/form-data`

Fields:
- `companyName` required
- `countryOrMarket` required
- `currency` required
- `businessDescription` required
- `ticker` optional
- `sector` optional
- `fiscalYear` optional
- `reportingPeriod` optional
- `mode` optional: `SYNC` or `ASYNC`
- `interestBearingDebt` optional decimal
- `cashAndInterestBearingAssets` optional decimal
- `totalAssetsOrMarketCap` optional decimal
- `totalRevenue` optional decimal
- `nonCompliantIncome` optional decimal
- `sharesOwned` optional decimal
- `investedAmount` optional decimal
- `documents` optional, repeatable PDF files

Behavior:
- `SYNC` is allowed only when all core financial inputs are sent manually and no files are uploaded
- Any uploaded PDF forces async processing
- `ASYNC` can also be used without files when the frontend wants a job/polling flow

Response:
- `200 OK` when the request completes synchronously
- `202 Accepted` when the request is queued/processing asynchronously

Example sync response:

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

Example async acceptance response:

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

### `GET /api/v1/stocks/check/{requestId}`
Returns the current processing state and the final result when ready.

Use this endpoint for polling after an async submission.

Example polling flow:
1. Frontend submits multipart form
2. Backend returns `202` with `requestId`
3. Frontend polls every 1-3 seconds
4. When `status` becomes `COMPLETED`, use the returned payload directly in the UI

### `GET /api/v1/stocks/check/history`
Returns the authenticated user's stock screening history.

Query params:
- `page` optional, default `0`
- `size` optional, default `10`
- `status` optional: `SUBMITTED`, `PROCESSING`, `COMPLETED`, `FAILED`
- `verdict` optional: `HALAL`, `NON_HALAL`, `REVIEW_REQUIRED`
- `query` optional: case-insensitive company/ticker search

Behavior:
- v1 history is "my screenings only"
- the endpoint returns pending, completed, and failed requests
- use it for a table view, cards view, or recent activity section

Example response:

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

Example failed-item snippet:

```json
{
  "content": [
    {
      "requestId": "4beebf3a-fede-4e12-826f-b6fa231684e0",
      "companyName": "Broken Extract",
      "ticker": null,
      "status": "FAILED",
      "verdict": null,
      "needsTatheer": null,
      "tatheerAmount": null,
      "purificationRatio": null,
      "userMessage": "Processing failed: Simulated extraction failure",
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

### `GET /api/v1/stocks/check/{requestId}/explanation`
Returns the explanation-focused view:
- `requestId`
- `status`
- `verdict`
- `explanation`
- `userMessage`
- `warnings`
- `completedAt`

Use this if the frontend wants a dedicated explanation tab/panel without re-reading the full result object.

## Frontend Rendering Notes

- `status`
  - `PROCESSING`: show a spinner/progress state
  - `COMPLETED`: render verdict/results
  - `FAILED`: render an error state using `warnings`

- `verdict`
  - `HALAL`: green/approved state
  - `NON_HALAL`: red/rejected state
  - `REVIEW_REQUIRED`: amber/manual-review state

- `history`
  - use it for the screening history page
  - each item already contains the summary fields needed for cards/table UI
  - navigate from a history item into `GET /api/v1/stocks/check/{requestId}` or `/explanation`

- `ruleResults`
  - render as a checklist/timeline
  - each item already contains `label`, `status`, `message`, and structured `details`

- `metricsUsed`
  - each metric includes `value` and `source`
  - if `source=manual`, the user supplied the number
  - if `source=extracted`, the number came from document analysis

- `warnings`
  - render as non-blocking banners/messages
  - v1 may include a warning that PDF extraction still uses a placeholder adapter

- `userMessage`
  - use as the short summary shown near the verdict

- `explanation`
  - use as the long-form detailed reasoning view

## Important v1 Notes

- Uploaded files must be PDFs
- Manual values always override extracted values
- If required financial inputs are still missing after processing, the verdict becomes `REVIEW_REQUIRED`
- Tatheer is computed precisely when enough holdings context exists
- If holdings are missing, the API still returns purification guidance through `purificationRatio`
- Supported roles for this API are `ADMIN`, `LOAN_OFFICER`, `SHARIA_AUDITOR`, and `SAAS_USER`
- Bank users can retrieve stock requests for their organization; SaaS users can retrieve only the requests they created
- The history endpoint returns only the screenings created by the authenticated user in v1
