# common-java-bom

Bill of materials imported by every Java service at Arden Capital Markets.

- `spring-boot.version` — the only approved Boot line.
- `core-dates.version` — internal business-day / settlement calendar library (`com.ardencm.posttrade:core-dates`).
- Pins under `dependencyManagement` override Boot's defaults; each pin must reference an ADR in `docs/adr/`.

Publishing: `mvn deploy` to `arden-artifactory` (libs-release-local). CI uses the shared `ci-shared-workflows/java-build.yml`.
