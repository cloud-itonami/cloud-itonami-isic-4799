# Business Model: Non-Store Retail Operations Coordination

## Classification
- Repository: `cloud-itonami-isic-4799`
- ISIC Rev.5: `4799` -- other retail sale not in stores, stalls or
  markets (door-to-door selling, vending-machine retail, direct-sales/
  party-plan home-demonstration selling -- the catch-all non-store,
  non-stall, non-online retail class)
- Social impact: consumer protection, local economy, transparency

## Customer
- independent door-to-door sales routes needing an auditable
  operations-coordination platform
- vending-machine-fleet operators needing consistent restock/supply-
  order governance across a distributed fleet
- direct-sales/party-plan home-demonstration representatives and their
  parent organizations
- programs that cannot accept closed, unauditable back-office platforms

## Offer
- sale/order transaction logging
- door-to-door-route/vending-machine-fleet scheduling coordination
- inventory supply-order coordination with registered, verified vendors
- compliance-concern flagging (consumer complaints, cooling-off/
  cancellation-right violations, route/territory disputes) for human
  triage
- role-based access and immutable audit ledger

## Revenue
- self-host setup fee
- managed hosting subscription per seller/route/fleet
- support retainer with SLA

## Trust Controls
- `:non-store-retail-governor` never lets a proposal for an
  unregistered/unverified seller/route/vending-machine-fleet, or a
  supply order naming an unregistered/unverified vendor, commit or even
  escalate
- every proposal's `:effect` must be `:propose` -- a claim to directly
  actuate is a HARD, un-overridable block
- directly finalizing a waiver of a consumer's statutory cooling-off/
  cancellation right is permanently out of scope, not a rollout
  milestone -- the actor may only flag a concern for a human
- a `:flag-compliance-concern` proposal, and a high-cost
  `:coordinate-supply-order`, always require human sign-off
- sensitive customer, seller and vendor data stays outside Git
