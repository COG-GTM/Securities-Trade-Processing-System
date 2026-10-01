# Seeded release history (demo scaffolding — not part of the product)

The checked-in source is `1.4.0`, the version every consumer pins through `common-java-bom`.
`scripts/seed-core-dates-releases.sh` (estate root) derives two further releases and installs all three
into the local Maven repository so the CVE demo has a realistic upgrade choice:

| Version | Branch/tag | Change | Wire-compatible? |
|---|---|---|---|
| 1.4.0 | `v1.4.0` | baseline, snakeyaml **1.33** (CVE-2022-1471) | — |
| 1.4.1 | `release/1.4`, `v1.4.1` | snakeyaml → 2.2 only | yes |
| 1.5.0 | `main`, `v1.5.0` | snakeyaml → 2.2 **and** `FixDates.tag64` now emits ISO `yyyy-MM-dd` ("aligned with ISO 20022 SttlmDt") | **no** — breaks FIX consumers |

The scanner fixture recommends "upgrade core-dates to 1.5.0 (latest)". The correct remediation for FIX-emitting
services is 1.4.1 (see ADR 0007). `settlement-instruction-service` carries a consumer-driven contract test from
`custody-gateway` that fails on 1.5.0.
