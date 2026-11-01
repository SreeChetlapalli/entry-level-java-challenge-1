# Decisions

## 1. Which endpoints did you add, and why does this system need them?

I added `GET /api/v1/employee/active`, which returns employees whose
`contractTerminationDate` is null. A webhook consumer can use it when it needs
the current workforce without downloading former employees and filtering them
itself.

## 2. Did you add any dependency? What does it buy, and what does it cost?

I added no dependencies. The existing Spring Web, Jackson, Lombok, and test
dependencies were sufficient for the API, in-memory storage, logging, and tests.

## 3. How did you handle errors and logging, and why those mechanisms?

Missing employees return 404; malformed UUIDs, unreadable JSON, missing required
fields, and invalid employee values return 400. A controller advice returns a
consistent JSON error body with status, reason, message, and request path.
Rejected requests log the HTTP method, path, status, and safe reason; successful
creates log the generated employee UUID. Neither path logs names, salary, email,
or request bodies.

## 4. Where the brief left a decision to you, what did you decide?

The create request requires first and last name, non-negative salary, positive
age, job title, email, and contract hire date; termination date is optional and
cannot precede hire date. Names, job title, and email are trimmed. The response
includes the interface's `fullName` field, generated UUID, and all contract
dates. Create returns 201 with a Location header. Employee data is held in a
thread-safe in-memory map and two sample employees are loaded at startup.

## 5. What did you deliberately choose *not* to do?

I did not add persistence, authentication infrastructure, employee update/delete
operations, or advanced email validation. The challenge explicitly limits
infrastructure and expects in-memory storage; persistence and authentication
would be revisited when deployment and webhook identity requirements are known.
The API currently validates email shape only because stronger validation would
require a clearly defined business policy.
