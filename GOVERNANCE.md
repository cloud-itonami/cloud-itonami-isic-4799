# Governance

`cloud-itonami-isic-4799` is an OSS open-business blueprint for
non-store retail operations coordination (ISIC Rev.5 4799 -- other
retail sale not in stores, stalls or markets: door-to-door selling,
vending-machine retail, direct-sales/party-plan home-demonstration
selling).

## Maintainers
Maintainers may merge changes that preserve these invariants:
- a proposal for an unverified/unregistered seller/route/vending-
  machine-fleet, or a supply order naming an unverified/unregistered
  vendor, can never commit.
- the NonStoreRetailGovernor remains independent of the advisor.
- hard policy violations (non-`:propose` effect, content that directly
  finalizes a waiver of a consumer's statutory cooling-off/cancellation
  right, an op outside the closed allowlist) cannot be overridden by
  human approval.
- every sale/order-record log, route/fleet-operation schedule,
  supply-order coordination and compliance-concern flag is auditable.
- customer, seller and vendor data stays outside Git.

## Decision Records
Architecture decisions live in `docs/adr/`. Changes to the trust model,
storage contract, public business model, operator certification or
license should add or update an ADR.

## Operator Governance
Anyone may fork and operate independently. itonami.cloud certification is
a separate trust mark and should require security, audit and data-flow
review.

Certified operators can lose certification for:
- bypassing sale-record, route/fleet-scheduling, supply-order or
  compliance policy checks
- mishandling customer, seller or vendor data
- misrepresenting certification status
- failing to respond to consumer-protection or security incidents
