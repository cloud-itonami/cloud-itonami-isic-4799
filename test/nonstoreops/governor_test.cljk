(ns nonstoreops.governor-test
  "Pure unit tests of `nonstoreops.governor/check` against hand-built
  proposals -- the fast, focused complement to `governor-contract-test`'s
  full-graph integration coverage."
  (:require [clojure.test :refer [deftest is testing]]
            [nonstoreops.advisor :as adv]
            [nonstoreops.governor :as gov]
            [nonstoreops.store :as store]))

(def seller-1 {:seller-id "seller-1" :name "North Loop Door-to-Door Route" :channel :door-to-door :registered? true :verified? true})
(def seller-3 {:seller-id "seller-3" :name "New Home-Demonstration Party-Plan Rep" :channel :party-plan :registered? true :verified? false})
(def vendor-1 {:vendor-id "vendor-1" :name "Northgate Household Goods Supply" :registered? true :verified? true})
(def vendor-2 {:vendor-id "vendor-2" :name "Unverified Import Broker Co." :registered? true :verified? false})

(defn- clean-proposal [op seller-id]
  {:op op :seller-id seller-id :summary "s" :rationale "routine non-store retail operations coordination"
   :cites [seller-id] :effect :propose :value {} :confidence 0.85})

(defn- clean-supply-order [seller-id vendor-id cost]
  (assoc (clean-proposal :coordinate-supply-order seller-id)
         :value {:seller-id seller-id :vendor-id vendor-id :estimated-cost cost}))

