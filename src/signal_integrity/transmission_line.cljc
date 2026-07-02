(ns signal-integrity.transmission-line
  "Transmission line parameter calculation for microstrip, stripline, and
  coplanar waveguide. Restored from kami-si's `transmission_line` module
  (kami-engine/kami-si/src/transmission_line.rs, deleted PR #82).")

(def ^:private trace-thickness-mm 0.035) ; 1 oz copper
(def ^:private c-mm-per-ps 0.2998) ; speed of light

;; TLineType variants
(defn microstrip [width height er] {:kind :microstrip :width width :height height :er er})
(defn stripline [width height1 height2 er] {:kind :stripline :width width :height1 height1 :height2 height2 :er er})
(defn coplanar [width gap height er] {:kind :coplanar :width width :gap gap :height height :er er})

(defn tline-params [z0-ohm delay-ps-per-mm loss-db-per-mm length-mm]
  {:z0-ohm z0-ohm :delay-ps-per-mm delay-ps-per-mm :loss-db-per-mm loss-db-per-mm :length-mm length-mm})

(defn calculate-z0
  "Calculate transmission line parameters from `tline-type` geometry.
  Microstrip uses the IPC-2141 formula; stripline and coplanar use
  simplified closed-form approximations."
  [tline-type length-mm]
  (case (:kind tline-type)
    :microstrip
    (let [{:keys [width height er]} tline-type
          t trace-thickness-mm
          z0 (* (/ 87.0 (Math/sqrt (+ er 1.41))) (Math/log (/ (* 5.98 height) (+ (* 0.8 width) t))))
          eff-er (+ (/ (+ er 1.0) 2.0) (/ (- er 1.0) (* 2.0 (Math/sqrt (+ 1.0 (/ (* 12.0 height) width))))))
          delay (/ (Math/sqrt eff-er) c-mm-per-ps)
          loss (* 0.001 (+ 1.0 (/ 1.0 z0)))]
      (tline-params z0 delay loss length-mm))

    :stripline
    (let [{:keys [width height1 height2 er]} tline-type
          b (+ height1 height2)
          t trace-thickness-mm
          z0 (* (/ 60.0 (Math/sqrt er)) (Math/log (/ (* 4.0 b) (* 0.67 Math/PI (+ (* 0.8 width) t)))))
          delay (/ (Math/sqrt er) c-mm-per-ps)
          loss (* 0.0008 (+ 1.0 (/ 1.0 z0)))]
      (tline-params z0 delay loss length-mm))

    :coplanar
    (let [{:keys [width gap height er]} tline-type
          k (/ width (+ width (* 2.0 gap)))
          eff-er (+ 1.0 (* (/ (- er 1.0) 2.0) (/ 1.0 (Math/sqrt (+ 1.0 (* 0.7 (/ width height)))))))
          z0 (* (/ (* 30.0 Math/PI) (Math/sqrt eff-er)) (Math/log (/ 1.0 k)))
          delay (/ (Math/sqrt eff-er) c-mm-per-ps)
          loss (* 0.0012 (+ 1.0 (/ 1.0 z0)))]
      (tline-params z0 delay loss length-mm))))
