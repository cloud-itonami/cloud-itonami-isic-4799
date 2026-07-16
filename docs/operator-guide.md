# Operator Guide

## First Deployment
1. Register operator, sellers/routes/vending-machine fleets and vendors;
   independently confirm each seller's business registration (door-to-
   door permit, vending-machine-fleet license, or party-plan/direct-
   sales registration) and each vendor's registration before seeding
   `nonstoreops.store`.
2. Import existing sale/order, route/fleet-scheduling and supply-order
   history.
3. Run read-only sale/order-record-logging and route-scheduling dry-runs
   (Phase 0-1).
4. Configure the rollout phase and the `coordinate-supply-order`
   cost-escalation threshold for human sign-off paths.
5. Publish a dry-run compliance-concern flag and audit export.

## Minimum Production Controls
- seller/route/vending-machine-fleet-registration/verification check
  before ANY proposal for that seller
- vendor-registration/verification check before ANY `:coordinate-
  supply-order` proposal
- governor gate on every proposal before commit
- human sign-off for `:flag-compliance-concern` (always) and high-cost
  `:coordinate-supply-order` proposals
- audit export for every commit, hold and approval
- backup manual back-office process

## Consumer-Protection Note
This ISIC class covers door-to-door and home-demonstration/party-plan
selling, which are heavily regulated against high-pressure sales tactics
in most jurisdictions and typically carry a statutory cooling-off/
cancellation-right period. This actor **never** finalizes a waiver of
that right on a customer's behalf -- it only logs sales, coordinates
routes/supply orders, and flags compliance concerns (including possible
cooling-off/cancellation-right violations) for a human to review and
act on through the operator's own compliant consumer-rights process.

## Certification
Certified operators must prove seller/vendor-verification discipline,
governor-bypass resistance, evidence-backed compliance-concern
reporting, and human review for every escalation-gated action --
especially any cooling-off/cancellation-right-related complaint.
