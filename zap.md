# OWASP ZAP – Quick Guide (+ CASA AL1)

Practice target (erp-zap):
- UI: `http://localhost:5173`
- API: `http://localhost:8088`
- OpenAPI spec: `http://localhost:8088/v3/api-docs`
- Logins: `admin / Admin@1234`, `teacher1 / Teacher@1234`, `student1 / Student@1234`

---

## 1. Setup
1. Install desktop ZAP: https://www.zaproxy.org/download/
2. On start, choose "No, I do not want to persist this session" (fine for practice).
3. Install add-ons: puzzle-piece icon → **Marketplace**, then restart ZAP:
   - Active scanner rules (beta) and (alpha)
   - Passive scanner rules (beta)
   - DOM XSS Active scanner rule
   - JWT Support
   - OAST Support
   - Access Control Testing
4. OAST: **Tools → Options → OAST**, pick a service (e.g. Interactsh). The SSRF rule needs it.

## 2. Passive vs active scanning
| | Passive | Active (attack) |
|---|---|---|
| What it does | Reads traffic only | Sends attack payloads (XSS, SQLi, …) |
| Risk | None | Creates and deletes data, can break things |
| When it runs | Always, on everything you browse | Only when you start it |

Only active-scan systems you own or are authorized to test. Back up the database first.

## 3. Manual exploration (passive)
1. **Quick Start → Manual Explore**, enter `http://localhost:5173`, pick a browser, click **Launch Browser**.
2. Log in and click through **every** page and form.
3. Repeat for each role (admin, teacher, student).
4. Watch the **Sites** tree (what was found) and the **Alerts** tab (passive findings).

## 4. Spider
- Right-click the site in **Sites → Attack → Spider**. This is the classic spider: it follows links in HTML.
- Right-click → **Attack → AJAX Spider**. It drives a real browser and is required for SPAs (React/Angular/Vue).
- Spider while logged in (from the Manual Explore browser session) so protected pages are found.

## 5. Import the API
- **Import → Import an OpenAPI definition from a URL** → `http://localhost:8088/v3/api-docs`.
- Every endpoint appears in the Sites tree, including ones the UI never calls.

## 6. Active scan (authenticated)
1. **Get a token:** log in and copy `accessToken` from the login response (History tab → the login request → Response).
2. **Send it on every request:** **Tools → Options → Replacer → Add**
   - Match type: *Request Header (will add if not present)*
   - Match string: `Authorization`
   - Replacement: `Bearer <token>`
   - Enable it.
3. **Token expiry:** the token lasts 15 min. For long scans, start the backend with:
   ```powershell
   $env:APP_JWT_ACCESSTOKENTTL = "4h"; ./gradlew bootRun
   ```
4. **Run it:** right-click `http://localhost:8088` (and the UI host) → **Attack → Active Scan** → choose the policy (see §9) → **Start**.
5. **Check it was really logged in:** in **History**, sort by **Code**. Many 401/403 responses mean the token was missing or expired, so rescan.
6. **Afterwards:** restore the DB backup (the scan writes junk data).

## 7. Reading results
- **Alerts** tab, grouped by risk: High / Medium / Low / Informational. **Confidence** shows how sure ZAP is.
- Click an alert and check the **Request**, **Response** and **Evidence** panels to see what triggered it.
- Real issue: fix it, then re-test with right-click → **Open/Resend with Request Editor**.
- False positive: right-click → **Mark as False Positive**, and write down why.
  - Common example: CSRF alerts on token-based APIs, because the bearer header isn't sent automatically by browsers.
- **Report → Generate Report** (HTML/JSON) for a record of each run.

## 8. Access control by hand (ZAP won't find these automatically)
1. Log in as `student1` and put **their** token in the Replacer rule.
2. In History, pick admin or other-student requests, e.g.:
   - `GET /api/students`
   - `GET /api/students/{otherId}/grades`
   - `DELETE /api/classes/1`
3. Right-click → **Open/Resend with Request Editor** → **Send**.
4. Expected result is **403**. A 200 means broken access control (IDOR).
5. To automate later: **Access Control Testing** add-on. Define a Context with users, then mark which URLs each user may access.

---

## 9. CASA AL1 – what to do in ZAP

