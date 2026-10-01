POSITION KEEPER
===============
Nightly position roll for the London books. Ant build (ant jar), runs on JDK 8.
No automated tests - regression is done by comparing dist output with the previous night's file (see ops runbook OPS-RB-31).
Settlement dates are computed locally (SettleDateUtil) - this predates core-dates.

Owner: settlements-legacy (see CODEOWNERS). Contact: #legacy-position-keeper
