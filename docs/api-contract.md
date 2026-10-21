# CaseVault API Contract

Reference for every endpoint the CaseVault backend exposes: request/response
shapes, validation rules, access rules and error codes.

## Conventions

- Base path: `/api`
- JSON in, JSON out. `Content-Type: application/json`.
- Auth: `Authorization: Bearer <jwt>` header on protected endpoints.
- Enums are UPPER_SNAKE_CASE strings on the wire.
- Error shape (all 4xx/5xx):
  ```json
  { "error": "VALIDATION_ERROR", "message": "Human-readable summary", "fields": { "username": "Username is already taken" } }
  ```
  `fields` is omitted when the error isn't field-specific.

## Enums

- `District`: `NORTH`, `EAST`, `SOUTH`, `WEST`
- `Role`: `OFFICER`, `SUPERVISOR`, `ADMIN`
- `CaseStatus`: `OPEN`, `IN_PROGRESS`, `CLOSED`
- `DocumentType`: `MESSAGE`, `REPORT`, `FILE`

## POST /api/auth/register

Public. Creates a new account. Always created with `role = OFFICER` —
role is never accepted from the client.

**Request**
```json
{
  "fullName": "Maria Ibsen",
  "username": "mibsen",
  "email": "mibsen@politiet.example",
  "password": "correct horse battery staple",
  "confirmPassword": "correct horse battery staple",
  "district": "NORTH"
}
```

**Validation**
- `fullName`: required, 2-100 chars.
- `username`: required, 3-50 chars, unique (case-insensitive), pattern `[a-zA-Z0-9._-]+`.
- `email`: required, must be a syntactically valid email address. Not
  checked for uniqueness — two accounts could share an email.
- `password`: required, 8-100 chars.
- `confirmPassword`: must match `password` (server-side check, not just client-side).
- `district`: required, must be a valid `District` enum value.

**Response `201 Created`**
```json
{
  "id": 42,
  "username": "mibsen",
  "email": "mibsen@politiet.example",
  "fullName": "Maria Ibsen",
  "district": "NORTH",
  "role": "OFFICER"
}
```
No token is returned — registration does not auto-login. User is redirected
to `/login` on the frontend with a success message.

**Errors**
- `400 VALIDATION_ERROR` — field-level messages as above.
- `409 USERNAME_TAKEN` — username already exists.

## POST /api/auth/login

Public.

**Request**
```json
{ "username": "mibsen", "password": "correct horse battery staple" }
```

**Response `200 OK`**
```json
{
  "token": "eyJhbGciOi...",
  "expiresAt": "2026-09-03T14:30:00Z",
  "user": {
    "id": 42,
    "username": "mibsen",
    "email": "mibsen@politiet.example",
    "fullName": "Maria Ibsen",
    "district": "NORTH",
    "role": "OFFICER"
  }
}
```

