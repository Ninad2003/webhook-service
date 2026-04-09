# Webhook Delivery Service

Hi! This is my implementation of the Webhook Delivery Service for the Full Stack Developer assessment. 

I approached this by building a highly reliable delivery system that ingests events from an upstream screening engine and ensures they are delivered to partner endpoints with strict ordering guarantees, idempotency handling, and a retry backoff engine.

Since the system isn't currently deployed to a public cloud, I've fully containerized the entire stack so you can run it perfectly and instantly on your local machine with a single command.

## Tech Stack
- **Backend API**: Spring Boot 3.x (Java 17), Spring Data JPA, Lombok
- **Database**: MySQL 8.0
- **Frontend Dashboard**: React 19, Vite (Simple & functional UI)
- **Infrastructure**: Docker & Docker Compose

## Quick Start (How to run it)

All you need is Docker Desktop installed.

```bash
docker-compose up --build
```

This single command spins up everything:
1. **MySQL Database** (mapped to port `3307` to avoid locking up your local 3306)
2. **Spring Boot API** on port `8080`
3. **React Dashboard** on port `3000`

## How to Test the System

To prove the delivery works exactly as designed, I recommend using a free testing endpoint like [webhook.site](https://webhook.site).

### 1. Register a Partner Endpoint
Copy your unique mock URL from `webhook.site` and register it.

**Via cURL:**
```bash
curl -X POST http://localhost:8080/api/partners \
  -H "Content-Type: application/json" \
  -d '{
    "partnerId": "demo-partner",
    "webhookUrl": "https://webhook.site/YOUR-UNIQUE-URL"
  }'
```

**Via Postman:**
1. Select **POST** method and enter `http://localhost:8080/api/partners`
2. Go to the **Headers** tab and add `Content-Type: application/json`
3. Go to the **Body** tab, select **raw** > **JSON**, and paste:
```json
{
  "partnerId": "demo-partner",
  "webhookUrl": "https://webhook.site/YOUR-UNIQUE-URL"
}
```

### 2. Ingest an Event
Act as the upstream screening engine and push an event into the system.

**Via cURL:**
```bash
curl -X POST http://localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId": "TXN-12345",
    "partnerId": "demo-partner",
    "eventType": "TXN_SCREENED",
    "payload": "{\"userId\":\"user_789\"}"
  }'
```

**Via Postman:**
1. Select **POST** method and enter `http://localhost:8080/api/events`
2. Go to the **Headers** tab and add `Content-Type: application/json`
3. Go to the **Body** tab, select **raw** > **JSON**, and paste:
```json
{
  "transactionId": "TXN-12345",
  "partnerId": "demo-partner",
  "eventType": "TXN_SCREENED",
  "payload": "{\"userId\":\"user_789\"}"
}
```

### 3. Watch it work
- **Check your `webhook.site` page.** The POST request and payload will appear almost instantly.
- **Open the React Dashboard** at [http://localhost:3000](http://localhost:3000). You'll see the event tracked in real-time, showing its delivery timeline, retry status, and HTTP logs.

## Testing the Edge Cases (Retries, Ordering & Reliability)

- **Test Retry Backoff**: Register a partner with a broken URL (e.g., `http://localhost:9999/fail`). Ingest an event and check the React dashboard. You'll see it actively fail and schedule a retry according to the backoff strategy (e.g., 10s, 30s, 2m).
- **Test Strict Ordering**: While that first event is stuck in the retry loop you just created, ingest a second event for the *same* `partnerId`. The system will safely queue the second event and completely refuse to attempt delivery until the first one succeeds or fully fails (this guarantees strict sequential delivery per partner).
- **Test Idempotency (Duplicate Events)**: Send the exact same event JSON payload (same `transactionId`, `partnerId`, and `eventType`) multiple times in a row. The system detects the duplicate via an idempotency hash and processes it only once, ignoring the redundant ingestions but returning a success response.
- **Test Payload Validation**: Attempt to ingest an event missing required fields like `transactionId` or test sending an empty string. The API will safely reject it and return a `400 Bad Request` detailing the exact validation errors.
- **Test Business State Validation**: Try to ingest an event with `eventType: "TXN_RELEASED"` for a new transaction ID. The API enforces strict domain logic and will reject the event with a `400 Bad Request` because a transaction cannot be released unless a `TXN_BLOCKED` event was previously ingested for it.

## Project Structure
- `/backend` - The Spring Boot application (API controllers, models, delivery engine).
- `/frontend` - The React dashboard UI.
- `docker-compose.yml` - The orchestration setup.
- `DESIGN.md` - Complete architectural write-up covering idempotency, scaling, and the data model logic.
