# ADR 0007 — Internal library upgrade policy

**Status:** Accepted (2025-11-03)

Minor/major bumps of internal libraries (`com.ardencm.*`) change wire formats consumed by
downstream teams (custody, clearing, regulatory reporting). Therefore:

1. Security fixes to internal libraries are released as **patch** versions on the affected
   minor line (e.g. `1.4.0 -> 1.4.1`) *and* as part of the next minor.
2. Consumers remediating a vulnerability MUST prefer the patch release. A minor bump requires a
   contract test run against the downstream consumer's golden samples and sign-off from the
   consumer's CODEOWNERS.
3. Wire-format changes must be documented in the library CHANGELOG under **Breaking**.
