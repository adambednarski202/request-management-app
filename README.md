# Request Management Application

The Request Management Application is a production-grade Spring Boot backend
service that manages the lifecycle of formal requests. It guides each request
through a deterministic, auditable state machine—from initial creation to
verification, acceptance, and publication, or terminal rejection and deletion.

---

## Architecture overview

The application follows a clean 3-layer architecture that isolates HTTP
transport, business logic, and database persistence. This separation ensures
maintainability, modularity, and testability across all layers.

```
┌────────────────────────────────────────────────────────┐
│            Presentation / Web Controller Layer         │
│  (RequestController, DTOs, Bean Validation, RFC 7807)  │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│                    Service Layer                       │
│ (RequestService, AuditLogService, PublicationIdGen)    │
│            @Transactional Business Workflows           │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│             Domain & Persistence Layer                 │
│      (Request, AuditLogEntry, Spring Data JPA, H2)     │
└────────────────────────────────────────────────────────┘
```

The system organizes responsibilities across three core layers:

- **Web controller layer (`controller`, `dto`):** Handles incoming HTTP
  requests, enforces input validation via Jakarta Bean Validation annotations,
  maps DTO commands to domain actions, and formats HTTP responses using RFC 7807
  problem details for error conditions.
- **Service layer (`service`):** Orchestrates business transactions,
  manages transaction boundaries (`@Transactional`), and coordinates repository
  operations and audit log generation.
- **Domain and persistence layer (`domain`, `repository`):** Encapsulates core
  business invariants inside domain models (`Request`, `RequestState`), logs
  auditing entries (`AuditLogEntry`), and integrates with Spring Data JPA
  repositories backed by an in-memory H2 database.

A 3-layer architecture was chosen over hexagonal architecture. For small applications 
hexagonal architecture may introduce unnecessary overhead; a simpler structure ensures 
easier implementation and maintenance without compromising quality.

---

## State diagram and lifecycle governance

The lifecycle of each request follows a strict finite state machine. Transitions
can only move along permitted forward or terminal branches. Once a request
reaches a terminal state (`DELETED`, `REJECTED`, or `PUBLISHED`), you cannot
transition it to any other state.

```
       ( Start )
           │
        [Create]
           │
           ▼
     ┌───────────┐   [Delete]   ┌───────────┐
     │  CREATED  ├─────────────►│  DELETED  │ (Terminal)
     └─────┬─────┘              └───────────┘
           │
        [Verify]
           │
           ▼
     ┌───────────┐   [Reject]   ┌───────────┐
     │ VERIFIED  ├─────────────►│           │
     └─────┬─────┘              │           │
           │                    │           │
        [Accept]                │ REJECTED  │ (Terminal)
           │                    │           │
           ▼                    │           │
     ┌───────────┐   [Reject]   │           │
     │ ACCEPTED  ├─────────────►│           │
     └─────┬─────┘              └───────────┘
           │
       [Publish]
           │
           ▼
     ┌───────────┐
     │ PUBLISHED │ (Terminal - assigns publicationId)
     └───────────┘
```

The lifecycle enforces the following governing rules:

1. A request always starts in the `CREATED` state upon creation.
2. Content editing is exclusively permitted in the `CREATED` and `VERIFIED`
   states.
3. Cancellation to `DELETED` is only possible directly from the `CREATED` state.
4. Rejection to `REJECTED` is permitted from either `VERIFIED` or `ACCEPTED`.
5. Transitioning to `DELETED` or `REJECTED` mandates a non-blank `reason`.
6. Transitioning to `PUBLISHED` assigns a globally unique numeric identifier
   (`publicationId`) and closes the lifecycle.

---

## Business rules matrix

The table below summarizes every state in the application, its valid
transitions, permitted actions, and governing invariants.

