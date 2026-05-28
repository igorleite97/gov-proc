# GovProc

🌐 [Leia em Português](README.pt-br.md) | **English**

> A backend platform for managing government procurement processes — from capture to contract — built as a portfolio project demonstrating Domain-Driven Design, state machine patterns, and operational traceability with Java 21 and Spring Boot 3.

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Process State Machine](#process-state-machine)
- [Tech Stack](#tech-stack)
- [Critical Domain Rules](#critical-domain-rules)
- [Module Status](#module-status)
- [Getting Started](#getting-started)
- [API Endpoints](#api-endpoints)
- [Technical Decisions](#technical-decisions)
- [Roadmap](#roadmap)
- [License](#license)

---

## Overview

GovProc models the complete lifecycle of a Brazilian public procurement process (_licitação_). The system tracks every state transition from the moment a bid is captured through analysis, quotation, dispute, and contract activation — with full audit trail and operational timeline.

**What this project demonstrates:**

- Domain state machines encapsulated inside entities (no public status setters)
- Hard separation between **cost** (Quotation) and **strategy** (Dispute)
- Dual traceability: `ProcessTimelineEvent` for operational events, `AuditLog` for field-level changes
- `BigDecimal` with `NUMERIC(19,4)` precision throughout — no `double` for money
- Modular monolith with 8 bounded-context packages, no unnecessary abstraction layers

---

## Architecture

```
com.govproc
├── auth/           JWT authentication, roles (ADMIN, MANAGER, ANALYST, VIEWER)
├── process/        Core procurement entity + 11-state machine
├── analysis/       Operational viability analysis (1:1 per process)
├── quotation/      Supplier cost records — NO markup, NO margin
├── supplier/       Independent supplier registry
├── timeline/       Immutable operational event log
├── audit/          Immutable field-level change log
└── shared/         BaseEntity, ApiResponse, exceptions, global handler
```

**Key structural choices:**
- Package-by-feature, not by layer
- No interface/implementation split without real contracts
- No `ApplicationEventPublisher` — service orchestration is explicit and readable
- `supplierId` stored as UUID in `Quotation` — no `@ManyToOne`, no cascade risk

---

## Process State Machine

Every state transition is a domain method on `ProcurementProcess` with internal validation. No public status setter exists.

```
CAPTURED
   │
   └──[startAnalysis()]──► UNDER_ANALYSIS
                                │
              ┌─────────────────┴─────────────────┐
              │                                   │
   [approveAnalysis()]                  [rejectAnalysis(reason)]
              │                                   │
       ANALYSIS_APPROVED               ANALYSIS_REJECTED
              │
   [startQuotation()]
              │
         IN_QUOTATION
              │
   [markAsQuoted()]  ← requires selected quotation
              │
           QUOTED
              │
   [startDispute()]
              │
          IN_DISPUTE
              │
       ┌──────┴──────┐
       │             │
  [markAsWinner()] [markAsLoser()]
       │             │
    WINNER         LOSER
       │
  [activateContract()]
       │
  CONTRACT_ACTIVE
       │
   [close()]
       │
    CLOSED
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3.5 |
| Security | Spring Security + JWT (jjwt 0.12.6) |
| Persistence | Spring Data JPA + Hibernate |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| Validation | Jakarta Bean Validation |
| Documentation | SpringDoc OpenAPI 3 / Swagger UI |
| Testing | JUnit 5 + Mockito |
| Build | Maven |
| Container | Docker + Docker Compose |

---

## Critical Domain Rules

### 1. Cost ≠ Strategy

`Quotation` represents **cost only**. It contains:
- Supplier, manufacturer, brand
- Unit cost, shipping cost, total cost
- Technical notes, delivery days, quantity

It does **NOT** contain markup, margin, pricing strategy, or bid aggressiveness. Those belong to **Dispute** — a completely separate bounded context.

This is a deliberate, non-negotiable domain decision.

### 2. `selected` is not a delete

Multiple quotations can exist per process. When one is selected (`selected = true`), the others are **deselected, not deleted**. Full quotation history is preserved for compliance and operational traceability.

### 3. `BigDecimal` always

All monetary values use `BigDecimal` with `NUMERIC(19,4)` precision. `double` and `float` are forbidden for money, cost, margin, or price.

### 4. No Cascade ALL on Supplier

`Supplier` has its own lifecycle. `Quotation.supplierId` is stored as a plain `UUID` — no `@ManyToOne`, no cascade. A supplier must not be deleted because a quotation was removed.

---

## Module Status

| Module | Status | Migrations | Tests |
|---|---|---|---|
| `shared/` | ✅ Complete | — | — |
| `auth/` | ✅ Complete | V1 | 6 |
| `process/` | ✅ Complete | V2 | — |
| `timeline/` | ✅ Complete | V3 | — |
| `analysis/` | ✅ Complete | V4 | 6 |
| `audit/` | ✅ Complete | V5 | — |
| `supplier/` | ✅ Complete | V6 | 2 |
| `quotation/` | ✅ Complete | V7 | 5 |
| `dispute/` | 🔲 Pending | — | — |
| `postbid/` | 🔲 Pending | — | — |
| `contract/` | 🔲 Pending | — | — |

**Totals:** 62 classes · 19 tests · 7 Flyway migrations

---

## Getting Started

### Prerequisites

- Java 21
- Docker + Docker Compose
- Maven (or use the included wrapper)

### 1. Start the database

```bash
docker compose up -d
```

### 2. Run the application

```bash
./mvnw spring-boot:run
```

On Windows:
```bash
.\mvnw.cmd spring-boot:run
```

> If `JAVA_HOME` is not configured globally, set it before running:
> ```powershell
> $env:JAVA_HOME = "C:\path\to\jdk-21"
> ```

### 3. Access the API

| URL | Description |
|---|---|
| `http://localhost:8080/swagger-ui.html` | Interactive API documentation |
| `http://localhost:8080/v3/api-docs` | Raw OpenAPI spec |

### 4. Authenticate

```bash
# Register
POST /auth/register
{
  "name": "Igor Andrade",
  "email": "igor@govproc.dev",
  "password": "password123"
}

# Login — returns JWT token
POST /auth/login
{
  "email": "igor@govproc.dev",
  "password": "password123"
}
```

Use the returned token as `Authorization: Bearer <token>` in all subsequent requests.

---

## API Endpoints

### Auth
| Method | Path | Description |
|---|---|---|
| `POST` | `/auth/register` | Register new user (role: ANALYST) |
| `POST` | `/auth/login` | Authenticate and receive JWT |

### Processes
| Method | Path | Description |
|---|---|---|
| `POST` | `/processes` | Capture new procurement process |
| `GET` | `/processes` | List all processes |
| `GET` | `/processes/{id}` | Get process by ID |
| `GET` | `/processes/{id}/timeline` | Operational event history |
| `GET` | `/processes/{id}/audit` | Field-level change log |

### Analysis
| Method | Path | Description |
|---|---|---|
| `POST` | `/processes/{id}/analysis/start` | Start analysis → `UNDER_ANALYSIS` |
| `POST` | `/processes/{id}/analysis/approve` | Approve → `ANALYSIS_APPROVED` |
| `POST` | `/processes/{id}/analysis/reject` | Reject → `ANALYSIS_REJECTED` |
| `GET` | `/processes/{id}/analysis` | Get analysis record |

### Quotation
| Method | Path | Description |
|---|---|---|
| `POST` | `/processes/{id}/quotation/start` | Start quotation phase → `IN_QUOTATION` |
| `POST` | `/processes/{id}/quotations` | Add supplier cost quotation |
| `GET` | `/processes/{id}/quotations` | List all quotations (ordered by total cost) |
| `PUT` | `/processes/{id}/quotations/{qid}/select` | Select winning quotation |
| `POST` | `/processes/{id}/quotation/mark-quoted` | Mark as quoted → `QUOTED` |

### Suppliers
| Method | Path | Description |
|---|---|---|
| `POST` | `/suppliers` | Register supplier |
| `GET` | `/suppliers` | List active suppliers |
| `GET` | `/suppliers/{id}` | Get supplier by ID |
| `DELETE` | `/suppliers/{id}` | Deactivate supplier (logical delete) |

---

## Technical Decisions

### No interface/implementation split
`ProcessService`, not `ProcessService` + `ProcessServiceImpl`. Classes are concrete. Mockito mocks concrete classes.

### No ApplicationEventPublisher
Timeline and audit calls are explicit in every service method. The tradeoff is verbosity for 100% visible control flow — no "magic" wiring.

### AuditLog does not extend BaseEntity
`AuditLog` is an immutable compliance record. It has its own `performedAt: Instant` set in the constructor. Using `@LastModifiedDate` on a log entry would be semantically wrong.

### `totalCost` computed and persisted
`totalCost = unitCost × quantity + shippingCost`, normalized to scale 4, stored in the database. If the formula changes in the future, historical records remain accurate.

### Atomic quotation selection
`selectQuotation()` calls `clearSelectedByProcess(processId)` via `@Modifying @Query` before calling `quotation.select()`. This ensures exactly one `selected = true` per process with no inconsistency window.

---

## Roadmap

- [ ] **Dispute module** — markup, margin, bid strategy (these belong here, not in Quotation)
- [ ] **PostBid module** — homologation, adjudication, final result
- [ ] **Contract module** — contract number, validity, balance, budget notes
- [ ] Supplier name/document snapshot on Quotation (for immutable historical accuracy)
- [ ] Partial unique index `WHERE selected = true` at the database level
- [ ] Integration tests (full workflow, no Spring context)
- [ ] Role promotion endpoint (ANALYST → MANAGER → ADMIN)

---

## License

MIT License — Copyright (c) 2026 [Igor Leite de Andrade](https://github.com/igorleite)
