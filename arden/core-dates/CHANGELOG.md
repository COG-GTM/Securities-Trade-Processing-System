# Changelog

## 1.4.0 — 2026-02-11
- Added TARGET2 calendar and XSWX 2026 holidays.
- snakeyaml 1.33 is embedded in the jar, relocated to `com.ardencm.posttrade.dates.internal.snakeyaml` (PTI-212). Consumers cannot override its version; upgrade core-dates instead.
- `SettlementCycle.standard()` remains T+2 for all EU/UK/CH markets (US/CA T+1 since 2024-05-28).
