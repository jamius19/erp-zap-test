# School ERP – API Contract

Shared source of truth for backend and frontend. All paths are prefixed with `/api`.
JSON uses camelCase. Dates are `YYYY-MM-DD`. Timestamps are ISO-8601 UTC.

## Conventions

- **Errors:** RFC 9457 ProblemDetail, e.g. `{ "status": 404, "title": "Not Found", "detail": "Student not found" }`.
  Validation errors (400) add `"errors": { "field": "message" }`. 401 means no/invalid/expired token. 403 means wrong role or not owner.
- **Pagination** (list endpoints marked *paged*): query `page` (0-based), `size` (default 20, max 100), `sort` (e.g. `fullName,asc`).
  Response shape: `{ "content": [...], "page": { "size": 20, "number": 0, "totalElements": 42, "totalPages": 3 } }`
  (Spring Data `PagedModel` via `@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)`).
- **Search** `q` param is case-insensitive substring match. It must use parameterized queries only.

## Auth model

- Access token: JWT (HS256), 15 min. Sent as `Authorization: Bearer <token>`. Claims: `sub` = user id (string),
  `username`, `role` (`ADMIN|TEACHER|STUDENT`). Spring authority = `ROLE_<role>`.
- Refresh token: opaque random 256-bit value. The DB stores only its SHA-256 hex (`refresh_tokens.token_hash`).
  Sent as an HttpOnly cookie `refresh_token`, `SameSite=Strict`, `Path=/api/auth`, `Secure` per config, 7 days.
  **Rotated** on every refresh: the old token is revoked. Reusing a revoked token revokes all of that user's tokens (theft detection).
- Stateless sessions. CSRF is disabled because the API uses bearer tokens; the refresh cookie is SameSite=Strict and path-scoped.
- The frontend keeps the access token **in memory only** (not localStorage). On a 401 it calls `/auth/refresh` once, then retries.
- Passwords: BCrypt. Minimum 8 chars, with at least one letter and one digit.

### `UserSummary`
`{ id, username, fullName, email, role, hasPhoto }`

### Endpoints
| Method | Path | Who | Body / Notes |
|---|---|---|---|
| POST | /auth/login | public | `{username, password}` → `{accessToken, expiresIn, user: UserSummary}` + sets cookie. Wrong creds → 401 generic message. |
| POST | /auth/refresh | cookie | → same as login, new cookie. Missing/invalid → 401. |
| POST | /auth/logout | cookie | revokes the token, clears the cookie → 204 |
| GET | /auth/me | any auth | → `UserSummary` plus `teacherId` or `studentId` (nullable) |
| POST | /auth/change-password | any auth | `{currentPassword, newPassword}` → 204; revokes all refresh tokens |

## Users / profile photo
| Method | Path | Who | Notes |
|---|---|---|---|
| POST | /users/me/photo | any auth | multipart `file`; only image/png or image/jpeg (magic bytes are checked, not just the header); max 2MB; stored under `app.upload-dir` with a random UUID filename → 204 |
| GET | /users/{id}/photo | any auth | streams the image with the correct content type and `X-Content-Type-Options: nosniff`; 404 if none |

## Teachers
`Teacher`: `{ id, userId, username, fullName, email, employeeNo, phone }`

| Method | Path | Who | Notes |
|---|---|---|---|
| GET | /teachers | ADMIN, TEACHER | *paged*, `q` searches name/username/employeeNo |
| GET | /teachers/{id} | ADMIN, TEACHER | |
| POST | /teachers | ADMIN | `{username, password, fullName, email, employeeNo, phone}` → 201 Teacher |
| PUT | /teachers/{id} | ADMIN | `{fullName, email, employeeNo, phone}` |
| DELETE | /teachers/{id} | ADMIN | 204 (deletes the user too) |

## Students
`Student`: `{ id, userId, username, fullName, email, studentNo, dateOfBirth, guardianName, guardianPhone }`

| Method | Path | Who | Notes |
|---|---|---|---|
| GET | /students | ADMIN, TEACHER | *paged*, `q` searches name/username/studentNo |
| GET | /students/{id} | ADMIN, TEACHER, owning STUDENT | another student → 403 |
| POST | /students | ADMIN | `{username, password, fullName, email, studentNo, dateOfBirth, guardianName, guardianPhone}` → 201 |
| PUT | /students/{id} | ADMIN | same fields without username/password |
| DELETE | /students/{id} | ADMIN | 204 |

## Subjects
`Subject`: `{ id, code, name }`

| Method | Path | Who |
|---|---|---|
| GET | /subjects | any auth (not paged) |
| POST / PUT /subjects/{id} / DELETE /subjects/{id} | | ADMIN |

