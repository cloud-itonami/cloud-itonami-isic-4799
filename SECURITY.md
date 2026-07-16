# Security Policy

This project handles non-store-retail operations and compliance-concern
workflows, including consumer cooling-off/cancellation-right complaints.
Treat vulnerabilities as potentially high impact even when the demo data
is synthetic.

## Do Not Disclose Publicly

Report privately before opening public issues for:

- credential exposure
- real customer, seller or vendor data exposure
- authorization bypass
- NonStoreRetailGovernor bypass
- audit-ledger tampering
- over-disclosure in compliance-concern reports or exports
- tenant isolation failures

## Reporting

Use GitHub private vulnerability reporting when available for the repository.
If that is unavailable, contact the repository maintainers through the
cloud-itonami organization before publishing details.

Include:

- affected commit or version
- reproduction steps
- expected and actual behavior
- impact on customer/seller/vendor data, policy enforcement or audit logging
- suggested fix, if known

## Production Guidance

- Store secrets outside Git.
- Keep real customer, seller and vendor data outside this repository.
- Run policy tests before deployment.
- Export and review audit logs regularly.
- Use least privilege for operators and service accounts.
