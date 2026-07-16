(ns nonstoreops.governor
  "NonStoreRetailGovernor -- the independent compliance layer that earns
  the NonStoreRetailAdvisor the right to commit. The advisor has no
  notion of whether a door-to-door route / vending-machine fleet /
  party-plan seller is actually registered and license-verified, whether
  a named supply-order vendor is itself a registered/verified
  counterparty, whether its own proposed `:effect` secretly claims a
  direct actuation instead of a mere proposal, or whether it has
  silently drifted into finalizing a waiver of a consumer's statutory
  cooling-off/cancellation right, so this MUST be a separate system able
  to *reject* a proposal and fall back to HOLD.

  This actor's scope is deliberately narrow -- COORDINATION ONLY
  (sale/order transaction logging, door-to-door-route/vending-machine-
  fleet scheduling, inventory supply-order coordination,
  compliance-concern flagging). It NEVER performs or authorizes:
    - setting or overriding a unit price
    - directly finalizing a waiver of a consumer's statutory cooling-off/
      cancellation right (obtaining, confirming, executing, or otherwise
      closing out such a waiver)
    - any other consumer-rights-waiver or route/territory-dispute
      adjudication authority

  Four HARD checks, ALL permanent, un-overridable by any human approval:

    1. Seller unverified          -- the target door-to-door route /
                                     vending-machine-fleet / party-plan
                                     seller record must exist AND be
                                     independently confirmed
                                     `:registered?`/`:verified?` in the
                                     store before ANY proposal for it may
                                     commit or even escalate. Never trusts
                                     a proposal's own claim about the
                                     seller -- re-derived from the
                                     seller's own record, the same
                                     'ground truth, not self-report'
                                     discipline every sibling actor's
                                     governor uses. This is this
                                     vertical's primary gate: there is no
                                     fixed storefront to verify, so
                                     seller/route/fleet registration is
                                     the analog every other 47xx actor
                                     spends on store verification.
    2. Vendor unverified          -- for `:coordinate-supply-order` ONLY,
                                     the proposal's own drafted `:value`
                                     must name a `:vendor-id` that
                                     resolves to an independently
                                     `:registered?`/`:verified?` vendor
                                     record. A missing vendor-id, or one
                                     that resolves to an unregistered or
                                     unverified vendor, is a HARD block.
    3. Effect not :propose        -- every proposal's `:effect` MUST be
                                     `:propose`. Any other effect value
                                     is, by construction, a claim to
                                     directly actuate/commit outside
                                     governance -- HARD block, not merely
                                     low-confidence.
    4. Scope exclusion            -- ANY proposal (regardless of op)
                                     whose op, summary, rationale, cites
                                     or draft value touches directly
                                     finalizing a waiver of a consumer's
                                     statutory cooling-off/cancellation
                                     right is a HARD, PERMANENT block --
                                     this actor's charter excludes that
                                     territory structurally, not as a
                                     rollout milestone. Evaluated
                                     UNCONDITIONALLY on every proposal. An
                                     op outside the closed four-op
                                     allowlist is the SAME failure mode
                                     (an advisor proposing something it
                                     was never authorized to propose) and
                                     is folded into this same check.
                                     `:flag-compliance-concern` itself is
                                     NEVER excluded by this check --
                                     surfacing a consumer complaint,
                                     cooling-off/cancellation-right
                                     concern, or route/territory dispute
                                     for a human is exactly this actor's
                                     job; only FINALIZING a waiver of that
                                     right is excluded (see
                                     `scope-excluded-terms` below --
                                     phrased as the finalization/execution
                                     ACTION, never a bare noun like
                                     'cooling-off' or 'cancellation
                                     right', so the default mock
                                     advisor's own
                                     `:flag-compliance-concern` rationale
                                     never self-trips this check).

  Two ESCALATE (SOFT) gates, either forces human sign-off:
    - LLM confidence below the floor.
    - The op is `:flag-compliance-concern` -- ALWAYS escalates to a
      human, regardless of confidence, regardless of how clean the
      proposal otherwise is. `nonstoreops.phase` independently agrees:
      `:flag-compliance-concern` is never a member of any phase's
      `:auto` set either -- two layers, not one. This op can NEVER
      auto-commit-eligible, by design: it is the actor's sole channel
      for surfacing a cooling-off/cancellation-right concern, and this
      class's consumer-protection dimension means that channel always
      needs a human, never an auto-commit.
    - A `:coordinate-supply-order` whose drafted `:value` names an
      `:estimated-cost` above `supply-cost-threshold` -- a large-value
      inventory procurement proposal always needs a human sign-off, even
      when the governor and phase would otherwise allow auto-commit."
  (:require [clojure.string :as str]
            [nonstoreops.store :as store]))

