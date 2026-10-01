# Changelog

## 1.4.2 — SETTLE-4471 T+1 (EU/UK/CH)
- `SettlementCycle.standard(LocalDate)` / `forMarket(mic, LocalDate)`: T+2 for trade dates before 2027-10-11, T+1 from 2027-10-11.
- `SettlementDates.settlementDate(tradeDate, mic)` and the CLI resolve the cycle from the trade date.
- No-arg `standard()` / `forMarket(mic)` deprecated; still return the pre-go-live cycle so unmigrated callers do not change behaviour silently.
- No wire-format changes: tag 64 stays LocalMktDate `yyyyMMdd`; tag 63 follows the resolved cycle.

## 1.4.1 — 2026-09-03 (security)
- snakeyaml 1.33 -> 2.2 (CVE-2022-1471). No API or wire-format changes.

## 1.4.0 — 2026-02-11
- Added TARGET2 calendar and XSWX 2026 holidays.
- `SettlementCycle.standard()` remains T+2 for all EU/UK/CH markets (US/CA T+1 since 2024-05-28).
