# legacy-stored-procs

SQL Server stored-procedure pack behind the London settlements database (`SETTLE_LDN`). Deployed by DBA
change ticket, not CI. Three objects carry the settlement lag: `usp_CalcSettlementDate` (parameterised, default 2),
`usp_FlagLateSettlements` (hard-coded `DATEADD(day, 2, …)`) and `vw_SettlementCalendar` (hard-coded, statement only).

- **Owner:** `@ardencm/settlements-legacy-db`
- **Test:** none automated; DBA runs `tests/smoke.sql` against UAT.
