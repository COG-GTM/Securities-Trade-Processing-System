# iso20022-fix-messages

Golden message samples and the canonical settlement-cycle mapping (`mapping/settlement-cycle.yaml`) for
outbound FIX 4.4 and ISO 20022 sese.023.

The mapping is effective-dated on trade date (SETTLE-4471): `mapping/settlement-cycle.yaml` applies from
2027-10-11 (EU/UK/CH T+1, tag 63 `2`); the pre-migration mapping and golden samples are archived under
`mapping/archive/` and `samples/archive/pre-2027-10-11/`. Tag 64/75 stay LocalMktDate `YYYYMMDD`.

- **Owner:** `@ardencm/messaging-standards`
- **Validate:** `python validate.py` · **Test:** `python -m pytest`
