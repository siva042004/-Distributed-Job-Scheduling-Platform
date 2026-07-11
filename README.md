# ⚙️ Distributed Job Scheduling Platform

**A production-style, horizontally-scalable job orchestration system** — built to demonstrate real distributed-systems patterns (event-driven processing, retry/backoff, concurrent workers, live observability) rather than a toy CRUD app.

`Java 17` · `Spring Boot 3` · `Apache Kafka` · `React` · `WebSocket (STOMP)` · `PostgreSQL` · `Docker` · `Kubernetes`

---

## Why this project

Most fresher portfolios have a CRUD app with a database. This one simulates how job processing actually works at scale: jobs are created via REST, handed off to **Kafka** for asynchronous distribution, picked up by a **concurrent worker pool**, retried with **exponential backoff** on transient failure, and their status is **pushed live** to a React dashboard over WebSocket — with failure alerts and throughput metrics streamed in real time.

It's designed to be talked through in an interview: every moving part (producer, consumer, retry policy, broadcast layer, autoscaling) maps to a specific distributed-systems concept.

---

## Architecture

```
┌─────────────────┐        REST         ┌──────────────────┐
│  React Dashboard │ ──────────────────▶ │  Spring Boot API  │
│  (live updates)  │ ◀──── WebSocket ─── │                    │
└─────────────────┘      (STOMP)         └─────────┬──────────┘
                                                      │ publishes
                                                      ▼
                                          ┌───────────────────────┐
                                          │   Kafka: job-events     │
                                          └───────────┬─────────────┘
                                                      │ consumed by
                                     ┌────────────────┼────────────────┐
                                     ▼                ▼                ▼
                              worker-1          worker-2          worker-3
                          (concurrent listener threads, retry + backoff)
                                     │                │                │
                                     └────────────────┼────────────────┘
                                                      ▼
                                          ┌───────────────────────┐
                                          │  PostgreSQL / H2         │
                                          │  (job state, audit)      │
                                          └───────────────────────┘
```

Runs as 3 auto-scaling backend replicas in Kubernetes (HPA: 3→10 pods on CPU load), each running its own Kafka consumer group members — so "concurrent workers" happens at two levels: threads within a pod, and pods across the cluster.

---

## Key Engineering Highlights

| Area | What was implemented |
|---|---|
| **Event-driven architecture** | Kafka decouples job creation from execution — the API stays fast under load even if processing is backed up |
| **Fault tolerance** | Retry with exponential backoff (`@Retryable`, 3 attempts, 1s→2s→4s), failed jobs marked terminal and alerted rather than retried forever |
| **Concurrency** | Kafka consumer concurrency = 3, tested under simultaneous multi-threaded load to confirm no state corruption |
| **Real-time observability** | WebSocket (STOMP/SockJS) pushes job status, failure alerts, and aggregate metrics to the UI with zero polling |
| **Horizontal scalability** | Kubernetes HPA scales backend 3→10 replicas on CPU utilization; readiness/liveness probes via Actuator |
| **Monitoring** | Spring Actuator + Micrometer Prometheus endpoint for ops-grade metrics scraping |
| **Testing** | Unit tests specifically targeting retry-exhaustion and concurrent-processing correctness, not just happy-path CRUD |

---

## Tech Stack

**Backend:** Java 17, Spring Boot 3, Spring Kafka, Spring Data JPA, Spring Retry, Spring WebSocket, PostgreSQL, H2 (dev)
**Frontend:** React 18, STOMP.js, SockJS, Axios
**Infra:** Docker, Docker Compose, Kubernetes (Deployments, Services, HPA), Nginx
**Messaging:** Apache Kafka + Zookeeper

---

## Quick Start

```bash
git clone <your-repo-url>
cd job-scheduler-platform/docker
docker compose up --build
```

- Dashboard → `http://localhost:3000`
- API → `http://localhost:8080/api/jobs`
- Health → `http://localhost:8080/actuator/health`

Full setup, Kubernetes deployment steps, and API reference are in [`SETUP.md`](./SETUP.md).

---

## Screenshots

> _Add 2–3 screenshots or a short screen recording of the dashboard here — this is the single highest-impact addition for recruiters skimming GitHub. A live job table mid-processing, the failure alert panel firing, and the metrics cards updating in real time all make great shots._

---

## What I'd improve with more time

Being upfront about scope, the way I'd want to be in an interview:
- Dead-letter queue for jobs that exhaust retries, instead of just marking them `FAILED` in place
- Idempotency keys to guard against duplicate processing on consumer rebalance
- Persisted metrics history (currently in-memory/live-only) for trend charts over time
- Auth (JWT) on the API — currently open for local demo purposes

---

## Author

**Sivaragul M** — Full Stack Java Developer
[GitHub](https://github.com/siva042004) · [LinkedIn](https://linkedin.com/in/sivaragul-m-4496682ba) · [Portfolio](https://siva042004.github.io/Sivaragul-portfolio/) · msan6848@gmail.com