(def confidence-floor 0.6)

(def supply-cost-threshold
  "Example single-seller inventory-procurement threshold
  (USD-equivalent units, domain-illustrative -- not a universal
  cross-domain constant). A `:coordinate-supply-order` proposal citing an
  `:estimated-cost` above this value ALWAYS escalates to human sign-off,
  regardless of confidence or rollout phase."
  1000.0)

(def allowed-ops
  "The closed proposal-op allowlist -- an op outside this set is a scope
  violation by construction (see `scope-exclusion-violations`)."
  #{:log-sales-record :schedule-route-operation
    :coordinate-supply-order :flag-compliance-concern})

(def always-escalate-ops
  "Ops that ALWAYS require human sign-off, clean or not. This actor never
  auto-commits a compliance-concern flag -- the consumer-protection
  dimension of this ISIC class (door-to-door/party-plan sales are
  heavily regulated against high-pressure tactics in most jurisdictions)
  means a human always reviews it."
  #{:flag-compliance-concern})

(def scope-excluded-terms
  "Case-insensitive substrings that mark a proposal as touching a
  permanently out-of-scope decision area -- directly finalizing a waiver
  of a consumer's statutory cooling-off/cancellation right, or otherwise
  closing out such a waiver, rather than merely flagging a compliance
  concern for a human. Scanned across the proposal's
  op/summary/rationale/cites/value, never trusting the advisor's own
  framing of its intent.

  CRITICAL: every term here is phrased as the finalization/execution
  ACTION (e.g. 'finalized the cooling-off waiver', 'have the customer
  sign away their cancellation right'), never a bare noun like
  'cooling-off', 'cancellation right' or 'compliance concern' -- a bare
  noun would accidentally match inside this actor's own legitimate
  `:flag-compliance-concern` default proposal text (whose whole job is
  to talk about cooling-off/cancellation-right consumer concerns) and
  self-block the happy path. See
  `nonstoreops.governor-test/default-mock-advisor-proposals-never-self-trip-scope-exclusion`
  for the regression test."
  ["finalize the cooling-off waiver" "finalized the cooling-off waiver"
   "finalizes the cooling-off waiver" "finalizing the cooling-off waiver"
   "finalize the cancellation-right waiver" "finalized the cancellation-right waiver"
   "finalizing the cancellation-right waiver"
   "waive the customer's cooling-off right" "waived the customer's cooling-off right"
   "waiving the customer's cooling-off right"
   "waive the customer's cancellation right" "waived the customer's cancellation right"
   "waiving the customer's cancellation right"
   "have the customer sign away their cancellation right" "had the customer sign away their cancellation right"
   "have the customer sign away the cancellation right" "had the customer sign away the cancellation right"
   "get the customer to sign away their cancellation right" "got the customer to sign away their cancellation right"
   "sign away the cooling-off period" "signed away the cooling-off period" "signing away the cooling-off period"
   "sign away their cancellation right" "signed away their cancellation right" "signing away their cancellation right"
   "confirm the customer waived cooling-off" "confirmed the customer waived cooling-off"
   "confirming the customer waived cooling-off"
   "obtain a waiver of the cancellation right" "obtained a waiver of the cancellation right"
   "obtaining a waiver of the cancellation right"
   "execute the cooling-off waiver" "executed the cooling-off waiver" "executing the cooling-off waiver"
   "クーリングオフ権を放棄させた" "クーリングオフの放棄を確定させた" "解約権の放棄を確定させた"
   "解約権を放棄させた" "クーリングオフ期間の放棄に同意させた" "クーリングオフを放棄させた"])

;; ----------------------------- checks -----------------------------

(defn- seller-unverified-violations
  "The target seller (door-to-door route / vending-machine fleet /
  party-plan representative) must exist AND be independently
  `:registered?`/`:verified?` in the store -- never trust the
  proposal's own `:seller-id` claim without a store lookup."
  [{:keys [seller-id]} st]
  (let [s (store/seller-record st seller-id)]
    (when-not (and s (:registered? s) (:verified? s))
      [{:rule :seller-unverified
        :detail (str seller-id " は未登録または未検証の販売者/ルート/自動販売機フリート -- いかなる提案も進められない")}])))

(defn- vendor-unverified-violations
  "For `:coordinate-supply-order` ONLY, the proposal's own drafted
  `:value` must name a `:vendor-id` that resolves to an independently
  `:registered?`/`:verified?` vendor record. A missing vendor-id, or one
  that resolves to an unregistered/unverified vendor, is a HARD block --
  never trust the proposal's own vendor claim without a store lookup, the
  SAME 'ground truth, not self-report' discipline as
  `seller-unverified-violations`, reapplied to the supply-chain
  counterparty."
  [proposal st]
  (when (= :coordinate-supply-order (:op proposal))
    (let [vendor-id (get-in proposal [:value :vendor-id])
          v (and vendor-id (store/vendor-record st vendor-id))]
      (when-not (and v (:registered? v) (:verified? v))
        [{:rule :vendor-unverified
          :detail (str (or vendor-id "(vendor-id missing)")
                        " は未登録または未検証の仕入先 -- 発注調整提案を進められない")}]))))

(defn- effect-not-propose-violations
  "`:effect` must ALWAYS be `:propose` -- any other value is a claim to
  directly actuate/commit outside governance."
  [proposal]
  (when (not= :propose (:effect proposal))
    [{:rule :effect-not-propose
      :detail (str ":effect は :propose のみ許可されるが " (pr-str (:effect proposal)) " が提案された")}]))

(defn- text-blob
  "Flatten every advisor-authored field on a proposal into one lower-cased
  blob the scope-exclusion scan checks."
  [proposal]
  (str/lower-case (pr-str (select-keys proposal [:op :summary :rationale :cites :value]))))

(defn- scope-exclusion-violations
  "HARD, PERMANENT block: a proposal outside the closed op allowlist, or
  one whose content touches directly finalizing a waiver of a consumer's
  statutory cooling-off/cancellation right, regardless of confidence or
  how clean every other check is. Evaluated UNCONDITIONALLY on every
  proposal."
  [proposal]
  (let [op (:op proposal)
        blob (text-blob proposal)]
    (cond
      (not (contains? allowed-ops op))
      [{:rule :op-not-allowed
        :detail (str (pr-str op) " は許可された操作(closed allowlist)に含まれない")}]

      (some #(str/includes? blob %) scope-excluded-terms)
      [{:rule :scope-excluded
        :detail "消費者の法定クーリングオフ/解約権の放棄を確定させる行為(consumer cooling-off/cancellation-right-waiver finalization)に触れる提案は永久に禁止"}])))

(defn- high-cost-supply-order?
  "A `:coordinate-supply-order` proposal citing an `:estimated-cost` above
  `supply-cost-threshold` -- always needs human sign-off (SOFT escalate,
  not a hard block: the order itself is in scope, only its size requires
  a human)."
  [proposal]
  (and (= :coordinate-supply-order (:op proposal))
       (some-> proposal :value :estimated-cost (> supply-cost-threshold))))

(defn check
  "Censors a NonStoreRetailAdvisor proposal against the governor rules.
  Returns {:ok? bool :violations [..] :confidence c :escalate? bool
  :high-stakes? bool :hard? bool}."
  [request _context proposal store]
  (let [seller-id (or (:seller-id proposal) (:seller-id request))
        hard (into []
                   (concat (seller-unverified-violations {:seller-id seller-id} store)
                           (vendor-unverified-violations proposal store)
                           (effect-not-propose-violations proposal)
                           (scope-exclusion-violations proposal)))
        conf (:confidence proposal 0.0)
        low? (< conf confidence-floor)
        stakes? (boolean (or (always-escalate-ops (:op proposal))
                              (high-cost-supply-order? proposal)))
        hard? (boolean (seq hard))]
    {:ok?          (and (not hard?) (not low?) (not stakes?))
     :violations   hard
     :confidence   conf
     :hard?        hard?
     :escalate?    (and (not hard?) (or low? stakes?))
     :high-stakes? stakes?}))

(defn hold-fact
  "The audit fact written when a proposal is rejected (HOLD)."
  [request context verdict]
  {:t          :governor-hold
   :op         (:op request)
   :actor      (:actor-id context)
   :seller-id  (:seller-id request)
   :disposition :hold
   :basis      (mapv :rule (:violations verdict))
   :violations (:violations verdict)
   :confidence (:confidence verdict)})
