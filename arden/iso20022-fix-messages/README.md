# iso20022-fix-messages

Golden message samples and the canonical settlement-cycle mapping (`mapping/settlement-cycle.yaml`) for
outbound FIX 4.4 and ISO 20022 sese.023. EU/UK/CH regular way is T+1 from 2027-10-11 (tag 63 = 2); the pre-migration
mapping and sample are kept under `mapping/archive/` and `samples/fix/archive/` for back-dated trades.

- **Owner:** `@ardencm/messaging-standards`
- **Validate:** `python validate.py` · **Test:** `python -m pytest`
