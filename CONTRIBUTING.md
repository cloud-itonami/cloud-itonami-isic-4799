# Contributing

`cloud-itonami-isic-4799` accepts contributions to the OSS blueprint,
capability bindings, policy tests, documentation and operator model.

## Development

```bash
clojure -M:test
clojure -M:lint
```

## Rules
- Do not commit real customer, seller, vendor or compliance-incident
  data.
- Keep sale/order-record logging, route/fleet-operation scheduling,
  supply-order coordination and compliance-concern flagging behind the
  NonStoreRetailGovernor.
- Treat non-store-retail-operations workflows as high-risk: add tests
  for seller/vendor verification, effect discipline, scope exclusion,
  escalation and audit logging.
- Never phrase a governor scope-exclusion term as a bare noun (e.g.
  "cooling-off", "cancellation right") -- phrase it as the
  finalization/execution ACTION (e.g. "finalized the cooling-off
  waiver"), and add/extend the
  `default-mock-advisor-proposals-never-self-trip-scope-exclusion`
  regression test for any new term. A bare-noun term will self-trip this
  actor's own legitimate `:flag-compliance-concern` happy path -- see
  `nonstoreops.governor/scope-excluded-terms`'s docstring.
- Document any new business-model or operator assumption in `docs/`.

## Pull Requests
PRs should describe: what behavior changed, which policy invariant is
affected, how it was tested, whether operator or certification docs need
updates.
