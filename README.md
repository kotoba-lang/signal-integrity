# kotoba-lang/signal-integrity

Zero-dep portable `.cljc` — restored from the legacy `kami-engine/kami-si`
Rust crate (deleted in kotoba-lang/kami-engine PR #82 "Remove Rust workspace
from kami-engine") as part of the **clj-wgsl migration** (ADR-2607010930,
`com-junkawasaki/root`).

KAMI Signal Integrity: transmission line analysis, eye diagram generation,
crosstalk analysis, and S-parameter extraction.

**Named `signal-integrity`, not `si`** — the migration ledger's original
short-hand note "SI units" for this crate was ambiguous/wrong (this is
Signal Integrity, an EDA discipline, not the physical SI unit system;
same class of correction as the `kami-dft` "DFT" ambiguity found earlier
in the migration).

| Namespace | Restored from | Purpose |
|---|---|---|
| `signal-integrity.transmission-line` | `transmission_line` | Microstrip/stripline/coplanar-waveguide characteristic-impedance (Z0) calculation |
| `signal-integrity.eye-diagram` | `eye_diagram` | Eye diagram waveform generation + quality metrics (height/width/jitter/BER) |
| `signal-integrity.crosstalk` | `crosstalk` | Near-end (backward) / far-end (forward) crosstalk between adjacent traces |
| `signal-integrity.s-param` | `s_param` | S-parameter summary metrics + Touchstone `.s2p` export |

## Status

Restored — all 4 modules ported from the original 512-line Rust source
(`lib.rs` + `transmission_line.rs` + `eye_diagram.rs` + `crosstalk.rs` +
`s_param.rs`), with all 8 original Rust unit tests mirrored 1:1 in
`test/signal_integrity_test.cljc` (+1 smoke test) — 9 tests / 12
assertions, 0 failures. Pure data + pure functions throughout; no IO/GPU.

`eye-diagram`'s deterministic LCG PRNG uses u64-wraparound arithmetic
(JVM `unchecked-multiply`/`unchecked-add` on `long` — same 2's-complement
bit pattern as Rust's `wrapping_mul`/`wrapping_add`; unsigned
interpretation for the `[0,1)` mapping uses the standard
`(x >>> 1) * 2 + (x & 1)` trick). `s-param`'s Touchstone formatting is
reader-conditional (`String/format` on JVM, `.toExponential`/`.toFixed`
on CLJS) for full portability.

## Develop

```bash
clojure -M:test
```
