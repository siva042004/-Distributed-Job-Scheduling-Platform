# Distributed Job Scheduling Platform

A full-stack, distributed job scheduling and monitoring system built with Spring Boot, Kafka, React, WebSocket, Docker, and Kubernetes.

## Architecture

```
React Dashboard (WebSocket + REST)
        │
        ▼
Spring Boot API  ──publishes──▶  Kafka (job-events topic)
        │                                │
        │                     consumed by 3 concurrent
        │                     listener threads (worker pool)
        ▼                                ▼
   PostgreSQL / H2                Retry logic (@Retryable,
   (job state)                    exponential backoff)
        │                                │
        └──────────── WebSocket (STOMP) ─┘
              /topic/jobs, /topic/alerts, /topic/metrics
```

## Features

- **React dashboard**: live job table, metrics cards, failure alert feed, job creation form (monochrome, analyst-grade styling)
- **Spring Boot backend**: REST API for job CRUD, scheduled dispatcher, actuator health/metrics
- **Kafka**: `job-events` topic decouples job creation from execution; consumer group with concurrency=3 for parallel processing across partitions
- **Docker**: full docker-compose stack (Zookeeper, Kafka, Postgres, backend, frontend)
- **Kubernetes**: namespace, Postgres/Kafka/Zookeeper deployments, backend with 3 replicas + HPA (auto-scales 3→10 on CPU), frontend with LoadBalancer service, readiness/liveness probes
- **WebSocket**: real-time push of job status changes, failure alerts, and aggregate metrics (STOMP over SockJS)
- **Failure alerts**: any job that exhausts its retry budget triggers a broadcast to `/topic/alerts`
- **Performance monitoring**: `/actuator/prometheus` metrics endpoint + live in-app metrics panel (pending/running/retrying/completed/failed counts refreshed every 3s)
- **Retry & concurrency testing**: unit tests exercising the retry-exhaustion path and concurrent job processing (see Testing section)

## Project Structure

```
job-scheduler-platform/
├── backend/                   Spring Boot service
│   ├── src/main/java/com/scheduler/
│   │   ├── controller/        REST endpoints
│   │   ├── service/           JobService (scheduling, dispatch, metrics)
│   │   ├── model/              JPA entities
│   │   ├── repository/        Spring Data repositories
│   │   ├── kafka/              Producer + Consumer (retry logic lives here)
│   │   ├── websocket/          STOMP broadcast service
│   │   └── config/             Kafka, WebSocket, CORS config
│   └── src/test/java/...       Retry + concurrency + service unit tests
├── frontend/                   React dashboard
│   └── src/
│       ├── components/         MetricsPanel, JobTable, CreateJobForm, AlertsPanel
│       └── services/           REST client (axios), WebSocket client (stomp)
├── docker/docker-compose.yml   Full local stack
└── k8s/                        Kubernetes manifests (namespace, postgres, kafka, backend+HPA, frontend)
```

## Running Locally with Docker Compose

```bash
cd docker
docker compose up --build
```

- Backend: http://localhost:8080
- Frontend: http://localhost:3000
- H2/Postgres, Kafka, Zookeeper spin up automatically

## Running Backend Standalone (dev mode, in-memory H2, no Kafka required for API-only testing)

```bash
cd backend
mvn spring-boot:run
```

Note: Kafka must be reachable at `localhost:9092` for job dispatch/consumption to work end-to-end. For local Kafka without Docker Compose, run `docker run -p 9092:9092 ... confluentinc/cp-kafka` or use the compose file above.

## Running Frontend Standalone

```bash
cd frontend
npm install
npm start
```

Set `REACT_APP_API_URL` and `REACT_APP_WS_URL` env vars if the backend isn't on `localhost:8080`.

## Deploying to Kubernetes

```bash
# Build images first (or push to a registry and update image names in k8s/*.yaml)
docker build -t job-scheduler-backend:latest ./backend
docker build -t job-scheduler-frontend:latest ./frontend

kubectl apply -f k8s/00-namespace.yaml
kubectl apply -f k8s/01-postgres.yaml
kubectl apply -f k8s/02-kafka.yaml
kubectl apply -f k8s/03-backend.yaml
kubectl apply -f k8s/04-frontend.yaml

kubectl get pods -n job-scheduler
```

The backend Deployment runs 3 replicas by default and scales to 10 under CPU load via HorizontalPodAutoscaler — this is what gives the "distributed" processing its horizontal scale in a real cluster, on top of Kafka consumer concurrency within each pod.

## API Reference

| Method | Endpoint              | Description               |
|--------|------------------------|---------------------------|
| POST   | /api/jobs              | Create/schedule a job     |
| GET    | /api/jobs              | List all jobs             |
| GET    | /api/jobs/{id}         | Get job by id              |
| POST   | /api/jobs/{id}/cancel  | Cancel a pending job        |
| POST   | /api/jobs/{id}/retry   | Reset and re-dispatch a failed job |

## WebSocket Topics

- `/topic/jobs` — job status change events
- `/topic/alerts` — failure alerts (job exhausted retries)
- `/topic/metrics` — aggregate counts, pushed every 3 seconds

## Testing Retry & Concurrent Processing

Automated tests live in `backend/src/test/java/com/scheduler/`:

- **`JobEventConsumerRetryTest.jobExhaustsRetriesAndMovesToFailed`** — drives a job through repeated `processJobWithRetry` calls and asserts it reaches a terminal `FAILED`/`COMPLETED` state rather than looping indefinitely or leaving state inconsistent.
- **`JobEventConsumerRetryTest.concurrentProcessingDoesNotCorruptJobState`** — fires three concurrent threads at the same job id to check the retry/state-transition logic doesn't corrupt shared state under contention.
- **`JobServiceTest`** — covers job creation, cancellation, and retry-reset logic at the service layer.

Run all tests:

```bash
cd backend
mvn test
```

### Manual end-to-end retry test

1. Start the full stack via `docker compose up --build`.
2. Create a job from the dashboard — the `simulateWork` method in `JobEventConsumer` has a built-in ~30% random failure rate, so roughly 1 in 3 jobs will exercise the retry path organically.
3. Watch the job's `status` cycle `RUNNING → RETRYING → COMPLETED` (or `→ FAILED` after `maxRetries` is exhausted) live in the dashboard, and check the **Failure Alerts** panel for any job that fails permanently.
4. To stress concurrency, create 10+ jobs in quick succession and confirm in the logs (`assignedWorker` field / `worker-1`, `worker-2`, `worker-3`) that they're processed across multiple concurrent listener threads rather than serially.

## Notes on Configuration

- Default profile uses in-memory H2 for quick local runs without Postgres.
- `docker` Spring profile (`application-docker.yml`) switches to Postgres and points Kafka at the `kafka` service name for use in Docker Compose / Kubernetes.
- Retry policy: 3 attempts by default per job (configurable per-job via `maxRetries` in the create request), exponential backoff starting at 1s and doubling each attempt.
