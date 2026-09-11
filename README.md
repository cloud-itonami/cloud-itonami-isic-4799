# cloud-itonami-isic-4799

Open Business Blueprint for **ISIC Rev.5 4799**: other retail sale not in
stores, stalls or markets -- the catch-all non-store, non-stall,
non-online retail class: door-to-door selling, vending-machine retail,
and direct-sales/party-plan home-demonstration selling.

This repository publishes a non-store-retail
operations-COORDINATION actor -- sale/order transaction logging,
door-to-door-route/vending-machine-fleet scheduling, inventory
supply-order coordination with registered vendors, and
compliance-concern flagging -- as an OSS business that any qualified
operator can fork, deploy, run, improve and sell, so an independent
door-to-door seller, vending-machine-fleet operator, or party-plan
representative never surrenders their operations data to a closed
back-office SaaS.

Built on this workspace's
[`langgraph`](https://github.com/kotoba-lang/langgraph)
StateGraph runtime (portable `.cljc`, supervised superstep loop,
interrupts, in-mem/Datomic checkpoints) -- the same actor pattern as
every prior actor in this fleet -- here it is **NonStoreRetailAdvisor ⊣
NonStoreRetailGovernor**. This blueprint's own
`:itonami.blueprint/governor` keyword, `:non-store-retail-governor`, is a
distinct, independent build (no naming-collision precedent question --
distinct from sibling `47xx` actors' own governors, e.g. ISIC 4719's
`:merchandise-retail-governor`).

> **Why an actor layer at all?** An LLM is great at drafting a sale/
> order-record summary, a route/vending-machine-fleet scheduling
> proposal, or a supply-order request -- but it has no license to
> actually finalize a waiver of a consumer's statutory cooling-off/
> cancellation right, no way to independently confirm a door-to-door
> route, vending-machine fleet, or party-plan seller is actually a
> registered/verified counterparty, and no notion of when a "flag this
> concern" op quietly turns into a claim to have already closed out a
> consumer's cancellation right. Letting it act directly invites an
> unverified seller's data entering the ledger, an unverified vendor
> receiving an inventory order, or -- worst of all -- a fabricated claim
> to have already finalized a cooling-off waiver against a pressured
> doorstep customer, exposing the seller and its operator to real
> regulatory liability. This project seals the NonStoreRetailAdvisor
> into a single node and wraps it with an independent
> **NonStoreRetailGovernor**, a human **approval workflow**, and an
> immutable **audit ledger**.

## Scope: coordination only, not consumer-rights adjudication

This actor is **operations coordination only**. It never performs or
authorizes:

- setting or overriding a unit price
- directly finalizing a waiver of a consumer's statutory cooling-off/
  cancellation right (obtaining, confirming, executing, or otherwise
  closing out such a waiver)
- adjudicating a route/territory dispute between sellers, or any other
  consumer-rights or dispute-adjudication authority

The governor's `scope-exclusion-violations` check re-scans every
proposal for this failure mode independently of the advisor's own
framing, and treats it as a HARD, permanent block regardless of
confidence or how clean everything else is. Flagging a compliance
concern for a human to triage is exactly this actor's job --
`:flag-compliance-concern` is never excluded by this check, only
FINALIZING a waiver of a consumer's cooling-off/cancellation right is.

### Actuation

**Every proposal this actor generates is `:effect :propose`, never a
direct actuation.** Two independent layers enforce this
(`nonstoreops.governor`'s `effect-not-propose-violations` HARD check
and `nonstoreops.phase`'s phase table, which never puts
`:flag-compliance-concern` in any phase's `:auto` set). A human
non-store-retail operations coordinator is always the one who actually
acts on a flagged concern or confirms a high-cost supply order.

## The core contract

```
seller/route/vending-machine-fleet registration + operations-coordination request
        |
        v
   ┌───────────────────────┐   proposal      ┌────────────────────────────┐
   │ NonStoreRetail-       │ ─────────────▶ │ NonStoreRetailGovernor      │  (independent system)
   │ Advisor (sealed)      │  + citations    │ seller-unverified ·         │
   └───────────────────────┘                 │ vendor-unverified ·         │
          │                 commit ◀┼ effect-not-propose ·               │
          │                         │ scope-excluded (cooling-off/       │
    record + ledger        escalate ┼ cancellation-right-waiver          │
          │              (ALWAYS for│ finalization) · op-not-allowed      │
          │       :flag-compliance- │                                      │
          │       concern/high-cost └────────────────────────────┘
          │       supply-order)
          ▼
      human approval
```

**The NonStoreRetailAdvisor never commits a proposal the
NonStoreRetailGovernor would reject, and a compliance-concern flag or a
high-cost supply order never commits without a human sign-off.** Hard
violations (an unregistered/unverified seller; an unregistered/
unverified supply-order vendor; a non-`:propose` effect; content
touching a cooling-off/cancellation-right-waiver finalization; an op
outside the closed allowlist) force **hold** and *cannot* be approved
past.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot
may perform physical domain work** (here: autonomous vending-machine
restocking, last-mile route delivery for door-to-door catalog drops)
under human/robot operations gated by seller/operator policy. This
actor itself does not dispatch robot/hardware actions -- it is strictly
the operations-coordination layer (sale/order-record logging, route/
fleet scheduling, supply-order coordination, compliance-concern
flagging) any physical-dispatch layer could eventually feed proposals
into, always gated the same way by the independent
NonStoreRetailGovernor.

## Features

- **Closed proposal-op allowlist**: `log-sales-record`,
  `schedule-route-operation`, `coordinate-supply-order`,
  `flag-compliance-concern` (all `:effect :propose`).
- **Four HARD governor checks** (permanent, un-overridable):
  1. **Seller unverified** -- the target door-to-door route/
     vending-machine-fleet/party-plan seller's registration must exist
     AND be independently registered/verified in the store. This is the
     vertical's primary gate: there is no fixed storefront to verify,
     so seller/route/fleet registration is the analog every other
     `47xx` actor spends on store verification.
  2. **Vendor unverified** -- for `:coordinate-supply-order` only, the
     named vendor must exist AND be independently registered/verified.
  3. **Effect is :propose** -- any other `:effect` value is rejected.
  4. **Scope exclusion** -- directly finalizing a waiver of a
     consumer's statutory cooling-off/cancellation right, and an op
     outside the closed allowlist, are both permanently blocked. This
     is this ISIC class's distinct consumer-protection dimension:
     door-to-door and home-demonstration selling are heavily regulated
     against high-pressure sales tactics in most jurisdictions, so this
     actor structurally can never be the system that finalizes a
     customer's cooling-off waiver.
- **Two ESCALATE (SOFT) gates**, either forces human sign-off:
  - `:flag-compliance-concern` -- ALWAYS escalates, regardless of
    confidence or phase. A "flag a concern" op is never auto-commit
    eligible and never finalizes a consumer-rights decision itself --
    it only surfaces the concern for a human.
  - `:coordinate-supply-order` above a cost threshold -- a large-value
    procurement proposal always needs a human sign-off.
  - (LLM confidence below the floor also escalates, as with every
    sibling actor.)
- **Staged rollout** (Phase 0→3):
  - Phase 0: read-only
  - Phase 1: sale/order-record logging only (approval-gated)
  - Phase 2: + route/fleet scheduling, supply-order proposals
    (approval-gated)
  - Phase 3: auto-commits clean, high-confidence, low-cost proposals
    (compliance concerns and high-cost supply orders always escalate)
- **Append-only audit ledger** -- every decision is an immutable log
  entry.
- **langgraph-clj StateGraph** -- one request = one supervised run;
  human-in-the-loop via `interrupt-before`.

### Development

```bash
# Install dependencies (if inside the superproject, use :dev alias for local overrides)
clojure -M:dev -P

# Run tests
clojure -M:test

# Run linter
clojure -M:lint

# Run demo
clojure -M:run
```

### Test suite

- `test/nonstoreops/governor_test.cljk` -- unit tests of governor hard
  checks, scope exclusion, and the self-trip regression test
- `test/nonstoreops/advisor_test.cljk` -- advisor proposal shape and
  consistency
- `test/nonstoreops/phase_test.cljk` -- rollout phase logic
- `test/nonstoreops/governor_contract_test.cljk` -- full graph
  integration, audit trail
- `test/nonstoreops/store_contract_test.cljk` -- Store protocol and
  MemStore implementation

### Modules

- `nonstoreops.store` -- SSoT (MemStore, String-keyed seller/vendor
  directories, append-only ledger)
- `nonstoreops.advisor` -- contained intelligence node (mock +
  real-LLM seam)
- `nonstoreops.governor` -- independent compliance layer
- `nonstoreops.phase` -- staged rollout (0→3)
- `nonstoreops.operation` -- langgraph-clj StateGraph
- `nonstoreops.sim` -- demo driver

## Capability layer

This blueprint resolves its technology stack via
[`kotoba-lang/industry`](https://github.com/kotoba-lang/industry) (ISIC
`4799`).

## Business-process coverage (honest)

| Covered | Not covered (out of scope for this R0) |
|---|---|
| Sale/order transaction logging (`:log-sales-record`) | Real POS/payment-terminal integration |
| Door-to-door-route/vending-machine-fleet scheduling coordination (`:schedule-route-operation`) | Direct route-optimization/telematics integration |
| Inventory supply-order coordination with a registered, verified vendor, HARD-gated on vendor verification and a double-actuation-free single-proposal shape (`:coordinate-supply-order`) | Real supplier-ordering-system integration |
| Compliance-concern flagging (consumer complaints, cooling-off/cancellation-right violations, route/territory disputes), ALWAYS human-gated (`:flag-compliance-concern`) | Directly finalizing a cooling-off/cancellation-right waiver, or adjudicating a route/territory dispute -- permanently out of scope, not a gap |
| Immutable audit ledger for every log/schedule/order/flag decision | Daily cash-reconciliation across a vending-machine fleet -- a follow-up slice, not in this R0 |

Extending coverage is additive: add the next op (e.g. a
return-authorization or a route-reassignment-request check) as its own
governed op with its own HARD checks and tests, following the SAME "an
independent governor re-verifies against the actor's own records before
any real-world act" pattern this repo's flagship checks already
establish.

## Maturity

`:implemented` -- `NonStoreRetailAdvisor` + `NonStoreRetailGovernor` run
as real, tested code (see `Development` above), following the SAME
governed-actor architecture as every prior actor across this fleet, with
its own distinct, independently-named governor and its own
consumer-protection cooling-off/cancellation-right scope exclusion.

## License

Code and implementation templates are AGPL-3.0-or-later.
