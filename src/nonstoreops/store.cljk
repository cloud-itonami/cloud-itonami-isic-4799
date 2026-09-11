(ns nonstoreops.store
  "SSoT for the ISIC-4799 'Other retail sale not in stores, stalls or
  markets' (door-to-door selling, vending-machine retail, direct-sales/
  party-plan home-demonstration selling -- the catch-all non-store,
  non-stall, non-online retail class) operations-COORDINATION actor,
  behind a `Store` protocol so the backend is a swap, not a rewrite --
  the same seam every `cloud-itonami-isic-*` actor in this fleet uses.

  Unlike a fixed-storefront retail actor, this vertical has no shop to
  verify -- its gate is the SELLER: a registered/verified door-to-door
  sales route, vending-machine-fleet operator, or party-plan/home-
  demonstration representative. This actor coordinates that seller's
  back-office operations: sale/order transaction logging, route/vending-
  machine-fleet scheduling, inventory supply-order coordination with
  registered vendors, and compliance-concern flagging (consumer
  complaints, cooling-off/cancellation-right disputes, route/territory
  disputes). It never sets or authorizes a unit price, and it never
  directly finalizes a waiver of a consumer's statutory cooling-off/
  cancellation right -- see `nonstoreops.governor`'s
  `scope-exclusion-violations`, a HARD, permanent, un-overridable block.

  `MemStore` -- atom of EDN. The deterministic default for dev/tests/demo
  (no deps). A `sellers` directory keyed by `:seller-id` STRING and a
  `vendors` directory keyed by `:vendor-id` STRING (never keywords --
  consistent keying from the start, avoiding the silent-miss bug that has
  plagued earlier sibling actors).

  A registered/verified seller record (door-to-door route license,
  vending-machine-fleet permit, or direct-sales/party-plan registration)
  must exist before ANY proposal targeting that seller may ever commit or
  escalate -- `nonstoreops.governor`'s `seller-unverified-violations`
  re-derives this from the seller's own `:registered?`/`:verified?`
  fields, never from proposal self-report. A `:coordinate-supply-order`
  proposal additionally names a registered vendor via its own
  `:vendor-id`; the SAME 'ground truth, not self-report' discipline
  applies via `vendor-unverified-violations`.

  The ledger stays append-only: which seller a proposal targeted, which
  operation, on what basis, committed/held/escalated and approved by whom
  is always a query over an immutable log.")

(defprotocol Store
  (seller-record [s seller-id] "Registered seller record, or nil.
    Seller map: {:seller-id .. :name .. :channel .. :registered? bool :verified? bool}.
    `:channel` is one of :door-to-door :vending-machine :party-plan (domain
    color only -- not gated on by the governor).")
  (all-seller-records [s])
  (vendor-record [s vendor-id] "Registered vendor record, or nil.
    Vendor map: {:vendor-id .. :name .. :registered? bool :verified? bool}.")
  (all-vendor-records [s])
  (ledger [s] "the append-only immutable decision-fact log")
  (coordination-log [s] "the append-only committed coordination-proposal history")
  (commit-record! [s record] "apply a committed proposal's record to the SSoT")
  (append-ledger! [s fact] "append one immutable decision fact")
  (with-seller-records [s sellers] "replace/seed the seller directory (map seller-id->seller)")
  (with-vendor-records [s vendors] "replace/seed the vendor directory (map vendor-id->vendor)"))

;; ----------------------------- demo data -----------------------------

(defn demo-data
  "A small, self-contained seller/vendor directory covering both the
  happy path and the governor's own hard checks, so the actor + tests
  run offline. Spans all three non-store retail channels this class
  covers (door-to-door, vending machine, party-plan)."
  []
  {:sellers
   {"seller-1" {:seller-id "seller-1" :name "North Loop Door-to-Door Route"
                :channel :door-to-door :registered? true :verified? true}
    "seller-2" {:seller-id "seller-2" :name "Riverside Vending Machine Fleet"
                :channel :vending-machine :registered? true :verified? true}
    "seller-3" {:seller-id "seller-3" :name "New Home-Demonstration Party-Plan Rep (in intake)"
                :channel :party-plan :registered? true :verified? false}}
   :vendors
   {"vendor-1" {:vendor-id "vendor-1" :name "Northgate Household Goods Supply"
                :registered? true :verified? true}
    "vendor-2" {:vendor-id "vendor-2" :name "Unverified Import Broker Co."
                :registered? true :verified? false}}})

;; ----------------------------- MemStore (default) -----------------------------

(defrecord MemStore [a]
  Store
  (seller-record [_ seller-id] (get-in @a [:sellers seller-id]))
  (all-seller-records [_] (sort-by :seller-id (vals (:sellers @a))))
  (vendor-record [_ vendor-id] (get-in @a [:vendors vendor-id]))
  (all-vendor-records [_] (sort-by :vendor-id (vals (:vendors @a))))
  (ledger [_] (:ledger @a))
  (coordination-log [_] (:coordination-log @a))
  (commit-record! [_ record]
    (swap! a update :coordination-log conj record)
    record)
  (append-ledger! [_ fact] (swap! a update :ledger conj fact) fact)
  (with-seller-records [s sellers] (when (seq sellers) (swap! a assoc :sellers sellers)) s)
  (with-vendor-records [s vendors] (when (seq vendors) (swap! a assoc :vendors vendors)) s))

(defn seed-db
  "A MemStore seeded with the demo seller/vendor directory. The
  deterministic default."
  []
  (->MemStore (atom (assoc (demo-data) :ledger [] :coordination-log []))))

(defn mem-store
  "A MemStore seeded with explicit `sellers`/`vendors` maps (seller-id/
  vendor-id string -> record map) -- the primary test/dev entry point.
  Either may be empty (an unregistered-everywhere seller)."
  ([sellers] (mem-store sellers {}))
  ([sellers vendors]
   (->MemStore (atom {:sellers (or sellers {}) :vendors (or vendors {})
                       :ledger [] :coordination-log []}))))
