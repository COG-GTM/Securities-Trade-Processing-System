# custody-gateway

Routes settlement instructions to custodians/CSDs and ingests MT548 / sese.024 status archives.

- **Owner:** `@ardencm/custody-integration` (see `CODEOWNERS`)
- **Runtime:** Java 17, Spring Boot 3.3.4, versions managed by `common-java-bom` 2026.3.0
- **Build:** `mvn -q verify` (CI: `ci-shared-workflows/java-build.yml@v3`, settings in `ci/settings.xml`)
- **Port:** see `src/main/resources/application.yml`

Forwards FIX to Euroclear/Clearstream/SIX SIS adapters and ingests MT548 tar archives from custodian SFTP drops. `contracts/` holds the consumer contracts we publish to upstream services.
