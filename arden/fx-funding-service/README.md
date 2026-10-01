# fx-funding-service

Schedules FX spot deals to fund cross-currency securities settlements.

- **Owner:** `@ardencm/treasury-tech` (see `CODEOWNERS`)
- **Runtime:** Java 17, Spring Boot 3.3.4, versions managed by `common-java-bom` 2026.3.0
- **Build:** `mvn -q verify` (CI: `ci-shared-workflows/java-build.yml@v3`, settings in `ci/settings.xml`)
- **Port:** see `src/main/resources/application.yml`

FX spot value date follows FX market convention (T+2), which is independent of securities settlement cycle. When securities settle before spot value, treasury deals tom-next.
