# School ERP (OWASP ZAP practice target)

Spring Boot 4.0.9 (Java 25) backend + React 18 (Vite, TypeScript) frontend + Postgres 17.
API contract: [docs/API_CONTRACT.md](docs/API_CONTRACT.md).

## Run

```powershell
docker compose up -d                 # Postgres, data in ./database
cd backend; ./gradlew bootRun        # http://localhost:8088 (seeds demo data on first start)
cd frontend; pnpm install; pnpm build; pnpm preview  # production build on http://localhost:5173 (proxies /api -> 8088)
```

For UI development with hot reload use `pnpm dev` instead of `build` + `preview` (same URL).

## Logins (seed data)

| User | Password | Role |
|---|---|---|
| admin | Admin@1234 | ADMIN |
| teacher1..teacher3 | Teacher@1234 | TEACHER |
| student1..student12 | Student@1234 | STUDENT |

teacher1 owns "Grade 5 - A" (student1-4), teacher2 "Grade 5 - B" (student5-8), teacher3 "Grade 6 - A" (student9-12).

## Tests

```powershell
cd backend; ./gradlew test    # needs Docker (Testcontainers)
cd frontend; pnpm test
```

## ZAP pointers

- Target the frontend at `http://localhost:5173` (all API calls go through `/api`).
- OpenAPI spec for API scans: `http://localhost:8088/v3/api-docs` (Swagger UI at `/swagger-ui/index.html`).
- Auth: `POST /api/auth/login` with JSON `{"username","password"}` returns `accessToken` (15 min).
  Send it as `Authorization: Bearer <token>`. The refresh token is an HttpOnly cookie on `/api/auth`.
- Use different users (admin / teacher1 / student1) to test access control between roles.
- Reset the DB: stop compose, delete `./database`, start again.

## Backup / restore the database folder

Postgres must be stopped so the copy is consistent. Run these from the repo root in Git Bash.

```bash
# backup
docker compose stop postgres
tar -czf backups/database-$(date +%Y%m%d-%H%M%S).tar.gz database
docker compose up -d

# restore (replaces current data)
docker compose stop postgres
rm -rf database && tar -xzf backups/<file>.tar.gz
docker compose up -d
```
