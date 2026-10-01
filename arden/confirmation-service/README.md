# confirmation-service

Generates client trade confirmations and affirmation deadlines (FIX 4.4 + PDF).

- **Owner:** `@ardencm/post-trade-confirms` (see `CODEOWNERS`)
- **Runtime:** Java 17, Spring Boot 3.3.4, versions managed by `common-java-bom` 2026.3.0
- **Build:** `mvn -q verify` (CI: `ci-shared-workflows/java-build.yml@v3`, settings in `ci/settings.xml`)
- **Port:** see `src/main/resources/application.yml`

Confirmation narrative and PDF footer are templated in `application.yml`. Client-facing wording is reviewed by Legal.
