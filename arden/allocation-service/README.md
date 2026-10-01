# allocation-service

Splits block trades into account-level allocations and stamps expected settlement dates.

- **Owner:** `@ardencm/post-trade-allocations` (see `CODEOWNERS`)
- **Runtime:** Java 17, Spring Boot 3.3.4, versions managed by `common-java-bom` 2026.3.0
- **Build:** `mvn -q verify` (CI: `ci-shared-workflows/java-build.yml@v3`, settings in `ci/settings.xml`)
- **Port:** see `src/main/resources/application.yml`

Give-up allocations use a calendar-day offset requested by ops in 2019; regular allocations use `core-dates`.
