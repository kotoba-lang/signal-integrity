(ns signal-integrity
  "KAMI Signal Integrity — transmission line analysis, eye diagram
  generation, crosstalk analysis, and S-parameter extraction. Restored
  from the legacy kami-engine/kami-si Rust crate (deleted in
  kotoba-lang/kami-engine PR #82 'Remove Rust workspace from
  kami-engine') as part of the clj-wgsl migration (ADR-2607010930,
  com-junkawasaki/root).

  Named `signal-integrity` (not `si`) for clarity — the ledger's original
  short-hand note 'SI units' for this crate was ambiguous/wrong (this is
  Signal Integrity, an EDA discipline, not the physical SI unit system;
  same class of correction as the kami-dft 'DFT' ambiguity).

  One namespace per original Rust module:
    signal-integrity.transmission-line — microstrip/stripline/coplanar Z0 calc
    signal-integrity.eye-diagram       — eye diagram waveform + quality metrics
    signal-integrity.crosstalk         — near/far-end crosstalk between traces
    signal-integrity.s-param           — S-parameters + Touchstone .s2p export

  Zero-dep portable CLJC — pure data + pure functions, no IO/GPU.
  `eye-diagram`'s deterministic LCG PRNG uses u64-wraparound arithmetic
  (JVM `unchecked-*` ops on `long`, same bit pattern as Rust's
  `wrapping_mul`/`wrapping_add`).")
