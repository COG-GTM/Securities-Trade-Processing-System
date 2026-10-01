# T+1 and calendar mismatches (open question)

Under T+2, a trading-venue closure that is not a TARGET2 closure (or vice versa) rarely changed
the settlement date because the second business day absorbed it. Under T+1 the settlement date
is the very next business day, so **which calendar is authoritative** — the venue's, the CSD's, or
the cash system's — becomes a business decision, not a code decision.

Known 2027 divergences in `holidays.yaml`:

| Date | XLON | XETR | XSWX | TARGET2 |
|---|---|---|---|---|
| 2027-05-17 (Whit Monday) | open | closed | closed | open |
| 2027-05-31 (UK Spring bank holiday) | closed | open | open | open |
| 2027-08-30 (UK Summer bank holiday) | closed | open | open | open |
| 2027-12-27 (Boxing Day substitute) | closed | open | open | open |

The EU/UK/CH T+1 industry task forces have not published a single harmonised rule. Any change to
`SettlementDates.settlementDate` for these dates requires sign-off from market-ops and the
Head of Post-Trade. Do not resolve this in code without that decision.