**Errors**
- `401 INVALID_CREDENTIALS` — generic message ("Invalid username or
  password") for "user not found," "wrong password," a deactivated account
  (see `POST /api/admin/users/{id}/deactivate`), and a locked-out account
  (see below) alike — one status/message for every case, to avoid leaking
  which one applies.

**Login lockout**: after 5 consecutive failed password attempts for an
account, it's locked for 15 minutes — further login attempts get the same
generic `401 INVALID_CREDENTIALS`, whether or not the password given is
actually correct. A successful login resets the failed-attempt counter to
zero.

Also sets the token as an `HttpOnly`, `SameSite=Lax`, `Path=/` cookie
(`Set-Cookie: token=...`), matching the response body's `expiresAt` as
`Max-Age`. `JwtAuthenticationFilter` accepts a token from either the
`Authorization: Bearer <token>` header or this cookie (header takes
precedence).

## POST /api/auth/logout

Public. Clears the login cookie (`Max-Age=0`). No effect on the
`Authorization`-header flow — the SPA also clears its own `localStorage`
token client-side.

**Response `200 OK`**: `{ "message": "Logged out." }`

## POST /api/auth/forgot-password

Public. Requests a password reset code for an account. There's no email
infrastructure, so the 4-digit numeric code is logged server-side instead
of actually being emailed.

**Request**: `{ "username": "mibsen" }`

**Response `200 OK`**: `{ "message": "If that account exists, a reset code
has been sent." }`, returned identically whether or not the username
exists, to avoid username enumeration. If the username exists, a code is
generated with a 10-minute lifetime and up to 10 verification attempts.

## POST /api/auth/reset-password

Public. Verifies a reset code and sets a new password.

**Request**
```json
{ "username": "mibsen", "code": "1234", "newPassword": "a new, longer password" }
```

**Validation**: `newPassword` 8-100 chars.

**Response `200 OK`**: `{ "message": "Password updated." }`

**Errors**
- `400 VALIDATION_ERROR` — new password too short/long.
- `400 INVALID_RESET_CODE` — one generic error for every failure case
  (unknown username, no outstanding code, expired code, wrong code, or a
  code that's already used up all 10 attempts) — deliberately not
  distinguished, to avoid leaking which case applies. A wrong code
  consumes one of the code's 10 attempts.

## GET /api/health

Public. A basic liveness/readiness check — verifies the database is
reachable with a lightweight query.

**Response `200 OK`**: `{ "status": "UP" }`
**Response `503 SERVICE_UNAVAILABLE`**: `{ "status": "DOWN" }`

## GET /api/me

Protected (any authenticated role). Returns the caller's own profile, used
by the frontend to hydrate user state on page load/refresh.

**Response `200 OK`**
```json
{
  "id": 42,
  "username": "mibsen",
  "email": "mibsen@politiet.example",
  "fullName": "Maria Ibsen",
  "district": "NORTH",
  "role": "OFFICER"
}
```

**Errors**
- `401 UNAUTHORIZED` — missing/invalid/expired token.

`Case.id` is a UUID, assigned at creation via `GenerationType.UUID`.
Serialized as the standard UUID string form everywhere it appears in a response (`GET`/`POST /api/cases`, activity
entries, `relatedCaseId` on messages, etc).

## GET /api/cases

Protected (any authenticated role). Returns cases visible to the caller,
scoped by role and district:

- `OFFICER` — cases in their own district, plus any case *shared* with
  their district or with them directly (see "Case sharing" below).
- `SUPERVISOR` — all cases in their own district, plus any case shared with
  their district.
- `ADMIN` — all cases, all districts (sharing is irrelevant — admin already
  sees everything).

Optional query parameters, applied on top of the role/district scoping above
(never widen it — a filter can only narrow what a caller can already see):
- `q`: case-insensitive substring match against `title` or `description`.
- `status`: exact `CaseStatus` match.
- `page` (default `0`), `size` (default `20`): pagination.

**Response `200 OK`**
```json
{
  "content": [
    {
      "id": "b3f1c9a0-6e2d-4c7a-9c1e-3a2f8d5b7e10",
      "caseNumber": "NORTH-2026-0001",
      "title": "Break-in at Elm Street warehouse",
      "status": "OPEN",
      "district": "NORTH",
      "assignedOfficerUsername": "mibsen",
      "createdAt": "2026-08-20T09:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

**Errors**
- `401 UNAUTHORIZED` — missing/invalid/expired token.

## GET /api/cases/stats

Protected (any authenticated role). Simple counts for a dashboard summary
widget, scoped by the same visibility rule as `GET /api/cases` — an
officer's counts only reflect cases they could otherwise see, not every
case in the system.

**Response `200 OK`**
```json
{
  "totalCases": 6,
  "openCases": 3,
  "inProgressCases": 2,
  "closedCases": 1,
  "recentCases": [
    {
      "id": "b3f1c9a0-6e2d-4c7a-9c1e-3a2f8d5b7e10",
      "caseNumber": "NORTH-2026-0001",
      "title": "Break-in at Elm Street warehouse",
      "status": "OPEN",
      "district": "NORTH",
      "assignedOfficerUsername": "mibsen",
      "createdAt": "2026-08-20T09:00:00Z"
    }
  ]
}
```
`recentCases` is the 5 most recently created cases visible to the caller
(same `CaseSummaryResponse` shape as `GET /api/cases`), for a "recently
created" widget on the dashboard that links to `/cases/{id}`.

**Errors**
- `401 UNAUTHORIZED`

## GET /api/cases/export

Protected. Same visibility scoping and `q`/`status` filters as `GET
/api/cases`, but returns every match as `text/csv` (no pagination) with
columns: Case Number, Title, Status, District, Assigned
Officer, Created At.

**Errors**
- `401 UNAUTHORIZED`

## POST /api/cases

Protected (any authenticated role). Creates a case in the caller's own
district, auto-assigned to the caller. Always `status = OPEN` — there's no
way to create a case with a different starting status yet.

**Request**
```json
{
  "title": "Stolen bicycle",
  "description": "Reported missing from a locked rack outside the library."
}
```

**Validation**
- `title`: 3-200 chars.
- `description`: required, non-blank, max 2000 chars.

**Response `201 Created`**
```json
{
  "id": "e7a4d2f1-9b3c-4a6e-8d0f-1c2b3a4d5e6f",
  "caseNumber": "SOUTH-2026-0004",
  "title": "Stolen bicycle",
  "description": "Reported missing from a locked rack outside the library.",
  "status": "OPEN",
  "district": "SOUTH",
  "assignedOfficerUsername": "mibsen",
  "createdAt": "2026-09-03T09:00:00Z"
}
```

Also records a `CASE_CREATED` activity log entry, in the same transaction as
creating the case (see "Activity log" below).

**Errors**
- `400 VALIDATION_ERROR` — field-level messages.
- `401 UNAUTHORIZED` — missing/invalid/expired token.

## GET /api/cases/{id}

Protected. Returns full case detail if the caller can see the case (same
visibility rule as `GET /api/cases`); otherwise `404`, not `403` — the API
doesn't reveal whether a case with that id exists outside the caller's
visibility.

**Response `200 OK`**: same shape as the `POST /api/cases` response.

**Errors**
- `404 CASE_NOT_FOUND`
- `401 UNAUTHORIZED`

## PUT /api/cases/{id}/status

Protected. Same visibility check as `GET /api/cases/{id}`. Sets the case's
status to any `CaseStatus` value — no transition state machine (e.g.
`CLOSED` → `OPEN` is allowed), matching the simple workflow used so far.
Records a `STATUS_CHANGED` activity entry with the old and new value.

**Request**
```json
{ "status": "IN_PROGRESS" }
```

**Response `200 OK`**: same shape as `GET /api/cases/{id}`.

**Errors**
- `400 VALIDATION_ERROR` — missing/invalid status value.
- `404 CASE_NOT_FOUND`
- `401 UNAUTHORIZED`

## PUT /api/cases/{id}/assignee

Protected, `SUPERVISOR`/`ADMIN` only (`403 FORBIDDEN` otherwise), via
method-level `@PreAuthorize("hasAnyRole('SUPERVISOR','ADMIN')")` — not
class-level, since most of `CaseController`'s other endpoints are open to
any authenticated role (visibility scoping instead happens in
`CaseService`); this and `.../shares` below are the only two actions in
the controller elevated to a specific role. Same visibility check as `GET
/api/cases/{id}` first. Assigns the case to the
named officer, who must have `role = OFFICER` and be in the case's
district. Records a `CASE_ASSIGNED` activity entry.

**Request**
```json
{ "officerUsername": "mibsen" }
```

**Response `200 OK`**: same shape as `GET /api/cases/{id}`.

**Errors**
- `400 INVALID_CASE_OPERATION` — target user isn't an officer, or isn't in the case's district.
- `401 UNAUTHORIZED` — no such user (reuses the generic credentials error).
- `403 FORBIDDEN` — caller isn't a supervisor/admin.
- `404 CASE_NOT_FOUND`

## Case sharing

- `POST /api/cases/{id}/shares` — `SUPERVISOR`/`ADMIN` only. Shares the case
  with **exactly one** of another district or one specific user (both
  fields exist on the request; provide exactly one, `400
  INVALID_CASE_OPERATION` otherwise). Either grants the recipient(s) the
  same read visibility as if the case were native to their district. A
  supervisor may only share cases in their own district; an admin can share
  any case. Records a `CASE_SHARED` activity entry.

  **Request**: `{ "district": "EAST" }` or `{ "username": "mibsen" }`

  **Response `201 Created`**
  ```json
  { "id": 1, "sharedWithDistrict": "EAST", "sharedWithUsername": null, "sharedByUsername": "supervisor1", "sharedAt": "2026-09-03T09:00:00Z" }
  ```
  Exactly one of `sharedWithDistrict` / `sharedWithUsername` is non-null.

  **Errors**: `400 INVALID_CASE_OPERATION` (neither or both of
  district/username provided, already shared with that district/user, or
  sharing with yourself), `403 FORBIDDEN` (not `SUPERVISOR`/`ADMIN`, or
  supervisor outside their own district), `404 CASE_NOT_FOUND` (no case with
  that id), `401 UNAUTHORIZED` (unknown username).

- `GET /api/cases/{id}/shares` — same visibility check as `GET
  /api/cases/{id}`. Lists current shares, newest first, same item shape as
  the `POST` response.

- `DELETE /api/cases/{id}/shares/{shareId}` — `SUPERVISOR`/`ADMIN` only.
  Revokes a share, immediately removing the recipient(s)' visibility into
  the case. Same district restriction as creating a share: a supervisor may
  only revoke shares on cases in their own district; an admin can revoke
  any. Records a `CASE_UNSHARED` activity entry.

  **Response `200 OK`**: `{ "message": "Share removed." }`

  **Errors**: `403 FORBIDDEN` (not `SUPERVISOR`/`ADMIN`, or supervisor
  outside their own district), `404 CASE_NOT_FOUND`, `404
  CASE_SHARE_NOT_FOUND` (no such share, or it belongs to a different case),
  `401 UNAUTHORIZED`.

## GET /api/cases/{id}/activity

Protected. Same visibility check as `GET /api/cases/{id}`. Returns the
case's activity log, newest first.

**Response `200 OK`**
```json
[
  {
    "id": 1,
    "actorUsername": "mibsen",
    "actorFullName": "Maria Ibsen",
    "action": "CASE_CREATED",
    "details": "Case created",
    "occurredAt": "2026-09-03T09:00:00Z"
  }
]
```

**Errors**
- `404 CASE_NOT_FOUND`
- `401 UNAUTHORIZED`

## GET /api/admin/users/me

Protected, any authenticated role — no restriction, just like `GET
/api/me`. Returns the caller's own profile. Duplicates `GET /api/me`
rather than replacing it, so that every user-administration and profile
action lives under `/api/admin`.

**Response `200 OK`**: `{id, username, email, fullName, district, role,
hasProfilePicture, active}`. `hasProfilePicture` is a boolean, not the image
itself — there's no endpoint to fetch the actual picture bytes yet. `active`
is always `true` here in practice, since a deactivated account can't log in
to obtain a token in the first place.

## PUT /api/admin/users/me

Protected, any authenticated role — no restriction. `multipart/form-data`.
One consolidated endpoint for every self-service account update: profile
details, password change, and profile picture upload all go through this
same request, since none of them can express `multipart/form-data`'s file
part as a JSON body. All fields optional; any field left out is left
unchanged. Records a single `ACCOUNT_UPDATED` activity entry
(`entityType = "USER"`) summarizing which fields changed, regardless of
which ones were sent.

**Form fields** (all optional)
- `fullName` — 2-100 chars if present.
- `email` — must be a valid email format if present.
- `currentPassword` / `newPassword` — both required together to change the
  password; `newPassword` must be 8-100 chars. `currentPassword` must
  match the caller's existing password.
- `file` — a `.jpg`/`.jpeg`/`.png` image (narrower allowlist than case
  documents, which also allow `.pdf`/`.txt`). Reuses `FileStorageService`
  (generated stored filename, size limit) after `AdminController`'s own
  Content-Type + magic-byte check.

**Response `200 OK`**: same shape as `GET /api/admin/users/me`, reflecting
whatever changed.

**Errors**
- `400 VALIDATION_ERROR` — `fullName`/`email`/`newPassword` fails
  validation.
- `400 INVALID_FILE` — disallowed picture extension/content or file too
  large.
- `401 INVALID_CREDENTIALS` — `newPassword` was sent without a matching
  `currentPassword` (reuses the same generic error as login, rather than a
  dedicated "wrong password" message).
- `401 UNAUTHORIZED`

## GET /api/admin/users/{id}/picture

Protected (any authenticated role — viewing another user's picture isn't
restricted, similar to a staff directory photo). Returns the raw image
bytes with a `Content-Type` derived server-side from the file's extension
at upload time (`image/jpeg` or `image/png`), not the client-supplied
`Content-Type` header from the upload request. The frontend can't
just point an `<img src>` at this directly, since that can't carry the
`Authorization` header — `ProfilePicture` fetches it as a blob and uses
`URL.createObjectURL`, same pattern as case file downloads.

**Response `200 OK`**: the image bytes, `Content-Type` of `image/jpeg` or
`image/png`.

**Errors**
- `404 PROFILE_PICTURE_NOT_FOUND` — user exists but has no picture set.
- `404 USER_NOT_FOUND` — no such user id.
- `401 UNAUTHORIZED`

## GET /api/admin/users

Protected, `SUPERVISOR`/`ADMIN` only (`403 FORBIDDEN` otherwise). Behavior
depends on the caller's role:
- `ADMIN` — returns every user in the system, all districts and roles.
- `SUPERVISOR` — returns `OFFICER`s in the caller's own district only. Used
  to populate the officer picker for case assignment.

**Response `200 OK`**: array of `{id, username, email, fullName, district, role,
hasProfilePicture, active}`.

## GET /api/admin/cases/overview

Protected, `ADMIN` only (`403 FORBIDDEN` otherwise). Department-wide case
overview for the admin dashboard: every case in the system, regardless of
district.

**Response `200 OK`**: array of `CaseSummaryResponse`, same shape as items
in `GET /api/cases`'s `content` array (`id`, `caseNumber`, `title`,
`status`, `district`, `assignedOfficerUsername`, `createdAt`).

**Errors**
- `403 FORBIDDEN` — caller isn't `ADMIN`
- `401 UNAUTHORIZED`

## GET /api/admin/users/{id}/activity

Protected, `ADMIN` only. Every recorded activity entry for that user
(`entityType = "USER"`), newest first — every admin/profile action that
calls `AuditLogService.record(...)` against a user shows up here (password
changes, picture updates, role/district changes, deactivation,
reactivation, account creation). Same generic `ActivityLogEntryResponse`
shape and the same `ActivityTimeline` frontend component `GET
/api/cases/{id}/activity` already uses.

**Response `200 OK`**: array of `{id, actorUsername, actorFullName, action, details, occurredAt}`.

**Errors**
- `403 FORBIDDEN` — caller isn't `ADMIN`
- `404 USER_NOT_FOUND`
- `401 UNAUTHORIZED`

## POST /api/admin/users

Protected, `ADMIN` only (`403 FORBIDDEN` otherwise). Creates a user account
with any role and district — the only way to create a `SUPERVISOR`/`ADMIN`
account, since self-registration always makes an `OFFICER`. Reuses the same
username pattern/password-length validation as `POST /api/auth/register`,
and the same `PasswordHasher`. Records a `USER_CREATED` activity entry
(`entityType = "USER"`, keyed on the new user's id, actor is the admin who
created it).

**Request**
```json
{
  "fullName": "South Supervisor",
  "username": "southsup",
  "email": "southsup@politiet.example",
  "password": "a new, longer password",
  "role": "SUPERVISOR",
  "district": "SOUTH"
}
```

**Validation**: `fullName` 2-100 chars; `username` 3-50 chars, letters/
digits/`.`/`_`/`-` only; `email` required, valid email syntax; `password`
8-100 chars; `role` and `district` required.

**Response `201 Created`**: `{id, username, email, fullName, district, role,
hasProfilePicture, active}`.

**Errors**
- `400 VALIDATION_ERROR`
- `403 FORBIDDEN` — caller isn't `ADMIN`
- `409 USERNAME_TAKEN`

## PUT /api/admin/users/{id}/role

Protected, `ADMIN` only. Changes another user's role. Records a
`ROLE_CHANGED` activity entry (actor is the admin, entity is the target
user).

**Request**: `{ "role": "SUPERVISOR" }`

**Response `200 OK`**: `{id, username, email, fullName, district, role,
hasProfilePicture, active}`.

**Errors**
- `400 VALIDATION_ERROR` — missing/invalid role
- `403 FORBIDDEN` — caller isn't `ADMIN`
- `404 USER_NOT_FOUND`

## PUT /api/admin/users/{id}/district

Protected, `ADMIN` only. Reassigns another user's district. Records a
`DISTRICT_CHANGED` activity entry.

**Request**: `{ "district": "WEST" }`

**Response `200 OK`**: same shape as `PUT /api/admin/users/{id}/role`.

**Errors**
- `400 VALIDATION_ERROR` — missing/invalid district
- `403 FORBIDDEN` — caller isn't `ADMIN`
- `404 USER_NOT_FOUND`

## POST /api/admin/users/{id}/deactivate

Protected, `ADMIN` only. Sets `active = false` on the target user. A
deactivated account can no longer log in (`POST /api/auth/login` returns
`401 INVALID_CREDENTIALS`, the same generic error as a wrong password —
deactivation isn't distinguished from bad credentials in the response).
Existing JWTs already issued to that user remain valid until they expire —
this endpoint doesn't revoke tokens, since the app has no server-side
session/token-revocation list. Records a `USER_DEACTIVATED` activity entry.

**Response `200 OK`**: same shape as `PUT /api/admin/users/{id}/role`, with
`active` now `false`.

**Errors**
- `403 FORBIDDEN` — caller isn't `ADMIN`
- `404 USER_NOT_FOUND`

## POST /api/admin/users/{id}/reactivate

Protected, `ADMIN` only. Sets `active = true` on the target user, undoing
`.../deactivate`. Records a `USER_REACTIVATED` activity entry.

**Response `200 OK`**: same shape as `PUT /api/admin/users/{id}/role`, with
`active` now `true`.

**Errors**
- `403 FORBIDDEN` — caller isn't `ADMIN`
- `404 USER_NOT_FOUND`

## DELETE /api/admin/users/{id}

Protected, `ADMIN` only. Permanently deletes another user's account —
allowed only if that account has left no historical footprint: no assigned
cases, authored documents, activity log entries, case shares, or sent/
received messages. If it has any of those, deletion is refused; the
account must be deactivated instead. (Outstanding password-reset codes for
the account are deleted as part of this, since they're ephemeral rather
than meaningful history.) Records a `USER_DELETED` activity entry
(attributed to the admin, since the target row no longer exists to be an
actor).

**Response `200 OK`**: `{ "message": "User deleted." }`

**Errors**
- `403 FORBIDDEN` — caller isn't `ADMIN`
- `404 USER_NOT_FOUND`
- `409 CANNOT_DELETE_USER` — account has existing history; deactivate it instead.

## DELETE /api/admin/users/me

Protected (any authenticated role). Self-service account deletion, after
re-confirming the current password. Same "no historical footprint" rule
and errors as the admin-initiated delete above. Not recorded via
`AuditLogService`: its `actor` column is a hard foreign key to `users`, so
there would be no row left to attribute the entry to once the account is
gone — and logging it beforehand would make the account permanently
ineligible for its own deletion, since that log entry would itself count
as history.

**Request**: `{ "currentPassword": "correct horse battery staple" }`

**Response `200 OK`**: `{ "message": "Account deleted." }`

**Errors**
- `401 INVALID_CREDENTIALS` — current password doesn't match.
- `409 CANNOT_DELETE_USER` — account has existing history; deactivate it instead.

## GET /api/cases/{caseId}/documents

Protected. Same visibility check as `GET /api/cases/{id}`. Returns the
case's document log (messages, reports, files), oldest first. No edit, but
a document can be deleted — see `DELETE
/api/cases/{caseId}/documents/{documentId}` below.

**Response `200 OK`**
```json
[
  {
    "id": 3,
    "type": "MESSAGE",
    "title": "Update",
    "content": "Spoke with the witness again, no new leads.",
    "originalFilename": null,
    "fileContentType": null,
    "fileSizeBytes": null,
    "authorUsername": "mibsen",
    "authorFullName": "Maria Ibsen",
    "createdAt": "2026-09-03T10:00:00Z"
  }
]
```
`content` is populated for `MESSAGE`/`REPORT`; the `originalFilename`/
`fileContentType`/`fileSizeBytes` fields are populated for `FILE` instead
(the other set is `null`).

`content` is stored and returned as **Markdown source**, exactly as the
author typed it — the server neither renders nor rewrites it. Clients are
expected to render it as Markdown (the SPA does, via `MarkdownContent`).
Direct-message bodies on `/api/messages` are *not* Markdown — they are
plain text.

**Errors**
- `404 CASE_NOT_FOUND`
- `401 UNAUTHORIZED`

## POST /api/cases/{caseId}/documents

Protected, `multipart/form-data`. Adds one entry to the case's document log
and records an activity entry inline (`COMMENT_ADDED`/`REPORT_ADDED`/
`FILE_ADDED` depending on `type`). Same visibility check as viewing the
case.

**Form fields**
- `type`: `MESSAGE`, `REPORT`, or `FILE`.
- `title`: optional.
- `content`: required (non-blank) for `MESSAGE`/`REPORT`, ignored for `FILE`.
  Markdown source; stored verbatim.
- `file`: required for `FILE`, ignored otherwise. Allowed extensions: `jpg`,
  `jpeg`, `png`, `pdf`, `txt`. Max size 10MB. Stored on disk under a
  generated filename (not the original name) — see `originalFilename` in the
  response for what to show the user.

**Response `201 Created`**: same shape as one item from `GET
/api/cases/{caseId}/documents`.

**Errors**
- `400 INVALID_DOCUMENT` — missing content for a text type, or missing file for `FILE`.
- `400 INVALID_FILE` — disallowed extension or file too large.
- `404 CASE_NOT_FOUND`
- `401 UNAUTHORIZED`

## GET /api/cases/{caseId}/documents/{documentId}/file

Protected. Downloads a `FILE`-type document's content, same visibility
check as the case. Response is the raw file bytes with `Content-Type` set
to the stored content type and `Content-Disposition: attachment` using the
original filename.

**Errors**
- `404 CASE_NOT_FOUND` — case not visible, or no such file document.
- `401 UNAUTHORIZED`

## DELETE /api/cases/{caseId}/documents/{documentId}

Protected. Deletes one entry from the case's document log. Same visibility
check as viewing the case, plus a permission check: the caller must be the
document's original author, a `SUPERVISOR` in the case's own district, or
an `ADMIN`.

**Response `200 OK`**
```json
{ "message": "Document deleted." }
```

**Errors**
- `404 CASE_NOT_FOUND` — case not visible.
- `404 DOCUMENT_NOT_FOUND` — no such document on this case.
- `403 FORBIDDEN` — caller is not the document's author, and not a
  same-district supervisor or admin.
- `401 UNAUTHORIZED`

## Activity log

A generic, reusable mechanism (`AuditLogService`), not specific to cases:
an entry records an actor, an `entityType` + `entityId` (so it can attach to
any kind of record later, not just cases), an `action` string, optional
`details`, and a timestamp. `entityId` is stored as a `String` rather than
a numeric type, since it has to represent whatever a given entity's id type
happens to be (a `Long` for `entityType = "USER"`, a UUID string for
`entityType = "CASE"`). Every endpoint that changes something meaningful
records an entry via `AuditLogService.record(...)`, in the same transaction
as the change itself.

## POST /api/activity-log

Protected (any authenticated role). Records one activity log entry
attributed to the caller — the actor is always derived from the caller's
own JWT (`Authentication`), never from the request body, so a caller cannot
attribute an entry to a different user.

**Request body**
```json
{
  "entityType": "CASE",
  "entityId": "b2f1...-uuid",
  "action": "COMMENT_DELETED",
  "details": "Deleted a comment"
}
```
`entityType`, `entityId`, `action` required (non-blank); `details` optional.

**Response `201 Created`**
```json
{ "message": "Activity recorded." }
```

**Errors**
- `400 VALIDATION_ERROR`
- `401 UNAUTHORIZED`

## GET /api/reports/district-overview

Protected, `SUPERVISOR`/`ADMIN` only (`403 FORBIDDEN` otherwise), via a
class-level `@PreAuthorize("hasAnyRole('SUPERVISOR','ADMIN')")` on
`ReportController` — the whole controller is one role-gated area, so it
gets the class-level form rather than repeating the check per endpoint.
Case counts by status plus per-officer assigned-case counts for one
district.

- `SUPERVISOR` — always their own district; an optional `?district=` query
  param is accepted but ignored (the caller's own district always wins).
- `ADMIN` — their own district by default, or any district via
  `?district=WEST`.

**Response `200 OK`**
```json
{
  "district": "NORTH",
  "totalCases": 5,
  "openCases": 2,
  "inProgressCases": 1,
  "closedCases": 2,
  "officerWorkload": [
    { "username": "mibsen", "fullName": "Maria Ibsen", "assignedCaseCount": 3 },
    { "username": "kolsen", "fullName": "Kari Olsen", "assignedCaseCount": 0 }
  ]
}
```

`officerWorkload` includes every `OFFICER` in the district, even those with
zero assigned cases; it does not include `SUPERVISOR`/`ADMIN` accounts.
`totalCases` counts every case in the district, including ones with no
assigned officer.

**Errors**
- `403 FORBIDDEN` — caller is `OFFICER`.
- `401 UNAUTHORIZED`

## GET /api/users/search

Protected (any authenticated role). Finds active users by username or full
name substring match (case-insensitive), for the recipient picker used when
composing a message. Excludes the caller. Backs `RecipientPicker`, the
autocomplete used on `ConversationListPage` and `CaseDetailPage`'s "Send this case"
form, instead of requiring an exact known username.

**Query**: `?q=kari`

**Response `200 OK`**: array of `{id, username, fullName, district, role}`,
capped at 10 matches. Empty array for a blank `q`.

## POST /api/messages

Protected (any authenticated role). `multipart/form-data`. Sends a direct
message to another user by username, independent of any case — internal
personal comms, not tied to case records, but optionally linkable to one
and optionally carrying a single file attachment. Starts a new
`Conversation` with that recipient if one doesn't already exist between the
two users, or reuses the existing one — use
`POST /api/conversations/{id}/messages` to reply within an existing
conversation instead.

**Form fields**
- `recipientUsername` (required)
- `body` (required, non-blank, max 4000 chars)
- `relatedCaseId` (optional) — links the message to a case. The case must
  be visible to the sender (same visibility rules as `GET /api/cases/{id}`)
  or this is rejected as if the case didn't exist, same as it would for any
  other case the sender can't see.
- `attachment` (optional) — a single file, stored via the same
  `FileStorageService` case-document uploads use (no narrower extension
  allowlist than that).

**Response `201 Created`**
```json
{
  "id": "e7a4d2f1-...",
  "conversationId": "c4a1b2...",
  "senderUsername": "mibsen",
  "senderFullName": "Maria Ibsen",
  "recipientUsername": "kolsen",
  "recipientFullName": "Kari Olsen",
  "body": "Can you cover my shift?",
  "sentAt": "2026-09-04T09:00:00Z",
  "readAt": null,
  "relatedCaseId": "e7a4d2f1-9b3c-4a6e-8d0f-1c2b3a4d5e6f",
  "relatedCaseNumber": "NORTH-2026-0004",
  "relatedCaseTitle": "Break-in at Elm Street warehouse",
  "attachmentFilename": "notes.txt"
}
```
`relatedCaseId`/`relatedCaseNumber`/`relatedCaseTitle` and
`attachmentFilename` are all `null` when not set.

**Errors**
- `400 VALIDATION_ERROR` — missing `recipientUsername`/`body`
- `400 INVALID_MESSAGE` — blank body
- `400 INVALID_MESSAGE` — recipient is the sender
- `404 USER_NOT_FOUND` — no such recipient username
- `404 CASE_NOT_FOUND` — `relatedCaseId` doesn't exist, or isn't visible to the sender
- `400 INVALID_FILE` — attachment has a disallowed extension or is too large
- `401 UNAUTHORIZED`

## GET /api/messages/unread-count

Protected. `{ "unreadCount": 3 }` — count of the caller's received
messages with `readAt` still `null`. Backs the badge in `TopBar`/`Sidebar`.

## GET /api/conversations

Protected. Lists the caller's conversations, sorted by most recent message
first. Each entry is a preview, not the full thread — use
`GET /api/conversations/{id}/messages` for the full history.

**Response `200 OK`**
```json
[
  {
    "id": "b1e2c3d4-...",
    "otherParticipantUsername": "kolsen",
    "otherParticipantFullName": "Kari Olsen",
    "lastMessageBody": "Can you cover my shift?",
    "lastMessageSentAt": "2026-09-04T09:00:00Z",
    "lastMessageSenderUsername": "mibsen",
    "unreadCount": 1
  }
]
```

**Errors**
- `401 UNAUTHORIZED`

## GET /api/conversations/{id}/messages

Protected. Full message history for one conversation, oldest first. Same
response shape as `POST /api/messages`, as an array.

**Errors**
- `403 FORBIDDEN` — caller is not a participant in this conversation
- `404 CONVERSATION_NOT_FOUND`
- `401 UNAUTHORIZED`

## POST /api/conversations/{id}/messages

Protected. `multipart/form-data`. Sends a reply within an existing
conversation — the recipient is implied (the other participant), unlike
`POST /api/messages` which starts a new conversation by username.

**Form fields**
- `body` (required, non-blank, max 4000 chars)
- `relatedCaseId` (optional) — same rules as `POST /api/messages`
- `attachment` (optional) — same rules as `POST /api/messages`

**Response `201 Created`**: same shape as `POST /api/messages`.

**Errors**
- `400 VALIDATION_ERROR` — missing `body`
- `400 INVALID_MESSAGE` — blank body
- `403 FORBIDDEN` — caller is not a participant in this conversation
- `404 CONVERSATION_NOT_FOUND`
- `404 CASE_NOT_FOUND` — `relatedCaseId` doesn't exist, or isn't visible to the sender
- `400 INVALID_FILE` — attachment has a disallowed extension or is too large
- `401 UNAUTHORIZED`

## POST /api/conversations/{id}/read

Protected. Marks every currently-unread message in the conversation, where
the caller is the recipient, as read (sets `readAt`) — idempotent, a
no-op for messages already read. Called automatically by the frontend when
a conversation is opened; there is no manual "mark as read" action.

**Response `200 OK`**: array of the messages that were just marked read
(same shape as `POST /api/messages`), possibly empty if nothing was unread.

**Errors**
- `403 FORBIDDEN` — caller is not a participant in this conversation
- `404 CONVERSATION_NOT_FOUND`
- `401 UNAUTHORIZED`

## GET /api/messages/{id}/attachment

Protected. Returns the raw attachment bytes with the original filename
(`Content-Disposition`) and content type recorded at send time. Either the
sender or the recipient may download it — unlike
`POST /api/conversations/{id}/read`, this isn't recipient-only, since the
sender should be able to re-download what they sent.

**Errors**
- `403 FORBIDDEN` — caller is neither the sender nor the recipient
- `404 MESSAGE_NOT_FOUND` — no such message, or it has no attachment
- `401 UNAUTHORIZED`

## Password storage

Passwords are never stored or returned in plain text — the `User` entity
only keeps a one-way hash, in its `passwordHash` field.
