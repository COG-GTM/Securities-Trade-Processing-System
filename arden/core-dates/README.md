# core-dates

Business-day calendars and settlement-date arithmetic for Arden Capital Markets post-trade services.

```java
LocalDate sd = SettlementDates.settlementDate(tradeDate, "XLON");   // market default cycle
String tag64 = FixDates.tag64(sd);                                   // yyyyMMdd
```

Calendars live in `src/main/resources/holidays.yaml` (owned jointly with market-ops). The YAML parser that loads them (snakeyaml) is shaded into the core-dates jar under `com.ardencm.posttrade.dates.internal.snakeyaml`, so its version is set by the core-dates release, not by the consuming service.

## CLI (ops runbooks / parity harness)

```bash
mvn -q package
java -jar target/core-dates-1.4.0-cli.jar [--cycle T_PLUS_1] < trades.csv
```

## Releases

Published to `arden-artifactory` as `com.ardencm.posttrade:core-dates`. Consumers take the version from `common-java-bom`.
See CHANGELOG.md. Upgrade policy: `common-java-bom/docs/adr/0007-internal-library-upgrade-policy.md`.
