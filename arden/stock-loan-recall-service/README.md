# stock-loan-recall-service

Issues recall notices on lent securities so shares are back before the sale settles.

- **Owner:** `@ardencm/sec-lending` (see `CODEOWNERS`)
- **Runtime:** Java 17, Spring Boot 3.3.4, versions managed by `common-java-bom` 2026.3.0
- **Build:** `mvn -q verify` (CI: `ci-shared-workflows/java-build.yml@v3`, settings in `ci/settings.xml`)
- **Port:** see `src/main/resources/application.yml`

Recall notice period comes from GMSLA schedules (2 business days). Any change is a legal-agreement change, not just a config change.