Source: ADA [CASA Test Guide](https://github.com/appdefensealliance/ASA-WG/blob/develop/CASA/CASA%20Test%20Guide.md), "DAST Guidance".

**Rules of the scan:**
- The scan must be **authenticated**. ADA's reference tool is Burp, but ZAP is OK if the lab accepts it.
- At AL1 the developer may run the scan.
- The scan must **not** report the issues below.

### Create the scan policy
1. **Analyse → Scan Policy Manager → Add**, name it `CASA`.
2. Set every rule to **OFF**.
3. Enable the active rules below with **Threshold: Low** and **Strength: High**.
4. Choose `CASA` when starting the Active Scan.

### Active rules
Status column: release = built in, beta/alpha = from the add-ons in §1.

| ID | Rule | Status | CASA test |
|---|---|---|---|
| 0 | Directory Browsing | release | 3.1.6 |
| 6 | Path Traversal | release | 5.1.10 |
| 7 | Remote File Inclusion | release | 5.1.10 |
| 20012 | Anti-CSRF Tokens Check | beta | 3.1.5 |
| 20014 | HTTP Parameter Pollution | beta | 5.1.1 |
| 20019 | External Redirect | release | 5.1.2 |
| 40012 | Cross Site Scripting (Reflected) | release | 5.1.7 |
| 40014 | Cross Site Scripting (Persistent) | release | 5.1.7 |
| 40016, 40017 | XSS (Persistent) – Prime / Spider (helpers for 40014) | release | 5.1.7 |
| 40026 | Cross Site Scripting (DOM Based) | DOM XSS add-on | 5.1.7 |
| 40018 | SQL Injection | release | 5.1.8 |
| 40022 | SQL Injection – PostgreSQL (Time Based)* | release | 5.1.8 |
| 90018 | Advanced SQL Injection | beta | 5.1.8 |
| 90020 | Remote OS Command Injection | release | 5.1.9 |
| 90037 | Remote OS Command Injection (Time Based) | release | 5.1.9 |
| 90019 | Server Side Code Injection | release | 5.1.3 |
| 90025 | Expression Language Injection | beta | 5.1.3 |
| 90035 | Server Side Template Injection | release | 5.1.4 |
| 90036 | Server Side Template Injection (Blind) | release | 5.1.4 |
| 40046 | Server Side Request Forgery (needs OAST) | beta | 5.1.5 |
| 90023 | XML External Entity Attack | release | 5.1.6 |
| 90021 | XPath Injection | release | 5.1.6 |
| 90017 | XSLT Injection | release | 5.1.6 |
| 40040 | CORS Header / CORS Misconfiguration | beta | 6.3.1 |
| 40036 | JWT Scan Rule (JWT Support add-on) | alpha | 2.3.4 |

\* Pick the time-based SQLi variant for your DB: MySQL 40019, Oracle 40021, MsSQL 40027, SQLite 40024.

### Passive rules (Tools → Options → Passive Scan Rules, make sure none is OFF)
| ID | Rule | CASA test |
|---|---|---|
| 3 | Session ID in URL Rewrite | 2.1.1 |
| 10024 | Information Disclosure – Sensitive Information in URL | 2.1.1 |
| 10010 | Cookie No HttpOnly Flag | 2.3.2 |
| 10011 | Cookie Without Secure Flag (only meaningful over HTTPS) | 2.3.1 |
| 10202 | Absence of Anti-CSRF Tokens | 3.1.5 |
| 10033 | Directory Browsing | 3.1.6 |
| 10028 | Off-site Redirect | 5.1.2 |

### Pass checklist
- Scanned while logged in, with few or no unexpected 401/403 in History
- UI spidered (AJAX Spider) **and** API imported (OpenAPI) and scanned
- OAST configured, so SSRF can be detected
- Final run against a production-like **HTTPS** deployment
- Zero alerts from the rules above, or each one marked false positive with a written reason
- HTML/JSON report exported as evidence
- Access control checked by hand per role (§8). Not a scan item, but CASA 3.1.1 / 3.1.4 need evidence for it.

### Where ZAP can't match Burp (tell the lab)
- **Code injection:** ZAP rule 90019 only tests PHP/ASP. Burp also tests server-side JS, Python, Ruby and Perl.
- **SQL injection:** no second-order (stored) SQL injection check.
- **Client-side checks:** no client-side HTTP parameter pollution or DOM-based XPath injection checks.

### Not covered by any scanner (AL1 needs written evidence or code snippets)
- Brute-force protection on login (1.1.1)
- MFA for admin interfaces (3.3.1)
- Logout and session invalidation, "end all sessions", token expiry ≤ 24 h (2.2.x)
- TLS 1.2+ and valid certificates (4.1.x): use Qualys SSL Labs
- Dependency vulnerability scan (6.1.1): OWASP Dependency-Check, `pnpm audit`
- No secrets or credentials in logs or source (6.5.1, 6.7.1)
- Webhook requirements (7.x), if you have webhooks
