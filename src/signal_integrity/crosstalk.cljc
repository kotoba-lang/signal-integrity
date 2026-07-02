(ns signal-integrity.crosstalk
  "Crosstalk analysis between adjacent transmission lines. Restored from
  kami-si's `crosstalk` module (deleted PR #82).")

(def coupling-types #{:forward :backward})

(defn crosstalk-result [victim-net aggressor-net coupling-type peak-mv width-ps]
  {:victim-net victim-net :aggressor-net aggressor-net :coupling-type coupling-type
   :peak-mv peak-mv :width-ps width-ps})

(defn analyze-crosstalk
  "Analyze crosstalk coupling between a `victim` and `aggressor`
  transmission line (`TLineParams` maps). Uses a simplified capacitive/
  inductive coupling model: backward (near-end) crosstalk depends on
  coupling length relative to signal rise time; forward (far-end)
  crosstalk depends on the difference in inductive and capacitive
  coupling coefficients. Returns the dominant coupling result."
  [victim aggressor coupling-length-mm spacing-mm rise-time-ps]
  (let [k-coupling (Math/exp (/ (- spacing-mm) 0.3))
        td (* (:delay-ps-per-mm victim) coupling-length-mm)
        kb (* k-coupling 0.25)
        saturation-length (/ rise-time-ps (* 2.0 (:delay-ps-per-mm victim)))
        effective-kb (if (> coupling-length-mm saturation-length)
                       kb
                       (* kb (/ coupling-length-mm saturation-length)))
        aggressor-amplitude-mv (/ (* 1000.0 (:z0-ohm aggressor)) (+ (:z0-ohm aggressor) 50.0))
        backward-peak (* effective-kb aggressor-amplitude-mv)
        backward-width (* 2.0 td)
        kf (* k-coupling 0.05 (/ coupling-length-mm (max saturation-length 0.1)))
        forward-peak (* kf aggressor-amplitude-mv)
        forward-width rise-time-ps]
    (if (>= backward-peak forward-peak)
      (crosstalk-result "victim" "aggressor" :backward backward-peak backward-width)
      (crosstalk-result "victim" "aggressor" :forward forward-peak forward-width))))
