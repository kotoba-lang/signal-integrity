(ns signal-integrity-test
  "Restoration-fidelity tests — one per original kami-si Rust test
  (kami-engine/kami-si/src/{lib,transmission_line,eye_diagram,crosstalk,
  s_param}.rs `mod tests`, deleted PR #82)."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [signal-integrity]
            [signal-integrity.transmission-line :as tline]
            [signal-integrity.eye-diagram :as eye]
            [signal-integrity.crosstalk :as crosstalk]
            [signal-integrity.s-param :as s-param]))

(deftest namespace-loads
  (testing "the restored CLJC namespace loads"
    (is (some? (the-ns 'signal-integrity)))))

;; mirrors `microstrip_z0_near_50_ohm` (transmission_line.rs)
(deftest microstrip-z0-near-50-ohm
  (let [t (tline/microstrip 0.2 0.2 4.3)
        params (tline/calculate-z0 t 25.0)]
    (is (and (> (:z0-ohm params) 30.0) (< (:z0-ohm params) 80.0)))
    (is (= 25.0 (:length-mm params)))))

;; mirrors `stripline_z0_positive` (transmission_line.rs)
(deftest stripline-z0-positive
  (let [t (tline/stripline 0.15 0.2 0.2 4.3)
        params (tline/calculate-z0 t 10.0)]
    (is (> (:z0-ohm params) 0.0))))

;; mirrors `eye_height_positive` (eye_diagram.rs)
(deftest eye-height-positive
  (let [data (eye/generate-eye-data 10.0 800.0 30.0 10.0 5.0 100)]
    (is (> (:eye-height-mv (:metrics data)) 0.0))
    (is (seq (:samples data)))))

;; mirrors `eye_width_bounded_by_bit_period` (eye_diagram.rs)
(deftest eye-width-bounded-by-bit-period
  (let [bit-rate 10.0
        data (eye/generate-eye-data bit-rate 800.0 30.0 10.0 5.0 50)
        bit-period (/ 1000.0 bit-rate)]
    (is (<= (:eye-width-ps (:metrics data)) bit-period))))

;; mirrors `crosstalk_coupling_decreases_with_spacing` (crosstalk.rs)
(deftest crosstalk-coupling-decreases-with-spacing
  (let [victim (tline/tline-params 50.0 7.0 0.001 20.0)
        aggressor (tline/tline-params 50.0 7.0 0.001 20.0)
        close (crosstalk/analyze-crosstalk victim aggressor 10.0 0.15 50.0)
        far (crosstalk/analyze-crosstalk victim aggressor 10.0 0.50 50.0)]
    (is (> (:peak-mv close) (:peak-mv far)))))

;; mirrors `crosstalk_has_positive_peak` (crosstalk.rs)
(deftest crosstalk-has-positive-peak
  (let [victim (tline/tline-params 50.0 7.0 0.001 20.0)
        aggressor (tline/tline-params 50.0 7.0 0.001 20.0)
        result (crosstalk/analyze-crosstalk victim aggressor 10.0 0.2 50.0)]
    (is (> (:peak-mv result) 0.0))))

(defn- sample-sparam []
  (s-param/s-parameter
   [0.1 1.0 5.0 10.0 20.0]
   [[0.05 -10.0] [0.08 -20.0] [0.15 -45.0] [0.25 -80.0] [0.40 -120.0]]
   [[0.98 -5.0] [0.95 -15.0] [0.80 -40.0] [0.55 -90.0] [0.30 -150.0]]
   [[0.01 170.0] [0.02 160.0] [0.03 140.0] [0.04 100.0] [0.05 60.0]]
   [[0.04 -8.0] [0.06 -18.0] [0.12 -40.0] [0.20 -70.0] [0.35 -110.0]]))

;; mirrors `touchstone_export_has_header` (s_param.rs)
(deftest touchstone-export-has-header
  (let [sp (sample-sparam)
        ts (s-param/export-touchstone sp)]
    (is (str/includes? ts "# GHz S MA R 50"))
    (let [data-lines (filter #(not (or (str/starts-with? % "!") (str/starts-with? % "#")))
                              (str/split-lines ts))]
      (is (= 5 (count data-lines))))))

;; mirrors `metrics_insertion_loss_negative_db` (s_param.rs)
(deftest metrics-insertion-loss-negative-db
  (let [sp (sample-sparam)
        m (s-param/metrics sp)]
    (is (< (:insertion-loss-db m) 0.0))))
