# ITR tax calculation v1

Taxoryn's ITR calculator is an estimation and calculation engine. It is not the complete government return preparation or filing engine.

## Calculation sequence

```text
Income
  ↓
Deductions
  ↓
Taxable income
  ↓
Slab tax
  ↓
Rebate
  ↓
Surcharge
  ↓
Health & Education Cess
  ↓
Final tax
```

All monetary arithmetic uses `BigDecimal`. The calculation endpoint returns each stage and the taxable portion of each slab. Regime comparison returns both calculations and `taxDifference` as old regime tax minus new regime tax; it does not select a regime.

## Rules and extensions

`TaxRuleProvider` resolves a `TaxRuleSet` by assessment year, taxpayer, age category, residential status, and regime. AY 2026-27's slabs, rebate, surcharge thresholds/rates, and cess rate are isolated in `Ay2026TaxRuleProvider`. The API currently supports individual taxpayers and AY 2026-27. Deductions are a controlled aggregate input capped at gross income; statutory deduction eligibility and category validation are outside v1.

Surcharge marginal relief, special-rate income, tax credits, additional income heads, and filing workflows are future extensions. Calculation requests contain no client or organization identifiers. Persisting any calculation against a client/ITR must use the existing tenant, authentication, RBAC, and client-scope controls.
