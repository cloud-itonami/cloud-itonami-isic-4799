(ns nonstoreops.advisor
  "NonStoreRetailAdvisor -- the *contained intelligence node* for the
  ISIC-4799 'Other retail sale not in stores, stalls or markets'
  (door-to-door selling, vending-machine retail, direct-sales/party-plan
  home-demonstration selling) operations-coordination actor.

  It drafts exactly four kinds of back-office proposal from a closed
  allowlist: sale/order transaction logging, door-to-door-route/
  vending-machine-fleet scheduling, inventory supply-order coordination,
  and compliance-concern flagging. CRITICAL: it is a smart-but-untrusted
  advisor. It returns a *proposal* (with a rationale + the fields it
  cited), never a committed record and NEVER a direct actuation -- every
  proposal's `:effect` is always `:propose`. Every output is censored
  downstream by `nonstoreops.governor` before anything touches the SSoT.

  This advisor NEVER drafts a unit-price decision, or any proposal that
  directly finalizes a waiver of a consumer's statutory cooling-off/
  cancellation-right (a hard consumer-protection line this ISIC class
  carries because door-to-door and home-demonstration selling are
  heavily regulated against high-pressure sales tactics in most
  jurisdictions) -- that is permanently out of scope for this actor, not
  merely un-implemented. `nonstoreops.governor`'s
  `scope-exclusion-violations` independently re-scans every proposal for
  exactly this failure mode (a compromised or confused advisor drifting
  into scope it must never touch) and HARD-holds it, regardless of
  confidence or op.

  Like every sibling actor's advisor, this is a deterministic mock so the
  actor graph runs offline and the governor contract is exercised
  end-to-end. In production this calls a real LLM (kotoba-llm or
  equivalent) with the same proposal shape.

  Proposal shape (all kinds):
    {:op         kw             ; echoes the request op
     :seller-id  str
     :summary    str            ; human-facing draft / finding
     :rationale  str            ; why -- SCANNED by the scope-exclusion gate
     :cites      [str ..]       ; facts/sources the advisor used -- SCANNED too
     :effect     :propose       ; ALWAYS :propose -- never a direct actuation
     :value      map            ; the draft payload a human/system would review
     :confidence 0..1}")

(defprotocol Advisor
  (-advise [advisor store request] "store + request -> proposal map"))

;; ----------------------------- proposal generators -----------------------------

(defn- propose-sales-record
  "Draft a sale/order transaction log entry (a door-to-door sale, a
  vending-machine cash/card collection, or a party-plan home-
  demonstration order). Pure logging of an observed transaction -- never
  a unit-price decision."
  [_db {:keys [seller-id patch]}]
  {:op         :log-sales-record
   :seller-id  seller-id
   :summary    (str seller-id " の販売/注文記録を記録: " (pr-str (keys patch)))
   :rationale  "訪問販売・自動販売機・パーティープラン注文の観察記録のみ。値付けの判断は含まない。"
   :cites      [seller-id]
   :effect     :propose
   :value      (merge {:seller-id seller-id} patch)
   :confidence 0.93})

(defn- propose-route-operation
  "Draft a door-to-door-route or vending-machine-fleet scheduling
  proposal (a route/restock calendar entry, never a direct actuation)."
  [_db {:keys [seller-id patch]}]
  {:op         :schedule-route-operation
   :seller-id  seller-id
   :summary    (str seller-id " のルート/自動販売機補充予定を提案: " (pr-str (keys patch)))
   :rationale  "訪問販売ルートまたは自動販売機補充のスケジュール調整提案のみ。最終配置は人間が確定する。"
   :cites      [seller-id]
   :effect     :propose
   :value      (merge {:seller-id seller-id} patch)
   :confidence 0.88})

(defn- propose-supply-order
  "Draft an inventory procurement coordination request naming a
  registered vendor -- never a finalized purchase order; a human always
  confirms procurement."
  [_db {:keys [seller-id patch]}]
  {:op         :coordinate-supply-order
   :seller-id  seller-id
   :summary    (str seller-id " 向け商品在庫の発注調整を提案: " (pr-str (keys patch)))
   :rationale  "訪問販売/自動販売機在庫の仕入先発注調整提案のみ。確定発注は人間が行う。"
   :cites      [seller-id]
   :effect     :propose
   :value      (merge {:seller-id seller-id} patch)
   :confidence 0.90})

(defn- propose-compliance-concern
  "Surface an observed compliance concern (a consumer complaint, a
  suspected cooling-off/cancellation-right violation, or a route/
  territory dispute with another seller) for HUMAN triage. This op
  ALWAYS escalates in `nonstoreops.governor` -- never auto-committed at
  any phase -- regardless of how confident the advisor is that the
  concern is real. Deliberately reports the OBSERVATION only, never a
  finalization of any consumer right, so the default rationale never
  trips the governor's `scope-excluded-terms` (see that var's
  docstring)."
  [_db {:keys [seller-id patch]}]
  {:op         :flag-compliance-concern
   :seller-id  seller-id
   :summary    (str seller-id " のコンプライアンス懸念フラグ: " (pr-str (:concern patch "unknown")))
   :rationale  "クーリングオフ苦情・解約権に関する消費者懸念・ルート/テリトリー紛争の観察事実の報告。常に人間の確認・対応が必要。"
   :cites      [seller-id]
   :effect     :propose
   :value      (merge {:seller-id seller-id} patch)
   :confidence (or (:confidence patch) 0.85)})

;; ----------------------------- default mock advisor -----------------------------

(defn infer
  "Mock advisor: routes to the correct proposal generator."
  [_db {:keys [op out-of-scope?] :as request}]
  (let [proposal (case op
                   :log-sales-record (propose-sales-record _db request)
                   :schedule-route-operation (propose-route-operation _db request)
                   :coordinate-supply-order (propose-supply-order _db request)
                   :flag-compliance-concern (propose-compliance-concern _db request)
                   {})]
    ;; Test hook: allow injecting scope-excluded content to exercise the
    ;; governor's scope-exclusion block end-to-end. Must be cleared before
    ;; production use.
    (if out-of-scope?
      (update proposal :rationale str
              " -- actually finalized the cooling-off waiver and closed the sale on the spot, no cancellation option offered")
      proposal)))

(defn trace
  "Audit fact for a proposal generated by this advisor."
  [_request proposal]
  {:t       :advisor-proposal
   :op      (:op proposal)
   :seller-id (:seller-id proposal)
   :summary (:summary proposal)
   :confidence (:confidence proposal)})

(defn mock-advisor
  "The deterministic default advisor for offline demo/test."
  []
  (reify Advisor
    (-advise [_ _store request]
      (infer nil request))))
