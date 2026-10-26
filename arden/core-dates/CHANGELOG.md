# Changelog

## 1.4.2 — unreleased (SETTLE-4471 T+1, tranche 1)
- `SettlementCycle.forMarket(mic, tradeDate)` / `standard(tradeDate)`: XLON, XPAR, XETR and XSWX trades dated on or after 2027-10-11 resolve to T+1; earlier trade dates stay T+2. Other markets unchanged.
- `SettlementDates.settlementDate(tradeDate, mic)` and the CLI default use the trade-date-gated cycle.
- `standard()` / `forMarket(mic)` are deprecated and keep returning the pre-go-live cycle.
- Cut from `release/1.4` (the line consumers run via the BOM); FIX tag 64/75 stay `yyyyMMdd`. Calendars unchanged (venue vs TARGET2 is escalated, see docs/t1-calendar-mismatch.md).

## 1.4.1 — 2026-09-03 (security)
- snakeyaml 1.33 -> 2.2 (CVE-2022-1471). No API or wire-format changes.

## 1.4.0 — 2026-02-11
- Added TARGET2 calendar and XSWX 2026 holidays.
- snakeyaml 1.33 is embedded in the jar, relocated to `com.ardencm.posttrade.dates.internal.snakeyaml` (PTI-212). Consumers cannot override its version; upgrade core-dates instead.
- `SettlementCycle.standard()` remains T+2 for all EU/UK/CH markets (US/CA T+1 since 2024-05-28).