| Source State | Permitted Target States | Allowed Actions | Requirements & Invariants |
|---|---|---|---|
| *(Start)* | `CREATED` | Create | `name` and `body` are mandatory. Initial state is always `CREATED`. |
| `CREATED` | `VERIFIED`, `DELETED` | Edit Content, Verify, Delete | `body` can be updated. Deleting mandates a non-blank `reason`. |
| `VERIFIED` | `ACCEPTED`, `REJECTED` | Edit Content, Accept, Reject | `body` can be updated. Rejecting mandates a non-blank `reason`. |
| `ACCEPTED` | `PUBLISHED`, `REJECTED` | Accept, Publish, Reject | Content modification is forbidden. Rejecting mandates a non-blank `reason`. Publishing assigns a unique numeric `publicationId`. |
| `PUBLISHED` | *(None)* | Read only | Terminal state. No further transitions or modifications are allowed. |
| `DELETED` | *(None)* | Read only | Terminal state. Contains mandatory cancellation reason. |
| `REJECTED` | *(None)* | Read only | Terminal state. Contains mandatory rejection reason. |

---

## REST API specification

All endpoints are hosted under the `/api/requests` path prefix. For state
transitions and modifications, the API accepts both `POST` and `PUT` HTTP
methods to accommodate different client integration conventions.

The controller exposes the following operations:

### Create request

Creates a new request and initializes its state to `CREATED`.

- **Method & Path:** `POST /api/requests`
- **Request Headers:** `Content-Type: application/json`
- **Request Body:**
  ```json
  {
    "name": "Cloud Infrastructure Provisioning",
    "body": "Provision 4-node Kubernetes cluster on AWS eu-west-1"
  }
  ```
- **Response Code:** `201 Created`
- **Response Headers:** `Location: /api/requests/{id}`
- **Response Body:** Returns the created `RequestResponse` object.

### Fetch request by identifier

Retrieves the current representation of a request by its unique numeric
identifier.

- **Method & Path:** `GET /api/requests/{id}`
- **Path Variable:** `id` (numeric `Long`, required)
- **Response Code:** `200 OK` (or `404 Not Found` if the identifier does not
  exist)
- **Response Body:** Returns the matching `RequestResponse` object.

### Modify request content

Updates the body content of an existing request. This action succeeds only while
the request is in the `CREATED` or `VERIFIED` state.

- **Method & Path:** `POST /api/requests/{id}/content` or
  `PUT /api/requests/{id}/content`
- **Path Variable:** `id` (numeric `Long`, required)
- **Request Body:**
  ```json
  {
    "body": "Provision 6-node Kubernetes cluster on AWS eu-west-1 with 32GB RAM"
  }
  ```
- **Response Code:** `200 OK` (or `409 Conflict` if in an ineligible state)
- **Response Body:** Returns the updated `RequestResponse` object.

### Verify request

Transitions a request from `CREATED` to `VERIFIED`.

- **Method & Path:** `POST /api/requests/{id}/verify` or
  `PUT /api/requests/{id}/verify`
- **Path Variable:** `id` (numeric `Long`, required)
- **Response Code:** `200 OK` (or `409 Conflict` if not in `CREATED` state)
- **Response Body:** Returns the updated `RequestResponse` object.

### Delete request

Cancels a request from `CREATED` to `DELETED`. A non-blank `reason` is mandatory.

- **Method & Path:** `POST /api/requests/{id}/delete` or
  `PUT /api/requests/{id}/delete`
- **Path Variable:** `id` (numeric `Long`, required)
- **Request Body:**
  ```json
  {
    "reason": "Project cancelled due to budget freeze"
  }
  ```
- **Response Code:** `200 OK` (or `400 Bad Request` if reason is omitted)
- **Response Body:** Returns the updated `RequestResponse` object.

### Reject request

Rejects a request from either `VERIFIED` or `ACCEPTED` to `REJECTED`. A non-blank
`reason` is mandatory.

- **Method & Path:** `POST /api/requests/{id}/reject` or
  `PUT /api/requests/{id}/reject`
- **Path Variable:** `id` (numeric `Long`, required)
- **Request Body:**
  ```json
  {
    "reason": "Hardware exceeds allocated quarterly hardware quota"
  }
  ```
