(ns signal-integrity.constants
  "Physical and empirical constants for the `signal-integrity.*`
  signal-integrity formulas, extracted from kami-si's (retired Rust
  crate, kotoba-lang/kami-engine, ADR-2607010000) `const` definitions
  and inline magic numbers into pure EDN data
  (`resources/signal_integrity/constants.edn`).

  No network, no I/O in the domain sense: on the JVM the bundled classpath
  resource is read once at namespace load (a local, offline read of data
  shipped inside the library jar — not runtime I/O against the network or
  filesystem outside the artifact). cljs has no `clojure.java.io`/`slurp`,
  so it inlines an equivalent literal map instead of reading the resource.

  `constants.edn` is stored as a single-entity Datomic/Datascript tx-data
  vector (`[{:db/id -1 ...}]`) so it can be handed directly to
  `(d/transact conn (edn/read-string (slurp f)))`; the JVM branch strips
  that wrapper (`dissoc :db/id`) to recover the original flat map."
  #?@(:clj [(:require [clojure.edn :as edn]
                       [clojure.java.io :as io])]))

(def constants
  "All signal-integrity constants, namespaced by domain (`:tline/*`
  `:crosstalk/*` `:eye/*`). See `resources/signal_integrity/constants.edn`
  for provenance comments tying each value back to the Rust source it was
  extracted from."
  #?(:clj (dissoc (first (edn/read-string (slurp (io/resource "signal_integrity/constants.edn"))))
                   :db/id)
     :cljs {:tline/trace-thickness-mm 0.035
            :tline/c-mm-per-ps 0.2998
            :tline/microstrip-loss-base 0.001
            :tline/stripline-loss-base 0.0008
            :tline/coplanar-loss-base 0.0012

            :crosstalk/spacing-decay-mm 0.3
            :crosstalk/backward-coefficient 0.25
            :crosstalk/forward-coefficient 0.05
            :crosstalk/driver-source-impedance-ohm 50.0
            :crosstalk/driver-amplitude-mv 1000.0
            :crosstalk/saturation-floor-mm 0.1

            :eye/samples-per-bit 64
            :eye/lcg-seed -2401053089206496716
            :eye/lcg-multiplier 6364136223846793005
            :eye/lcg-increment 1442695040888963407
            :eye/xorshift32-seed 3405691582
            :eye/center-window-start 0.35
            :eye/center-window-end 0.65
            :eye/sigma-multiplier 3.0
            :eye/jitter-pp-multiplier 6.0
            :eye/default-q-factor 20.0}))
