# settlement-instruction-service

Builds settlement instructions (ISO 20022 sese.023 and FIX 4.4) for custodians and CSDs.

- **Owner:** `@ardencm/settlements-core` (see `CODEOWNERS`)
- **Runtime:** Java 17, Spring Boot 3.3.4, versions managed by `common-java-bom` 2026.3.0
- **Build:** `mvn -q verify` (CI: `ci-shared-workflows/java-build.yml@v3`, settings in `ci/settings.xml`)
- **Port:** see `src/main/resources/application.yml`

Emits FIX 4.4 (tag 63/64) and ISO 20022 sese.023. `CustodyGatewayContractTest` is a consumer-driven contract owned by custody-gateway — a red build there means a custodian will reject our instructions.
