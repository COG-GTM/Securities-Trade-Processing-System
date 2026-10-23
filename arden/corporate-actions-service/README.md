# corporate-actions-service

Derives ex-dates and entitlement positions for dividends and rights from record dates.

- **Owner:** `@ardencm/asset-servicing` (see `CODEOWNERS`)
- **Runtime:** Java 17, Spring Boot 3.3.4, versions managed by `common-java-bom` 2026.3.0
- **Build:** `mvn -q verify` (CI: `ci-shared-workflows/java-build.yml@v3`, settings in `ci/settings.xml`)
- **Port:** see `src/main/resources/application.yml`

Ex-date derivation policy is in `docs/ex-date-policy.md`. CA-2291 tracks the open regime question for the T+1 transition.
