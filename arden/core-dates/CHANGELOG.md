# Changelog

## 1.4.2 — SETTLE-4471 T+1 tranche 1
- `SettlementCycle.forTrade(mic, tradeDate)`: XLON/XPAR/XETR/XSWX settle T+1 for trade dates on or after 2027-10-11, T+2 before. North America stays T+1; other markets unchanged.
- `SettlementDates.settlementDate(tradeDate, mic)` and the CLI default use the trade-date gate. FIX tag 63 follows the cycle (`2` for T+1).
- `SettlementCycle.standard()` / `forMarket(mic)` are deprecated and keep returning the pre-go-live cycle.
- No wire-format change: tag 64/75 stay LocalMktDate `yyyyMMdd`. Cut from `release/1.4` (the line pinned by common-java-bom), not from 1.5.0.

## 1.4.1 — 2026-09-03 (security)
- snakeyaml 1.33 -> 2.2 (CVE-2022-1471). No API or wire-format changes.

## 1.4.0 — 2026-02-11
- Added TARGET2 calendar and XSWX 2026 holidays.
- `SettlementCycle.standard()` remains T+2 for all EU/UK/CH markets (US/CA T+1 since 2024-05-28).
