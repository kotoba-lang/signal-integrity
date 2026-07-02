(ns signal-integrity.eye-diagram
  "Eye diagram generation and metrics extraction for serial link analysis.
  Restored from kami-si's `eye_diagram` module (deleted PR #82). Uses a
  deterministic LCG PRNG with u64 wraparound arithmetic (JVM `unchecked-*`
  ops on `long` give the same 2's-complement bit pattern as Rust's
  `wrapping_mul`/`wrapping_add` on `u64` — only interpretation as
  unsigned differs, handled explicitly where needed)."
  )

(defn eye-metrics [eye-height-mv eye-width-ps jitter-rms-ps jitter-pp-ps ber-estimate]
  {:eye-height-mv eye-height-mv :eye-width-ps eye-width-ps :jitter-rms-ps jitter-rms-ps
   :jitter-pp-ps jitter-pp-ps :ber-estimate ber-estimate})

(defn eye-diagram-data [samples metrics] {:samples samples :metrics metrics})

(defn- unsigned-long->double
  "Convert a `long` bit pattern to its unsigned interpretation as a double
  (standard `(x >>> 1) * 2 + (x & 1)` trick to avoid overflow)."
  [x]
  (+ (* (double (unsigned-bit-shift-right x 1)) 2.0) (double (bit-and x 1))))

(defn- lcg-next
  "One step of the LCG: returns `[value state']`, `value` in `[-1, 1]`."
  [state]
  (let [state' (unchecked-add (unchecked-multiply state 6364136223846793005) 1442695040888963407)
        u (unsigned-long->double state')
        v (- (* (/ u 1.8446744073709552E19) 2.0) 1.0)]
    [v state']))

(defn generate-eye-data
  "Generate eye diagram data for a serial link: overlaid bit-period
  waveform samples with noise and jitter, then computed eye-opening
  metrics."
  [bit-rate-gbps amplitude-mv rise-time-ps noise-rms-mv jitter-rms-ps num-bits]
  (let [bit-period-ps (/ 1000.0 bit-rate-gbps)
        samples-per-bit 64
        dt (/ bit-period-ps samples-per-bit)
        half-amp (/ amplitude-mv 2.0)
        tau (/ rise-time-ps 2.2)
        seed0 (unchecked-long 0xDEADBEEFCAFE1234N)]
    (let [[samples _seed _prev]
          (reduce
           (fn [[samples seed prev-level] bit-idx]
             (let [bit-val (if (zero? (mod (bit-xor (unchecked-multiply bit-idx 7) (unchecked-multiply bit-idx 13)) 3))
                              -1.0 1.0)
                   [jitter-rand seed] (lcg-next seed)
                   jitter-offset (* jitter-rand jitter-rms-ps)
                   [samples seed]
                   (reduce
                    (fn [[samples seed] s]
                      (let [t (+ (* s dt) jitter-offset)
                            t-wrapped (let [m (mod t bit-period-ps)] (if (neg? m) (+ m bit-period-ps) m))
                            transition (if (> (Math/abs (- bit-val prev-level)) 0.5)
                                         (let [alpha (- 1.0 (Math/exp (/ (- t-wrapped) tau)))]
                                           (+ prev-level (* (- bit-val prev-level) alpha)))
                                         bit-val)
                            [noise-rand seed] (lcg-next seed)
                            noise (* noise-rand noise-rms-mv)
                            voltage (+ (* transition half-amp) noise)]
                        [(conj samples [t-wrapped voltage]) seed]))
                    [samples seed]
                    (range samples-per-bit))]
               [samples seed bit-val]))
           [[] seed0 1.0]
           (range num-bits))
        center-start (* bit-period-ps 0.35)
        center-end (* bit-period-ps 0.65)
        center-samples (mapv second (filter (fn [[t _]] (and (>= t center-start) (<= t center-end))) samples))
        high-samples (filterv pos? center-samples)
        low-samples (filterv (complement pos?) center-samples)
        high-mean (if (empty? high-samples) half-amp (/ (reduce + 0.0 high-samples) (count high-samples)))
        low-mean (if (empty? low-samples) (- half-amp) (/ (reduce + 0.0 low-samples) (count low-samples)))
        high-sigma (if (> (count high-samples) 1)
                     (Math/sqrt (/ (reduce + 0.0 (map #(Math/pow (- % high-mean) 2) high-samples))
                                   (dec (count high-samples))))
                     noise-rms-mv)
        low-sigma (if (> (count low-samples) 1)
                    (Math/sqrt (/ (reduce + 0.0 (map #(Math/pow (- % low-mean) 2) low-samples))
                                  (dec (count low-samples))))
                    noise-rms-mv)
        eye-height (- (- high-mean (* 3.0 high-sigma)) (+ low-mean (* 3.0 low-sigma)))
        eye-width (- bit-period-ps (* 6.0 jitter-rms-ps))
        jitter-pp (* jitter-rms-ps 6.0)
        q (if (pos? (+ high-sigma low-sigma)) (/ (- high-mean low-mean) (+ high-sigma low-sigma)) 20.0)
        ber (* 0.5 (Math/exp (/ (* (- q) q) 2.0)))
        metrics (eye-metrics (max eye-height 0.0) (max eye-width 0.0) jitter-rms-ps jitter-pp ber)]
    (eye-diagram-data samples metrics))))