## Classes
`SchoolClass`: `{ id, name, gradeLevel, section, academicYear, teacherId, teacherName, studentCount }`

| Method | Path | Who | Notes |
|---|---|---|---|
| GET | /classes | any auth | ADMIN: all. TEACHER: all, with `?mine=true` filter. STUDENT: only enrolled classes. |
| GET | /classes/{id} | ADMIN, TEACHER, enrolled STUDENT | |
| POST | /classes | ADMIN | `{name, gradeLevel(1-12), section, academicYear("2026-2027"), teacherId?}` → 201 |
| PUT | /classes/{id} | ADMIN | same |
| DELETE | /classes/{id} | ADMIN | 204 |

"Class teacher" = the teacher whose `teachers.id == school_classes.teacher_id`.

## Enrollment
| Method | Path | Who | Notes |
|---|---|---|---|
| GET | /classes/{id}/students | ADMIN, TEACHER | → `Student[]` |
| POST | /classes/{id}/students | ADMIN, class teacher | `{studentId}` → 201; duplicate → 409 |
| DELETE | /classes/{id}/students/{studentId} | ADMIN, class teacher | 204 |

## Attendance (roll call)
`AttendanceRecord`: `{ id, classId, className, studentId, studentName, date, status }`, where status is `PRESENT|ABSENT|LATE|EXCUSED`.

| Method | Path | Who | Notes |
|---|---|---|---|
| GET | /classes/{id}/attendance?date=YYYY-MM-DD | ADMIN, TEACHER | records for that day (may be empty) |
| PUT | /classes/{id}/attendance | ADMIN, class teacher | `{date, records:[{studentId, status}]}` upserts; students must be enrolled (400 otherwise); future date → 400 → returns `AttendanceRecord[]` |
| GET | /students/{id}/attendance?from&to | ADMIN, TEACHER, owning STUDENT | `AttendanceRecord[]` sorted by date desc |

## Grades
`Grade`: `{ id, classId, className, studentId, studentName, subjectId, subjectName, term, score, remarks }`. Score is 0–100. Term examples: `TERM1`, `TERM2`, `FINAL`.

| Method | Path | Who | Notes |
|---|---|---|---|
| GET | /classes/{id}/grades?subjectId&term | ADMIN, TEACHER | filters optional |
| POST | /grades | ADMIN, class teacher | `{classId, studentId, subjectId, term, score, remarks}` → 201; student must be enrolled; duplicate → 409 |
| PUT | /grades/{id} | ADMIN, class teacher | `{score, remarks}` |
| DELETE | /grades/{id} | ADMIN, class teacher | 204 |
| GET | /students/{id}/grades | ADMIN, TEACHER, owning STUDENT | |

## Announcements
`Announcement`: `{ id, title, body, audience, classId, className, authorId, authorName, createdAt }`. Audience is `ALL|TEACHERS|STUDENTS`.
`body` is plain text. The frontend must render it as text and never as HTML.

| Method | Path | Who | Notes |
|---|---|---|---|
| GET | /announcements | any auth | *paged*, newest first. STUDENT sees ALL/STUDENTS where classId is null or a class they're enrolled in. TEACHER sees ALL/TEACHERS plus anything they authored. ADMIN sees all. |
| POST | /announcements | ADMIN, TEACHER | `{title(≤150), body(≤5000), audience, classId?}`. A TEACHER may only target a class they teach. → 201 |
| DELETE | /announcements/{id} | ADMIN or author | 204 |

## Dashboard
| GET | /dashboard | any auth | ADMIN: `{teachers, students, classes, subjects}` counts. TEACHER: `{myClasses, myStudents}`. STUDENT: `{myClasses, attendanceRate (0-100, null if none), averageScore (null if none)}` |

## Public / infra
- `GET /actuator/health` public
- `GET /v3/api-docs`, `/swagger-ui/**` public (lets ZAP import the OpenAPI spec)
- Everything else under `/api/**` requires auth.

## Seed data (when `app.seed.enabled=true` and the users table is empty)
| username | password | role |
|---|---|---|
| admin | Admin@1234 | ADMIN |
| teacher1..teacher3 | Teacher@1234 | TEACHER |
| student1..student12 | Student@1234 | STUDENT |
Subjects: MATH, ENG, SCI, HIST. Classes: "Grade 5 - A" (teacher1), "Grade 5 - B" (teacher2), "Grade 6 - A" (teacher3), year 2026-2027.
Students 1–4 go in 5A, 5–8 in 5B, 9–12 in 6A. Add some attendance, grades and 2 announcements.