- **Response Code:** `200 OK` (or `400 Bad Request` if reason is omitted)
- **Response Body:** Returns the updated `RequestResponse` object.

### Accept request

Transitions a request from `VERIFIED` to `ACCEPTED`.

- **Method & Path:** `POST /api/requests/{id}/accept` or
  `PUT /api/requests/{id}/accept`
- **Path Variable:** `id` (numeric `Long`, required)
- **Response Code:** `200 OK` (or `409 Conflict` if not in `VERIFIED` state)
- **Response Body:** Returns the updated `RequestResponse` object.

### Publish request

Transitions a request from `ACCEPTED` to `PUBLISHED` and assigns a unique
numeric identifier to `publicationId`.

- **Method & Path:** `POST /api/requests/{id}/publish` or
  `PUT /api/requests/{id}/publish`
- **Path Variable:** `id` (numeric `Long`, required)
- **Response Code:** `200 OK` (or `409 Conflict` if not in `ACCEPTED` state)
- **Response Body:** Returns the updated `RequestResponse` with numeric
  `publicationId`.

### Browse and search requests

Retrieves a paginated list of requests with optional filtering by name and
state.

- **Method & Path:** `GET /api/requests`
- **Query Parameters:**
  - `name` (string, optional): Case-insensitive substring match.
  - `state` (string enum, optional): Exact state match (`CREATED`, `VERIFIED`,
    `ACCEPTED`, `PUBLISHED`, `DELETED`, `REJECTED`).
  - `page` (integer, optional, default: `0`): Zero-based page index.
  - `size` (integer, optional, default: `10`): Number of items per page.
- **Response Code:** `200 OK`
- **Response Body:** `PageResponse<RequestResponse>` structure containing page
  metadata and the content array.

---

## Assumptions and design choices

During implementation, several technical design choices were established to
fulfill the requirements defined in `docs/request_application_management.md`:

### State initialization

The requirement specifies that creating an application mandates `name` and
`body`. The domain entity enforces that any newly instantiated request starts
unambiguously in the `CREATED` state through both builder initialization and the
JPA `@PrePersist` lifecycle hook.

### Identifier strategy

The primary key for requests (`Request.id`) uses a 64-bit numeric identifier
(`Long`) managed via `GenerationType.IDENTITY`. This strategy ensures
sequential, human-readable identifiers that integrate naturally with relational
databases and REST query paths.

### Publication identifier generation

The requirement specifies that upon publication, a request must receive a
"unique numeric identifier". The service satisfies this invariant by assigning
request id to publication id.
The downside of this solution is that publication IDs may have gaps if not all 
requests are published, although still it meats the criteria.

### Browsing and pagination

The browse endpoint provides pagination out of the box with a default page size
of 10 items and a default page index of 0. Dynamic filtering uses Spring Data
JPA `Specification` constructs, applying a case-insensitive substring search for
the `name` field and an exact match for the `state` enum.

### Audit logging

The service maintains an append-only audit trail in the `AuditLogEntry` table.
Every lifecycle transition records the request identifier, old state, new
state, optional reason, timestamp, and the assigned `publicationId` when
publishing. The audit recording uses `Propagation.MANDATORY`, guaranteeing that
the audit entry commits atomically within the same database transaction as the
request state modification.

### Validation and boundaries

Validation operates at two distinct boundaries:

1. **HTTP boundary:** DTOs validate incoming payloads using Jakarta Bean
   Validation (`@NotBlank`), rejecting malformed requests with `400 Bad
   Request`.
2. **Domain boundary:** Domain models guard state transitions (`transitionTo`,
   `verify`, `modify`, `delete`, `reject`, `accept`, `publish`), raising an
   `InvalidStateTransitionException` that the central `@RestControllerAdvice`
   translates into an RFC 7807 `409 Conflict` problem detail.

---

## Build and testing

The project uses Apache Maven and requires a standard Java development kit.

### Prerequisites

