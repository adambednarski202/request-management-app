# Coding test: Request Management Application

Build a backend service that manages the lifecycle of requests. Each request moves through a defined set of states, and every transition must be governed by the accompanying state diagram.

---

## Functional Requirements

The state diagram defines the valid states of a request and the permitted transitions between them.

* **Create Application** $\rightarrow$ The `name` and a `body` (content) are mandatory.
* **Editing content of Application** $\rightarrow$ `body` may only be modified while the request is in the `CREATED` or `VERIFIED` state.
* **Deleting/rejecting** $\rightarrow$ A `reason` must be supplied whenever a request is deleted or rejected.
* **Publication** $\rightarrow$ When a request is published, it must be assigned a unique numeric identifier.
* **Audit log** *(Optional)* $\rightarrow$ The complete history of state changes must be stored.
* **Browsing** *(Optional)* $\rightarrow$ Requests must be listable with pagination (default page size: 10) and filterable by `name` and by `state`.

---

## Technical Requirements

* **API:** Expose the functionality as REST endpoints. No GUI is required.
* **Persistence:** Persist requests in a database (SQL or NoSQL). The database itself is not part of the deliverable and should not be submitted.
* **Submission:** Submit source code only $\rightarrow$ no compiled classes, no dependency binaries, no bundled servers.
* **Assumptions:** Anything not explicitly specified is left to your own judgement; please provide a document with the assumptions you made.

---

## What We Will Look At

1. **Solution design** – Abstraction, configurability, separation of infrastructure from domain logic, etc.
2. **Correctness** – The application behaves as specified.
3. **Test coverage** – Code covered by tests.

---

## Deliverables

Submit by the agreed deadline:
* A link to a public repository on GitHub or Bitbucket.
* A short description of the application must be placed in a `../README.md` file in the repository root.

---

## State Diagram

```
       ( Start )
           │
        [Create]
           │
           ▼
     ┌───────────┐   [Delete]   ┌───────────┐
     │  CREATED  ├─────────────►│  DELETED  │
     └─────┬─────┘              └───────────┘
           │
        [Verify]
           │
           ▼
     ┌───────────┐   [Reject]   ┌───────────┐
     │ VERIFIED  ├─────────────►│           │
     └─────┬─────┘              │           │
           │                    │           │
        [Accept]                │ REJECTED  │
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
     │ PUBLISHED │
     └───────────┘
```

### State Transitions Summary

* **Create:** `START` $\rightarrow$ `CREATED`
* **Delete:** `CREATED` $\rightarrow$ `DELETED`
* **Verify:** `CREATED` $\rightarrow$ `VERIFIED`
* **Reject:** `VERIFIED` $\rightarrow$ `REJECTED`
* **Accept:** `VERIFIED` $\rightarrow$ `ACCEPTED`
* **Reject:** `ACCEPTED` $\rightarrow$ `REJECTED`
* **Publish:** `ACCEPTED` $\rightarrow$ `PUBLISHED`