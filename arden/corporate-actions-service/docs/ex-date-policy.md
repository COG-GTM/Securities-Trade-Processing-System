# Ex-date derivation policy

Ex-date is derived from record date using the settlement cycle of the security's primary market:

| Regime | Ex-date |
|---|---|
| T+2 (EU/UK/CH today) | record date − 1 business day |
| T+1 (US/CA since 2024; EU/UK/CH from 11 Oct 2027) | record date |

Open question logged in CA-2291: for events **announced before** 11 Oct 2027 with **record dates after** it,
issuers' agents have not confirmed whether the published ex-date follows the old or new regime. Dual-listed
XSWX/XLON names are also unresolved because the two venues may publish different ex-dates during the
transition. Asset-servicing ops (not engineering) owns this decision.
