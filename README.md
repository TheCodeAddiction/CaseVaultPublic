# CaseVault

CaseVault is a fictional police case & evidence management system built as a
whitebox source code review training exercise. The app is intentionally
realistic-looking (Spring Boot backend, React frontend) so participants can
practice spotting vulnerabilities and structural/architecture issues by
reading real, "decent size" application code — not a toy snippet.

This is training material. Do not use any part of this codebase (auth,
password handling, etc.) as a reference for production code.

## Documentation

- [`docs/api-contract.md`](docs/api-contract.md) — every backend endpoint:
  request/response shapes, validation, access rules and error codes.
- [`docs/roles-and-permissions.md`](docs/roles-and-permissions.md)
  ([Norsk](docs/roles-and-permissions.no.md)) — who is allowed to do what.

## How to install and run

Requires Java 17+, Maven (via the included wrapper), and Node.js 18+.

**Backend** (Spring Boot, runs on `http://localhost:9090`):

```bash
cd backend
./mvnw spring-boot:run
```

Uses an embedded H2 database stored in `backend/data/` — no external
database needed. **Always run this from inside `backend/`.** The database
path is relative to the process's working directory, so launching it from
anywhere else (repo root, an IDE run configuration with a different working
directory, etc.) silently creates a new, empty database instead of erroring
— which looks like your accounts/cases disappeared. Every startup logs the
resolved absolute database path; check that log line if data ever looks
missing.

Since there's no Flyway/Liquibase and `spring.jpa.hibernate.ddl-auto=update`,
a schema-incompatible code change (e.g. a column type change, or a new
non-nullable column with no backfill) won't auto-migrate an existing local
database — delete `backend/data/` before running after pulling a change like
that. It's disposable demo data, not something that needs migrating.

**Frontend** (React + Vite, runs on `http://localhost:5173`):

```bash
cd frontend
npm install
npm run dev
```

The frontend dev server proxies `/api` requests to the backend, so start the
backend first.

### Running tests

```bash
cd backend && ./mvnw test
cd frontend && npm test
```
