# UTETra audit remediation implementation plan

> For agentic workers: execute inline with executing-plans and TDD; one independent review after all fixes.

Goal: fix AUDIT-01..04 without changing existing roles, payment simulation contract or shared layout.
Architecture: keep current controllers/services; preserve GLOBAL product selection through scoped lookups, align LOCAL_QR cancellation policy, enforce expected-version for mutable catalog configuration, correct optional catalogKey validation.
Tech Stack: Java21, Boot3, JSP/JSTL, SiteMesh, Bootstrap5.3.3, JPA, MySQL-compatible DB.
Spec: docs/reviews/FULL_PROJECT_AUDIT_20261028.md; user explicitly approved implementing its fixes in the latest request.

Global constraints: stay on feature/order-payment in the requested D:/WEBCUOIKI checkout; preserve preexisting docs, .agents, skills-lock and deleted source QR image. No cloud writes/provider calls/commit/push. Keep a single shared layout. New Flyway migration only, never edit an applied migration.

Ruling: work in the existing requested order checkout rather than creating another worktree; no branch merge/publication is part of this fix. User has already selected this scope and authorized the audited approach.

## Tasks / ledger

- [x] AUDIT-02: add failing integration tests for paid solo/HOST LOCAL_QR using points; run RED; minimally accept LOCAL_QR cancellation refund state; run GREEN and assert no duplicate point credit after monetary refund.
- [x] AUDIT-03: add failing HTTP stale-form tests for category/topping/area using existing DTO + an extra JSON version field; run RED; add migration/version on entities and DTO/view, enforce expected-version, preserve omitted-version create compatibility, reject missing-version updates; run GREEN, migration and concurrent persistence checks.
- [x] AUDIT-01: add browser regression for GLOBAL selected-products promo rename preserving IDs; run RED; load scoped Admin products across stores, retain selected options and block save if lookup fails; run GREEN including all-products and lookup failure behavior.
- [x] AUDIT-04: add browser regression for blank optional catalogKey; run RED; remove required only for catalogKey; run GREEN including actual save/clear and invalid syntax.
- [x] Run full .\mvnw.cmd test, shared UI checker, native Tomcat/SiteMesh regression/browser flows and syntax/diff checks.
- [x] Request one independent read-only review of the final changes, handle substantiated blockers, record exact prompt/results/limitations in Markdown.

Review focus: stale serial form vs concurrent transactions; missing/null expected-version; lookup failure and missing product options; HOST points paid/refund idempotency; optional catalogKey clearing and old API clients omitting it.

Task AUDIT-02 evidence: loyalty-red.log fails at business refund policy for both paid points cases; loyalty-green.log BUILD SUCCESS after adding LOCAL_QR to cancelled prepaid refund policy. No source/provider/role changes outside the predicate.

RED evidence for AUDIT-01/04: browser cases report Existing GLOBAL selection lost: [] and Optional empty catalogKey is rejected by HTML validation. RED for AUDIT-03: stale category/topping/area and missing-version updates return200 instead of409.

Task AUDIT-03 evidence: catalog-version-red.log has four status200-vs409 failures; catalog-lock-red.log has three stale merge assertions that do not throw before @Version. catalog-version-green.log passes full CatalogIntegrationTest + IntegratedMigrationTest after schema/DTO/entity/service changes. Ruling: detached merge tests are provider optimistic-lock checks, not claimed as real two-transaction MySQL concurrency; H2/native-cloud limits remain explicit.

Task AUDIT-01/04 evidence: catalog-ui-red.json has global/key failures before production JS changes; final catalog-ui-green.log and .local/catalog-regressions/verification.json have global/key/version PASS. Global covers rename preservation, intentional all-products and lookup failure blocking; key covers actual blank edit/create; version covers stale browser submit409. Initial UI green attempt served old target resources (hash mismatch confirmed); Maven resources:resources synchronized compiled assets. Browser waits for editor reset after async save to avoid racing the UI reload.

Independent read-only reviewer: no Critical/Important found. Gaps retained: fallback IDs outside lookup and rapid scope switches have no dedicated browser case; category/area missing-version use same helper but direct HTTP missing-version case is topping; no independent concurrent MySQL transactions or cloud migration execution. No extra features added to conceal those limits.

Final backend verification: .\mvnw.cmd test exit0, 223 tests/0 failures/0 errors/0 skipped. Shared UI checker one layout21files, JS syntax and diff checks pass. Admin native UI17 checks pass; shopping is still running at this ledger entry.

Closure: shopping native UI18 checks pass, admin17 pass, catalog regressions3 pass; all plan tasks complete within stated test/DB limits. No commit/push or cloud migration applied.