Before compiling the application, verify that your environment meets the
following software requirements:

- **Java:** JDK 21 or newer
- **Maven:** Apache Maven 3.9 or newer

Check your installed versions by running:

```bash
java -version
mvn -version
```

### Compiling and running tests

Execute the complete test suite, including domain unit tests, service workflow
tests, and MockMvc REST integration tests:

```bash
mvn clean test
```

To run a full Maven build and verification:

```bash
mvn clean verify
```

### Running the application locally

Run the application using the Spring Boot Maven plugin with the `local` profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

The application runs on port `8080` by default. Note that `local` is also
configured as the default Spring profile in `application.yml`, so running
`mvn spring-boot:run` without arguments automatically activates the local
environment.

### Accessing the H2 database console

The local profile enables the embedded H2 database console for inspecting data
and audit logs during development:

- **URL:** [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
- **JDBC URL:** `jdbc:h2:mem:requestdb`
- **User Name:** `sa`
- **Password:** *(leave blank)*

---

## Developer guide: end-to-end walkthrough

This walkthrough demonstrates how to execute all lifecycle transitions,
validate business constraints, and inspect error responses on your local
machine using `curl`.

Start the application in a separate terminal:

```bash
mvn spring-boot:run
```

Set shell variables for convenience:

```bash
BASE_URL="http://localhost:8080/api/requests"
CONTENT_TYPE="Content-Type: application/json"
```

### Executing the request lifecycle

Follow the steps below to step through every lifecycle scenario.

#### Scenario 1: Happy path from creation to publication

1. **Create request:**

   ```bash
   curl -i -X POST "$BASE_URL" \
     -H "$CONTENT_TYPE" \
     -d '{
       "name": "Cloud Infrastructure Provisioning",
       "body": "Provision 4-node Kubernetes cluster on AWS eu-west-1"
     }'
   ```

   The server returns `201 Created` with `Location: /api/requests/1`. Save the
   assigned identifier:

   ```bash
   REQUEST_ID=1
   ```

2. **Edit content in `CREATED` state:**

   ```bash
   curl -i -X POST "$BASE_URL/$REQUEST_ID/content" \
     -H "$CONTENT_TYPE" \
     -d '{
       "body": "Provision 6-node Kubernetes cluster on AWS eu-west-1 with 32GB RAM"
     }'
   ```

   Returns `200 OK` with the updated body.

3. **Verify request:**

   ```bash
   curl -i -X POST "$BASE_URL/$REQUEST_ID/verify"
   ```

   Returns `200 OK` with `"state": "VERIFIED"`.

4. **Edit content in `VERIFIED` state:**

   ```bash
   curl -i -X POST "$BASE_URL/$REQUEST_ID/content" \
     -H "$CONTENT_TYPE" \
     -d '{
       "body": "Provision 6-node Kubernetes cluster on AWS eu-west-1 with 32GB RAM and SSD EBS volumes"
     }'
   ```

   Returns `200 OK` confirming modification in `VERIFIED` state.

5. **Accept request:**

   ```bash
   curl -i -X POST "$BASE_URL/$REQUEST_ID/accept"
   ```

   Returns `200 OK` with `"state": "ACCEPTED"`.

6. **Publish request:**

   ```bash
   curl -i -X POST "$BASE_URL/$REQUEST_ID/publish"
   ```

   Returns `200 OK` with `"state": "PUBLISHED"` and assigned numeric
   `"publicationId": 1001`.

#### Scenario 2: Deletion with mandatory reason

1. **Create and delete request:**

   ```bash
   # Create
   DEL_RESP=$(curl -s -X POST "$BASE_URL" -H "$CONTENT_TYPE" \
     -d '{"name": "Legacy Server Decommission", "body": "Decommission rack B-12"}')
   DEL_ID=$(echo "$DEL_RESP" | grep -o '"id":[0-9]*' | cut -d: -f2)

   # Delete
   curl -i -X POST "$BASE_URL/$DEL_ID/delete" \
     -H "$CONTENT_TYPE" \
     -d '{"reason": "Project cancelled due to budget freeze"}'
   ```

   Returns `200 OK` with `"state": "DELETED"` and the cancellation reason.

#### Scenario 3: Rejection from verified state

1. **Create, verify, and reject:**

   ```bash
   # Create
   REJ1_RESP=$(curl -s -X POST "$BASE_URL" -H "$CONTENT_TYPE" \
     -d '{"name": "GPU Workstation", "body": "NVIDIA H100 80GB GPU"}')
   REJ1_ID=$(echo "$REJ1_RESP" | grep -o '"id":[0-9]*' | cut -d: -f2)

   # Verify
   curl -s -X POST "$BASE_URL/$REJ1_ID/verify" > /dev/null

   # Reject
   curl -i -X POST "$BASE_URL/$REJ1_ID/reject" \
     -H "$CONTENT_TYPE" \
     -d '{"reason": "Hardware exceeds allocated quarterly hardware quota"}'
   ```

   Returns `200 OK` with `"state": "REJECTED"`.

#### Scenario 4: Rejection from accepted state

1. **Create, verify, accept, and reject:**

   ```bash
   # Create
   REJ2_RESP=$(curl -s -X POST "$BASE_URL" -H "$CONTENT_TYPE" \
     -d '{"name": "Third-Party SaaS", "body": "Analytics SDK integration"}')
   REJ2_ID=$(echo "$REJ2_RESP" | grep -o '"id":[0-9]*' | cut -d: -f2)

   # Verify & Accept
   curl -s -X POST "$BASE_URL/$REJ2_ID/verify" > /dev/null
   curl -s -X POST "$BASE_URL/$REJ2_ID/accept" > /dev/null

   # Reject
   curl -i -X POST "$BASE_URL/$REJ2_ID/reject" \
     -H "$CONTENT_TYPE" \
     -d '{"reason": "Security compliance review failed"}'
   ```

   Returns `200 OK` with `"state": "REJECTED"`.

#### Scenario 5: Browsing and filtering

1. **Browse with default pagination:**

   ```bash
   curl -i -X GET "$BASE_URL"
   ```

2. **Filter by state:**

   ```bash
   curl -i -X GET "$BASE_URL?state=PUBLISHED"
   ```

3. **Combined filter and pagination:**

   ```bash
   curl -i -X GET "$BASE_URL?name=Cloud&state=PUBLISHED&page=0&size=5"
   ```

#### Scenario 6: Validation and error enforcement

1. **Missing mandatory creation fields (`400 Bad Request`):**

   ```bash
   curl -i -X POST "$BASE_URL" \
     -H "$CONTENT_TYPE" \
     -d '{"name": "", "body": "Missing name"}'
   ```

2. **Missing mandatory reason on delete (`400 Bad Request`):**

   ```bash
   curl -i -X POST "$BASE_URL/$REQUEST_ID/delete" \
     -H "$CONTENT_TYPE" \
     -d '{"reason": "   "}'
   ```

3. **Content edit in forbidden state (`409 Conflict`):**

   ```bash
   # Attempting to edit content on a published request
   curl -i -X POST "$BASE_URL/$REQUEST_ID/content" \
     -H "$CONTENT_TYPE" \
     -d '{"body": "Unauthorized edit"}'
   ```

5. **Resource not found (`404 Not Found`):**

   ```bash
   curl -i -X GET "$BASE_URL/999999"
   ```

---

## Next steps

To prepare the application for production deployment, consider the following
enhancements:

- **Persistent database configuration:** Switch from the in-memory H2 database
  to PostgreSQL or MySQL by configuring a production Spring profile in
  `application-prod.yml`.
- **API documentation:** Integrate Springdoc OpenAPI to generate interactive
  Swagger UI documentation automatically from controller annotations.
- **Security and authorization:** Introduce Spring Security with OAuth2 or JWT
  tokens to restrict state transitions such as verification, acceptance, and
  publication to authorized roles.
