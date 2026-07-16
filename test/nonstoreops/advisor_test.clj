(ns nonstoreops.advisor-test
  "Unit tests of `nonstoreops.advisor` proposal generation."
  (:require [clojure.test :refer [deftest is testing]]
            [nonstoreops.advisor :as adv]
            [nonstoreops.store :as store]))

(def db (store/seed-db))

(deftest propose-sales-record-shape
  (testing "sales-record proposal has correct shape and fields"
    (let [p (adv/infer db {:op :log-sales-record
                           :seller-id "seller-1"
                           :patch {:units-sold 12 :collections 5}})]
      (is (= :log-sales-record (:op p)))
      (is (= "seller-1" (:seller-id p)))
      (is (= :propose (:effect p)))
      (is (<= 0 (:confidence p) 1))
      (is (map? (:value p)))
      (is (contains? (:value p) :seller-id)))))

(deftest propose-route-operation-shape
  (testing "route-operation proposal has correct shape"
    (let [p (adv/infer db {:op :schedule-route-operation
                           :seller-id "seller-2"
                           :patch {:route "riverside-loop-4" :date "2026-07-20"}})]
      (is (= :schedule-route-operation (:op p)))
      (is (= "seller-2" (:seller-id p)))
      (is (= :propose (:effect p))))))

(deftest propose-supply-order-shape
  (testing "supply-order proposal has correct shape"
    (let [p (adv/infer db {:op :coordinate-supply-order
                           :seller-id "seller-1"
                           :patch {:item "door-to-door catalog restock" :quantity 100 :estimated-cost 380.0
                                   :vendor-id "vendor-1"}})]
      (is (= :coordinate-supply-order (:op p)))
      (is (= :propose (:effect p)))
      (is (string? (:summary p)))
      (is (= "vendor-1" (get-in p [:value :vendor-id]))))))

(deftest propose-compliance-concern-shape
  (testing "compliance-concern proposal always escalates"
    (let [p (adv/infer db {:op :flag-compliance-concern
                           :seller-id "seller-1"
                           :patch {:concern "possible cooling-off period violation at the door"}})]
      (is (= :flag-compliance-concern (:op p)))
      (is (= :propose (:effect p)))
      (is (string? (:summary p))))))

(deftest all-proposals-effect-is-always-propose
  (testing "every proposal type has :effect :propose, never direct actuation"
    (doseq [op [:log-sales-record :schedule-route-operation :coordinate-supply-order
                :flag-compliance-concern]]
      (let [p (adv/infer db {:op op :seller-id "seller-1" :patch {}})]
        (is (= :propose (:effect p))
            (str "op " op " must have :effect :propose"))))))

(deftest rationale-string-is-present
  (testing "every proposal has a rationale explaining the advisor's thinking"
    (doseq [op [:log-sales-record :schedule-route-operation :coordinate-supply-order
                :flag-compliance-concern]]
      (let [p (adv/infer db {:op op :seller-id "seller-1" :patch {}})]
        (is (string? (:rationale p))
            (str "op " op " must have a :rationale string"))))))