(deftest seller-unregistered-is-hard
  (testing "no seller record at all -> HARD hold"
    (let [s (store/mem-store {"seller-1" seller-1})
          verdict (gov/check {} nil (clean-proposal :log-sales-record "unknown-seller") s)]
      (is (true? (:hard? verdict)))
      (is (some #{:seller-unverified} (map :rule (:violations verdict)))))))

(deftest seller-unverified-is-hard
  (testing "seller registered but not yet verified -> HARD hold"
    (let [s (store/mem-store {"seller-3" seller-3})
          verdict (gov/check {} nil (clean-proposal :log-sales-record "seller-3") s)]
      (is (true? (:hard? verdict)))
      (is (some #{:seller-unverified} (map :rule (:violations verdict)))))))

(deftest vendor-missing-on-supply-order-is-hard
  (testing "supply-order proposal with no :vendor-id at all -> HARD hold"
    (let [s (store/mem-store {"seller-1" seller-1} {"vendor-1" vendor-1})
          verdict (gov/check {} nil (clean-supply-order "seller-1" nil 100.0) s)]
      (is (true? (:hard? verdict)))
      (is (some #{:vendor-unverified} (map :rule (:violations verdict)))))))

(deftest vendor-unregistered-on-supply-order-is-hard
  (testing "supply-order proposal naming an unknown vendor -> HARD hold"
    (let [s (store/mem-store {"seller-1" seller-1} {"vendor-1" vendor-1})
          verdict (gov/check {} nil (clean-supply-order "seller-1" "unknown-vendor" 100.0) s)]
      (is (true? (:hard? verdict)))
      (is (some #{:vendor-unverified} (map :rule (:violations verdict)))))))

(deftest vendor-unverified-on-supply-order-is-hard
  (testing "supply-order proposal naming a registered-but-unverified vendor -> HARD hold"
    (let [s (store/mem-store {"seller-1" seller-1} {"vendor-1" vendor-1 "vendor-2" vendor-2})
          verdict (gov/check {} nil (clean-supply-order "seller-1" "vendor-2" 100.0) s)]
      (is (true? (:hard? verdict)))
      (is (some #{:vendor-unverified} (map :rule (:violations verdict)))))))

(deftest vendor-verified-on-supply-order-is-not-hard-on-vendor-check
  (testing "supply-order proposal naming a verified vendor never trips :vendor-unverified"
    (let [s (store/mem-store {"seller-1" seller-1} {"vendor-1" vendor-1})
          verdict (gov/check {} nil (clean-supply-order "seller-1" "vendor-1" 100.0) s)]
      (is (empty? (filter #(= :vendor-unverified (:rule %)) (:violations verdict)))))))

(deftest vendor-check-is-scoped-to-supply-order-only
  (testing "non-supply-order ops never trip :vendor-unverified, even with no vendors registered at all"
    (let [s (store/mem-store {"seller-1" seller-1})]
      (doseq [op [:log-sales-record :schedule-route-operation :flag-compliance-concern]]
        (let [verdict (gov/check {} nil (clean-proposal op "seller-1") s)]
          (is (empty? (filter #(= :vendor-unverified (:rule %)) (:violations verdict)))
              (str "op " op " must never trip :vendor-unverified")))))))

(deftest effect-not-propose-is-hard
  (testing "any :effect other than :propose is a HARD, un-overridable block"
    (let [s (store/mem-store {"seller-1" seller-1})
          verdict (gov/check {} nil (assoc (clean-proposal :schedule-route-operation "seller-1") :effect :commit) s)]
      (is (true? (:hard? verdict)))
      (is (some #{:effect-not-propose} (map :rule (:violations verdict)))))))

(deftest op-outside-allowlist-is-hard
  (testing "an op outside the closed four-op allowlist is a scope violation"
    (let [s (store/mem-store {"seller-1" seller-1})
          verdict (gov/check {} nil (clean-proposal :finalize-cooling-off-waiver "seller-1") s)]
      (is (true? (:hard? verdict)))
      (is (some #{:op-not-allowed} (map :rule (:violations verdict)))))))

(deftest cooling-off-waiver-finalization-content-is-hard-and-permanent
  (testing "a proposal whose rationale touches finalizing the cooling-off waiver is HARD-blocked regardless of op/confidence"
    (let [s (store/mem-store {"seller-1" seller-1})
          poisoned (assoc (clean-proposal :log-sales-record "seller-1")
                          :rationale "finalized the cooling-off waiver during the doorstep visit and closed the sale immediately"
                          :confidence 0.99)
          verdict (gov/check {} nil poisoned s)]
      (is (true? (:hard? verdict)))
      (is (some #{:scope-excluded} (map :rule (:violations verdict)))))))

(deftest cancellation-right-sign-away-content-is-hard
  (testing "a proposal touching having the customer sign away their cancellation right is HARD-blocked"
    (let [s (store/mem-store {"seller-1" seller-1})
          poisoned (assoc (clean-proposal :schedule-route-operation "seller-1")
                          :summary "the route rep should have the customer sign away their cancellation right before leaving the doorstep")
          verdict (gov/check {} nil poisoned s)]
      (is (true? (:hard? verdict)))
      (is (some #{:scope-excluded} (map :rule (:violations verdict)))))))

(deftest waiver-confirmation-content-is-hard
  (testing "a proposal touching confirming the customer waived cooling-off is HARD-blocked"
    (let [s (store/mem-store {"seller-1" seller-1})
          poisoned (assoc (clean-proposal :log-sales-record "seller-1")
                          :rationale "confirmed the customer waived cooling-off before processing the payment at the door")
          verdict (gov/check {} nil poisoned s)]
      (is (true? (:hard? verdict)))
      (is (some #{:scope-excluded} (map :rule (:violations verdict)))))))

(deftest obtained-waiver-content-is-hard
  (testing "a proposal touching obtaining a waiver of the cancellation right is HARD-blocked"
    (let [s (store/mem-store {"seller-1" seller-1} {"vendor-1" vendor-1})
          poisoned (assoc (clean-supply-order "seller-1" "vendor-1" 100.0)
                          :summary "obtained a waiver of the cancellation right from the customer at the vending kiosk")
          verdict (gov/check {} nil poisoned s)]
      (is (true? (:hard? verdict)))
      (is (some #{:scope-excluded} (map :rule (:violations verdict)))))))

(deftest legitimate-compliance-concern-is-not-scope-excluded
  (testing "flagging an observed cooling-off/cancellation-right/route-dispute CONCERN (not a waiver finalization) never trips scope-exclusion -- this actor's core valid use case must not be self-blocked"
    (let [s (store/mem-store {"seller-1" seller-1})
          concern (assoc (clean-proposal :flag-compliance-concern "seller-1")
                         :value {:concern "consumer complaint about a possible cooling-off period violation during a doorstep sale, and a route-territory dispute with another seller"})
          verdict (gov/check {} nil concern s)]
      (is (empty? (filter #(= :scope-excluded (:rule %)) (:violations verdict)))
          "raw observation content (cooling-off/cancellation-right/route-dispute concerns) is exactly what this op exists to surface"))))

(deftest compliance-concern-always-escalates-clean
  (testing ":flag-compliance-concern is always high-stakes/escalate, even when otherwise clean and high confidence"
    (let [s (store/mem-store {"seller-1" seller-1})
          verdict (gov/check {} nil (assoc (clean-proposal :flag-compliance-concern "seller-1") :confidence 0.99) s)]
      (is (false? (:hard? verdict)))
      (is (true? (:high-stakes? verdict)))
      (is (true? (:escalate? verdict))))))

(deftest high-cost-supply-order-always-escalates
  (testing "a :coordinate-supply-order above the cost threshold is high-stakes/escalate, even when otherwise clean and high confidence"
    (let [s (store/mem-store {"seller-1" seller-1} {"vendor-1" vendor-1})
          expensive (assoc (clean-supply-order "seller-1" "vendor-1" 5000.0) :confidence 0.97)
          verdict (gov/check {} nil expensive s)]
      (is (false? (:hard? verdict)))
      (is (true? (:high-stakes? verdict)))
      (is (true? (:escalate? verdict))))))

(deftest low-cost-supply-order-does-not-force-escalate
  (testing "a :coordinate-supply-order at or below the cost threshold does not trip the high-cost escalate gate"
    (let [s (store/mem-store {"seller-1" seller-1} {"vendor-1" vendor-1})
          cheap (assoc (clean-supply-order "seller-1" "vendor-1" 420.0) :confidence 0.9)
          verdict (gov/check {} nil cheap s)]
      (is (false? (:hard? verdict)))
      (is (false? (:high-stakes? verdict)))
      (is (false? (:escalate? verdict))))))

;; ----------------------------- self-trip regression -----------------------------
;;
;; A known bug class in this actor fleet: the governor's own
;; scope-exclusion term list is sometimes phrased as a bare noun (e.g.
;; "cooling-off" or "cancellation right"), which then accidentally
;; matches inside the mock advisor's own DEFAULT rationale/disclaimer
;; text for a legitimate, allowed proposal -- causing the actor to
;; self-block its own happy path. This is a dedicated regression test:
;; every op the default mock advisor can generate, with default
;; (non-`out-of-scope?`) request patches, must NEVER trip
;; `:scope-excluded` or `:op-not-allowed`.
(deftest default-mock-advisor-proposals-never-self-trip-scope-exclusion
  (testing "the default mock advisor's own proposals for every allowed op never trip the governor's scope-exclusion check"
    (let [s (store/mem-store {"seller-1" seller-1} {"vendor-1" vendor-1})]
      (doseq [op [:log-sales-record :schedule-route-operation :coordinate-supply-order
                  :flag-compliance-concern]]
        (let [patch (if (= op :coordinate-supply-order)
                      {:item "door-to-door catalog restock" :estimated-cost 380.0 :vendor-id "vendor-1"}
                      {})
              proposal (adv/infer nil {:op op :seller-id "seller-1" :patch patch})
              verdict (gov/check {:seller-id "seller-1"} nil proposal s)]
          (is (empty? (filter #(= :scope-excluded (:rule %)) (:violations verdict)))
              (str "default advisor proposal for " op " must never self-trip :scope-excluded -- rationale/summary: "
                   (pr-str (select-keys proposal [:summary :rationale]))))
          (is (empty? (filter #(= :op-not-allowed (:rule %)) (:violations verdict)))
              (str "default advisor proposal for " op " must always be inside the closed op allowlist")))))))
