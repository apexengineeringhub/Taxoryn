# Taxoryn — Indian CA & Tax Practice Software Market Standard and Gap Analysis

Research snapshot: 2 October 2026  
Audience: Product, engineering, practice operators and commercialization  
Purpose: Product strategy and sequencing, not a vendor ranking or legal opinion

## 1. Executive summary

The Indian CA software market is not one market with one winning suite. It is several overlapping categories that firms combine: accounting/books (TallyPrime, Zoho Books, Vyapar); tax preparation and filing (Clear, KDK Spectrum, Winman, CompuTax, Saral, Taxmann and government utilities); practice operations (Zoho Practice, Saral TaxOffice, KDK/CompuTax office tools, spreadsheets, email and chat); and specialist filing rails (GSTN/GSP, Income Tax ERI, TRACES, MCA V3, DSC and UDIN).

The market standard is therefore reliable domain execution plus a usable client/work register, not one app replacing every specialist from day one. A firm moving its operations into Taxoryn needs dependable client identity, recurring obligations, assignments and review, secure evidence exchange, communications history, filing status/evidence, receivables, migration/export, and credible tax/GST/TDS execution. Filing access depends on authorization, government programs, consent and signing—not only software development.

Taxoryn has a substantial practice-operations foundation: multi-tenant organizations and scope controls, clients/Client 360, employees and locations, services/engagements, work templates and instances, tasks, compliance/workflow records, document vault and requests, client portal, notices/hearings, billing/invoices, time tracking, audit, reminders/automation, Gmail foundations, dashboards/reporting, subscription/module/capability controls, marketplace/proposals and service pricing. These are repository-observed modules; they are not proof that every end-to-end workflow or production integration is complete.

The clearest shortfall is specialist tax execution. Taxoryn has GST, ITR and TDS data/workspace foundations and an ITR estimator, but its checked-in ITR note explicitly says it is not a full government return preparation/filing engine. A production GST 2B reconciliation engine, live GSTN/GSP connector, ERI filing implementation, AIS/TIS/26AS/Form 16 ingestion, full ITR schedules/validations, TRACES, DSC signing, UDIN and end-to-end ROC filing were not verified in this repository.

**Strategic recommendation:** Build Taxoryn first as a compliance work operating system. Unify client/service/obligation → work → documents/communication → reviewer decision → filing evidence → invoice/collection. Keep return engines and filing systems behind narrow adapters. Prove operational reliability and migration before broad AI or a general ledger replacement.

**Ten gaps before broad adoption:** (1) recurring obligation-to-work automation with idempotency and deadline change control; (2) GST 2B/IMS reconciliation; (3) complete ITR preparation engine and year-specific validations; (4) consented AIS/TIS/26AS/Form 16/prefill ingestion and reconciliation; (5) governed filing adapters with status and acknowledgement; (6) recurring engagement/service and renewal lifecycle; (7) communications timeline and dependable email/WhatsApp actions; (8) Excel/Tally/tax-software migration and useful exports; (9) workload, WIP, realization and profitability; (10) operational hardening, integration monitoring, recovery and support.

## 2. Scope, method, evidence labels

This report reviews vendor product/help/API/pricing pages, government/ICAI sources, the current Taxoryn repository, and the architecture baseline supplied in the brief. A vendor page proves what the vendor documents or claims, not independent performance.

- **Official product documentation:** vendor help, product, API or pricing pages.
- **Government/ICAI source:** official filing, consent, API or professional-body material.
- **Vendor claim:** marketing outcomes/adoption/accuracy not independently tested.
- **Community signal:** anecdotal practitioner discussion; useful for discovery, not prevalence.
- **Repository observation:** code, migration, controller, UI or checked-in product note. Presence is not production-readiness.
- **Analyst interpretation:** synthesis or recommendation based on cited material.

Prices change and are not comparable unless the billing unit is stated. This is a capability map, not a competitor ranking.

## 3. Indian CA practice software market overview

Practices often combine a books system, one or more tax packages and a lighter operating layer for clients, deadlines, assignments, files and communication. Tax suites increasingly include client dashboards and practice operations; practice suites increasingly include time, workflow, WhatsApp and portals. The categories overlap, but are not identical.

- **Practice operations:** client/task status, due dates, reusable work, document exchange, collaboration and review are expected in practice-management tools. Zoho Practice currently documents tasks, recurring tasks, timesheets/billing, documents, insights, workpapers, automation, WhatsApp and a premium self-service portal. (Official: [Zoho Practice](https://www.zoho.com/practice/pricing/))
- **Tax preparation:** year/form updates, computation, source import, validations, bulk processing, filing support, acknowledgements and prior-period carry-forward are central to tax-specialist products. Winman documents AIS/26AS/TIS and Form 16 import, computation, ITR/audit, Tally transfer and filing. (Official: [Winman CA-ERP](https://www.winmansoftware.com/products/ca-erp/))
- **GST execution:** GSTR-1/3B/9 preparation, GSTR-2B/IMS and purchase matching, mismatch review, filing permissions/status and ARN/evidence are expected in GST products. GST guidance tells taxpayers to reconcile GSTR-2B with books to prevent duplicate ITC and account for reversals/RCM. (Government: [GSTR-2B FAQ](https://tutorial.gst.gov.in/userguide/returns/FAQ_gstr2b.htm))
- **Accounting:** invoicing, receivables, bank reconciliation, accounting reports, GST-ready invoices and exports are standard in books products, but do not themselves constitute CA practice workflows or full return preparation.
- **Continuity/trust:** security, backups, import/export, visible audit trails and support matter as much as new features. Specialist vendors document Tally/Excel/prior-year imports; migration and coexistence are adoption requirements.

## 4. Competitor landscape

| Product | Customer/focus | Documented capabilities | Pricing evidence | Analyst interpretation / limit |
|---|---|---|---|---|
| Clear / ClearTax practitioner | CAs/tax experts, GST/ITR/TDS | Vendor documents multi-user client dashboard, bulk work, GST returns/reconciliation, ITR imports/filing, TDS/Form 16/FVU and alerts. Its speed, accuracy, savings and user-count statements are vendor claims. [Product](https://cleartax.in/tax-experts) | Trial/demo; consumer ITR plans are per-return and are not practitioner license prices. [Consumer examples](https://cleartax.in/Meta/Pricing/1) | Strong specialist execution; do not infer it replaces every engagement/resource/WIP workflow. Practitioner pricing was not public on the reviewed page. |
| TallyPrime | SME books/accounting, inventory, GST; often a source ledger | GST, reconciliation, e-invoice/e-way bill, payroll, bank reconciliation, reports and migration. [Features](https://help.tallysolutions.com/tallyprime-features-release-wise/) | License/renewal and subscription choices; official India page shows subscription from ₹750/month plus GST, subject to term/edition. [Pricing](https://tallysolutions.com/accounting-software/india/) | Strong books-system role, not a complete CA client-engagement and communication lifecycle by itself. Support coexistence and imports. |
| Zoho Books | Cloud accounting for businesses and accountants | GST filing/2B reconciliation, approvals, roles, portal, payments, reporting, time and mobile. Online filing requires setup and permissions. [Help](https://www.zoho.com/in/books/help/gst/) | Tiered per-organization subscription. [India plans](https://www.zoho.com/in/books/gst-accounting-software/) | Accounting-led; accountant access across client organizations is distinct from CA firm operations. |
| Zoho Practice | Accounting/tax practices | Client collaboration/chat, tasks, recurring work, time/billing, documents, workpapers, automation, WhatsApp, analytics and premium client portal. [Plans](https://www.zoho.com/practice/pricing/) | Per-org/month; five included users on public plans, add-on users/ledgers; partner free access is conditional. | Direct operations benchmark. Standard client-management scope has Zoho Books/client constraints; premium portal supports clients on other accounting tools. |
| Vyapar | Micro/small businesses, mobile/desktop billing | GST billing, inventory, expenses, accounting, reports, mobile and WhatsApp sharing, Tally export. [Product](https://vyapar.com/) | Free/basic and annual premium plans with device/company limits. [Pricing](https://vyapar.com/pricing) | Relevant to SME affordability and client source records, less direct as multi-client practice compliance system. |
| KDK Spectrum Cloud | CA firms and high-volume practitioners | Vendor documents multi-client/bulk GST/ITR/TDS, GSTR-2B/IMS reconciliation, roles, notices, Tally/Excel, Form 16/26AS and practice operations. [Suite](https://www.kdksoftware.com/spectrum-cloud-for-ca/) | Trial/demo/quote on reviewed pages; adoption/performance metrics are vendor claims. | Closest breadth competitor; Taxoryn cannot assume practice workflows alone differentiate it. |
| Winman CA-ERP/GST/TDS | CA/tax offices, ITR/audit/computation | Computation, ITR/audit reports, AIS/26AS/TIS and Form 16 import, Tally transfer, delegation, migration and backups. [CA-ERP](https://www.winmansoftware.com/products/ca-erp/) | Product/license and extra-user pricing; published price list/discounts change. [Price list](https://www.winmansoftware.com/files/Software_pricelist_winman.pdf) | Strong specialist/desktop workpaper workflows; migration path is a competitive advantage. |
| CompuTax / CompuOffice | Tax professionals/CA firms | Vendor documents ITR computation/e-filing, 26AS, GST/TDS, DSC, Tally/Excel, client dashboard, audit forms, payroll and shared client data. [IT product](https://www.computax.in/product/income-tax-software) | Quote/product packaging; comparable public SaaS price not established. | Direct integrated tax suite. Claims on efficiency/market adoption are marketing claims. |
| Saral TaxOffice / TDS | CA back office and specialist tax modules | Client management, tasks, billing/receipts, document library, interconnected tax/audit/balance-sheet data; Saral TDS documents TRACES and single/multi-user plans. [TaxOffice](https://www.saraltaxoffice.com/tax-filing-suite), [TDS price](https://www.saraltds.com/pricing/) | Product/module and single/multi-user licensing. | Evidence that practice suite + specialist tax tools is an established bundle; cloud breadth not established by cited pages. |
| Taxmann One Solution / e-TDS | Tax/legal information and ITR/TDS software | Official support documents separate ITR and TDS products, user editions and frequent updates. [One Solution](https://support.taxmann.com/onesolution-download-help.aspx), [updates](https://support.taxmann.com/updates.aspx) | Product/module licensing; current public price not established. | Relevant specialist; do not infer complete practice-management breadth from its wider publishing/research business. |
| IRIS GST | GST data/reconciliation services and integrations | Public developer docs expose GSTR-2B/IMS, import reconciliation, history and bulk APIs. [API docs](https://developer.irisgst.com/sapphire/index.html) | API/enterprise price not in public API docs. | Potential supplier/integration as much as competitor; diligence APIs, security, SLAs, scope and costs. |
| Government utilities | Statutory filing source | Government rules, authentication, consent, filing, status and acknowledgement; ITD defines ERI types and API flows. [ERI APIs](https://www.incometax.gov.in/iec/foportal/api-specifications) | No vendor SaaS fee; intermediary/provider may charge for tooling. | Authoritative statutory endpoint, but not a firm's engagement, communications or receivables operating system. |

See [Appendix A](#appendix-a--competitor-capability-crosswalk) for a compact feature crosswalk. **Community signal:** Practitioner discussions mention Tally, ClearTax, CompuTax, Winman, KDK, spreadsheets and portal utilities in combinations, and discuss manual downloads/reconciliation. These are anecdotes, not market-share evidence. [CA tool discussion](https://www.reddit.com/r/CharteredAccountants/comments/1csckmh/), [GST reconciliation discussion](https://www.reddit.com/r/CharteredAccountants/comments/1u5hpwp/)

## 5. Market framework + primary Taxoryn decision matrix

For explicit customer and business impact/action detail on the highest-severity gaps, see [Appendix B](#appendix-b--severity-and-impact-matrix-for-principal-gaps).

Market class is segment-relative: direct filing is standard for a filing suite, not for every practice tool. Taxoryn maturity below means repository-observed Present / Foundation / Gap—not independently verified production quality. P0 = blocks a promised workflow or major control; P1 = high-value/core; P2 = significant; P3 = enhancement; P4 = future/niche.

| Capability | Market standard | Taxoryn | Gap / priority | Why / dependency | Recommended phase |
|---|---|---|---|---|---|
| A Practice management | Standard for practice suites | Foundation: organization, settings, people, locations, controls | Cohesive operating cockpit P1 | Common client/service/work model | Stage 2–4 |
| B Organization/employee | Common | Present: employees, hierarchy, locations, roles | Capacity/skills/availability P2 | Needs workload/time data | Stage 4 |
| C Client 360 | Standard | Present: client master, portfolio scopes, 360 aggregation | Migration identity/data-quality P1 | Statutory IDs and dedupe | Stage 2 |
| D Lead/CRM | Common | Marketplace leads/proposals; general CRM depth unclear | Non-marketplace intake/conversion P2 | Referral/email intake, client match | Stage 4 |
| E Engagement | Common | Engagement/client service/proposal foundations | Signed scope, fee changes, renewals P1 | Client/service/e-sign | Stage 2–4 |
| F Service master | Common | Controlled catalog and practice price config | Bind scope, eligibility, frequency, checklist P1 | Service lifecycle | Stage 2 |
| G Compliance management | Standard | GST/ITR/TDS and notice/work records | Unified obligation/evidence state P1 | Calendar, statuses | Stage 2–3 |
| H Compliance calendar | Standard | Foundation/reminders | Authoritative date rules, client eligibility and change propagation P0 | Versioned rule source/overrides | Stage 2 |
| I Task/work | Standard | Tasks, work templates/instances, review states | Recurrence/SLA/escalation/queue quality P1 | Obligation engine | Stage 2 |
| J Document management | Standard | Secure vault and controls | Client/service/period linking, retention/export P1 | Work graph/storage policy | Stage 2 |
| K Document requests | Common | Request workflow | Partial upload/completeness/reminder usability P1 | Portal/notifications | Stage 2 |
| L Client portal | Common/emerging | Present | Mobile-first checklist and accessible uploads P1 | Identity/docs/comms | Stage 2 |
| M GST | Standard in tax suite | GST profile/returns/workspace foundations | Specialist-level preparation/filing P0 | GSP, schemas, approval | Stage 3 |
| N GST reconciliation | Standard for GST suite | Engine not verified | GSTR-2B/IMS matching and exception workflow P0 | Purchase data/source imports | Stage 3 |
| O ITR | Standard tax suite | Profile/return/workspace foundation | Complete preparation/submission not verified P0 | Forms, validations, ERI | Stage 3 |
| P ITR computation | Standard tax suite | Estimator only, AY 2026–27 individual; product note says not full return | Schedules, eligibility, special rates, relief, losses, validations P0 | Rule/version engine, tests | Stage 3 |
| Q AIS/26AS/TIS | Standard in practitioner ITR tools | Mentioned/document types; direct ingestion unverified | Consented ingestion and source reconciliation P0 | ERI/consent or uploads | Stage 3 |
| R TDS | Standard tax suite | Profiles, returns, challans, deductees, certificates/calculator | TRACES/FVU/correction/current forms unverified P1 | Government route/year rules | Stage 3 |
| S Tax notices | Common/emerging | Notice/task/docs/response/review/hearing foundation | Source capture, approval, submission proof/outcome P1 | Portal/email, evidence | Stage 3–4 |
| T Billing | Standard | Invoices/proposals, snapshots, service prices | WIP/retainer and accounting exports P2 | Work/time/invoice | Stage 4 |
| U Payment/receivables | Common | Payment entry and invoice states | Gateway, matching, aging/collection P2 | Provider/bank data | Stage 4–5 |
| V Time tracking | Common | Time-entry foundation | WIP conversion, approval, write-off P2 | Work/invoice linkage | Stage 4 |
| W Profitability | Emerging | Reporting foundation | Client/service margin and realization P2 | Cost/time/collection | Stage 4 |
| X DSC | Standard for relevant filings | No signing flow verified | Signatory/certificate/local signing lifecycle P1 | Approved signer path | Stage 3–5 |
| Y UDIN | Niche but essential for covered attestation | No lifecycle verified | Signer-specific UDIN capture/verification P2 | ICAI process | Stage 4 |
| Z ROC/MCA | Common full-service practice / specialist | No filing engine verified | Calendar, forms, DSC, SRN and evidence P2 | MCA V3/partner route | Stage 4–5 |
| AA Payroll/labour | Common service line, often specialist | No engine verified | Integrate/workflow before building P3 | Provider/rules | Stage 5 |
| AB WhatsApp | Common, increasingly expected | Configuration/message foundations; full timeline unclear | Consent, delivery, opt-out, client linkage P1 | Approved BSP | Stage 2–4 |
| AC Email integration | Common | Gmail foundation | Thread/attachment timeline, sent mail audit P2 | OAuth/client identity | Stage 4 |
| AD Reports/dashboards | Standard | Dashboard/reporting foundation | Reliable exception queues P1 | Consistent statuses | Stage 2–4 |
| AE Business analytics | Emerging | Reporting foundation | WIP/SLA/capacity/profitability P2 | Stable event model | Stage 4 |
| AF Government integration | Standard for specialist tax apps | Production GSP/ERI/TRACES/MCA adapters not verified | Authorization, consent, credentials, monitoring P0 for filing promise | Approval/provider/security | Stage 3–5 |
| AG Filing/submission | Standard tax product | Filing detail/status flows; live submit unverified | Prepare→approve→sign/OTP→submit→ack P0 | Validated engine/adapter | Stage 3 |
| AH Reconciliation | Standard where applicable | Workflow/manual foundations | Explainable match/variance/actions P0 GST/ITR | Ingestion/canonical data | Stage 3 |
| AI Audit trail | Standard | Audit module/entity bases | Integration event, export and tamper controls P1 | Event/retention model | Stage 1–2 |
| AJ Security/RBAC/tenant | Standard SaaS | Strong foundation: tenant, scope, roles, file/security tests | Operational/independent assurance P0 continuous | Key mgmt, security ops | Stage 1–5 |
| AK Mobile | Common access; native app varies | Responsive web; native/offline unverified | Upload/review/approval usability P3 | UX/auth | Stage 5 |
| AL AI/automation | Emerging | Reminders/automation; no validated tax AI | Human-reviewed, cited extraction/assist P3 | Good data/evaluation | Stage 6 |
| AM SaaS administration | Standard SaaS vendor-side | Plans/entitlements/modules present | Dunning, usage ledger, self-service P2 | Gateway/support | Stage 5 |
| AN Data migration | Essential adoption | Bulk/import exists in some domains; full migration unverified | Preview, mapping, rejects, dedupe, lineage P0 | Import contracts | Stage 2 |
| AO Backup/export | Standard trust | Storage/security foundation; complete tenant export/restore unverified | Restore drills and usable exit export P0 | Storage lifecycle/jobs | Stage 1–2 |

**Scope note:** Taxoryn's controller families cover many named domains. Module presence proves a foundation, not statutory coverage, filing access, form accuracy, mobile readiness, production integration, adoption, SLA or disaster recovery.

## 6. Practitioner workflow gap analysis

### Workflow 1: Lead → renewal

Taxoryn has marketplace leads/proposals, client onboarding, KYC/document handling, client services/engagement, work and document flows, review-oriented work states, billing and payment recording. Gaps: marketplace is not the only lead source; proposal acceptance → client → signed engagement should be an auditable conversion; scope/version/signature was not verified; active service should generate recurring obligations; renewal/change-of-fee and collection escalation are not one lifecycle. **Action:** create a general lead intake/conversion path, signed engagement scope and effective-dated service plan, then let each active plan generate obligations and renewal events.

### Workflow 2: Client → GST monthly compliance

Taxoryn has GST records/workspace, documents, tasks and filing states. The expected full loop is source books/purchase register → GSTR-2B/IMS import → explainable match/mismatch → vendor/client follow-up → reviewed ITC → GSTR-3B preparation → approval/submission → ARN/evidence → billing. GSTR-2B is a taxpayer portal download and official guidance advises reconciliation to prevent duplicate ITC and apply reversals/RCM correctly. [GST guidance](https://tutorial.gst.gov.in/userguide/returns/FAQ_gstr2b.htm)

**Action:** make reconciliation a versioned workpaper: stable document identity, configurable tolerance, match rationale, source snapshot, owner, comment/request, accept/reject/defer action, review, reopen history and return impact. Start with validated file intake/export; add live provider connection later.

### Workflow 3: ITR data collection → filing

Taxoryn has ITR profiles/returns/workspace and document/task support, plus an AY 2026–27 individual estimator. Its checked-in note explicitly limits that calculator: it is not full preparation or filing; statutory deduction/category checks, special-rate income, marginal relief, additional income heads and filing workflows are outside v1. A complete filing workflow also needs Form 16, AIS/TIS/26AS, bank/broker/foreign data, source reconciliation, income schedules, deductions and loss carry-forward, tax regime comparison, form-specific schema/rules, preparer/reviewer approval, validation, e-verification/DSC path, acknowledgement and final evidence.

The Income Tax Department documents ERI client-add, consented prefill, validation/submission and acknowledgement services; adding clients and prefill require taxpayer consent, and service requests can require taxpayer verification. [ERI API specs](https://www.incometax.gov.in/iec/foportal/api-specifications), [ERI client/consent](https://www.incometax.gov.in/iec/foportal/help/addclient?mobile-app=1), [verify ERI request](https://www.incometax.gov.in/iec/foportal/help/verifyservicerequestofERIs?mobile-app=1)

**Action:** label estimator output as estimates. Ship import/reconciliation, then one narrow form family to production-quality preparation, then authorized submission, then expand by form/entity.

### Workflow 4: Tax notice → final outcome

Taxoryn appears to have the strongest workflow breadth here: notice intake, assignment/tasks, document requests, responses, review, hearings/adjournments and status. Validate production cases for source and statutory date, partner approval, submission channel, receipt/version immutability, outcome capture, linked invoice and complete evidence export. **Action:** close submission/evidence and source/date provenance; do not prioritize speculative notice scraping before authorized sources are known.

### Workflow 5: Recurring compliance

Taxoryn has service configuration, work templates/instances, compliance/work items, assignment and reminder/automation foundations. The gap is proving each active service has a scope/date, generates each period obligation exactly once, propagates legal due-date changes, assigns staff/reviewer, requests the correct evidence, tracks filing and connects invoice/payment. **Action:** implement an idempotent obligation scheduler with versioned rule source, retries, duplicate prevention and missed-generation monitoring. This is more important than adding generic task types.

## 7. Important domain gaps and recommendations

### GST

**Build:** canonical GSTIN/period/return state; source import; purchase-book adapters; explainable GSTR-2B/IMS reconciliation workpaper; exception assignment/client follow-up; reviewer disposition; return validation and evidence/ARN capture.

**Integrate:** GSP connectivity and possibly high-volume data services. GSTN says an ASP has no separate empanelment but must tie up with a GSP to send/retrieve client data; becoming a GSP involves selection, agreement and direct API access. [GSP ecosystem](https://www.gstn.org.in/gsp-ecosystem), [ASP FAQ](https://www.gstn.org.in/faqs-category-details), [empanelled GSP list](https://www.gstn.org.in/empanelled-gsps). Do not scrape portals around authorized interfaces.

### ITR, AIS, TDS

**Build:** Taxoryn data model, source-to-schedule map, reconciliation workpaper, rule/form/year versioning, review, evidence and clear status.

**Partner or build narrowly:** statutory schemas, tax rules and filing. Type-2 ERI filing involves application/approval, credentials, add-client, taxpayer consent, prefill, validation/submission and evidence. It is not just an API key. [ERI registration](https://www.incometax.gov.in/iec/foportal/help/eri/registration?mobile-app=1), [ERI API specifications](https://www.incometax.gov.in/iec/foportal/api-specifications)

For TDS, support TAN-level access, challan and statement files, correction cycles, certificates, submission proof and TRACES/e-Filing status. Government help documents e-TDS routes through e-Filing and TAN registration; revalidate each form/year before implementation. [TRACES/e-Filing FAQ](https://traces61contents.tdscpc.gov.in/en/faq-dedu-efiling.html)

### DSC, UDIN, MCA

- **DSC:** make signing a privileged, local or approved operation. Record signatory, certificate identity/expiry, payload hash, authorization, timestamp and result. Do not store reusable DSC private keys in a general credential vault. MCA V3 requires signatories to associate DSC; ITD ERI specs describe DSC-based flows. [MCA](https://www.mca.gov.in/content/mca/global/en/home.html), [ERI API specs](https://www.incometax.gov.in/iec/foportal/api-specifications)
- **UDIN:** implement signatory-controlled generation tracking and release gates for relevant attest work, with number and verification evidence. ICAI states that eligible full-time practising CAs generate UDIN and warns about false “generate from this software” interfaces. No authorized public UDIN generation API was identified in this review; assume portal handoff unless ICAI documents one. [ICAI UDIN FAQ](https://www.udin.icai.org/pdf/FAQs%20on%20UDIN%20%285th%20Edition%29.pdf), [UDIN portal](https://udin.icai.org/)
- **MCA/ROC:** initially build entity-specific calendar, signatory directory, checklist, work, form/SRN/status/evidence and specialist handoff. Current public help references V3 user profiles and DSC; this review did not establish a universal partner API to file every form. [MCA V3 FAQ](https://www.mca.gov.in/Ministry/pdf/V3_Consolidated_FAQ_Dated_26042022.pdf)

### Migration, communications, analytics and assurance

Migration is part of the product: import client masters/statutory IDs, service periods, filing state, client-service maps, prior-year references, documents, users and accounting/tax-software links. Include mapping preview, rejects, duplicate warnings, lineage, totals and repeatable delta import. Export clients, documents, obligations, events and transaction evidence in usable formats.

Make email/WhatsApp a client-scoped timeline: sender, recipient, consent, delivery, linked service/work, attachments and retention. Use approved WhatsApp Business Platform flows, opt-outs and portal/email fallback. Integrations need token rotation, least privilege, health and retry queues.

Practice analytics should answer: what is due/blocked, awaiting documents/review, failed or rejected, overdue by owner, WIP, invoiced, collected, written off and realized by service/client/team. Build these on reliable states, not premature charts.

The architecture has useful foundations: tenant separation, RBAC, client scope, file validation, audit and production configuration tests appear in the repo. Large-scale readiness still requires independent penetration testing; tenant-boundary tests for integrations/exports; encrypted per-tenant credentials and rotation; restricted/audited support; secure links; retention/deletion/hold; backup/restore drills; incident response; delivery/retry monitoring and support SLAs. Do not infer these operational controls from code presence.

## 8. Government integration readiness

| Area | Operating model evidenced | Requirements for genuine connected filing |
|---|---|---|
| Income Tax | ERI Type 1 uses portal/approved utilities; Type 2 submits through ITD APIs; Type 3 provides offline utilities. ITD documents login, add-client, consented prefill, validation/submit, verification/DSC and acknowledgement. | Choose partner/Type 1 vs Type 2. Complete registration/approval, API keys, sandbox, consent and client authorization, form schemas/rules, signed API requests, OTP/DSC, retries/idempotency, state machine, receipt, evidence and support. Avoid password collection as shortcut. |
| GST | Third-party apps connect via GSP; ASP must tie up with a GSP. Portal access/data are taxpayer/GSTIN controlled; 2B is post-login download. | Evaluate GSP coverage, contract, costs, tokens/OTP, multiple GSTIN authorization, rate limits, sandbox, submit-vs-file boundaries, tax payment/signing and error recovery. Keep file import/export fallback. |
| TDS/TRACES | Deductor/TAN registration, e-Filing submission and TRACES services for statements/certificates/corrections; routes/forms can change. | TAN permissions, secure files/challans, FVU/validation, correction lineage, Form 16 evidence, status and receipt. Use documented providers/handoff if public API is unavailable. |
| DSC | Some flows use a registered signer/certificate; MCA V3 requires associated DSC. | Local signer or approved signing vendor, key remains signer-controlled, certificate checks, payload hash, authority, audit and retry handling. |
| UDIN | ICAI member-specific official portal and signer responsibility for applicable attestation. | Signer-specific record, verification and evidence. Do not claim generation without an authorized documented API. |
| MCA | Portal uses V3 business users, signatory association and DSC for relevant forms. | Start with workflow/evidence/portal handoff. Verify API, partner terms, current form coverage, signature/payment mechanics before committing to direct filing. |

**Architecture:** use provider-neutral adapters for provider, organization/taxpayer, credential grant, consent, payload, filing job, status event and evidence. Keep tokens in managed secrets, never logs/audit payloads. Keep business records provider-neutral to support switching GSP/ERI.

## 9. Pricing and commercial model

Observed models vary by category. Zoho Practice publishes per-organization/month pricing, included user caps and user/ledger add-ons; Zoho Books is tiered per organization; Tally offers subscription and license/renewal choices; Vyapar offers annual plans and basic/mobile options; Winman and Saral publish product/module/user licenses; practitioner suites commonly use trial/demo/quote pricing; consumer ITR products charge by return complexity. Sources: [Zoho Practice](https://www.zoho.com/practice/pricing/), [Zoho Books](https://www.zoho.com/in/books/gst-accounting-software/), [Tally](https://tallysolutions.com/accounting-software/india/), [Vyapar](https://vyapar.com/pricing), [Winman](https://www.winmansoftware.com/files/Software_pricelist_winman.pdf), [Saral](https://www.saraltds.com/pricing/), [Clear consumer examples](https://cleartax.in/Meta/Pricing/1).

Keep four concepts distinct: Taxoryn SaaS subscription/entitlements; Taxoryn suggested and practice service prices; practitioner proposals/client invoices/payments; and possible filing/provider usage costs. Practice service pricing is client-fee configuration, not Taxoryn SaaS monetization.

Commercial gaps include SaaS subscription dunning/tax invoices, trial conversion, usage ledger, support/migration packages, renewal/export, multi-branch packaging and provider pass-through cost disclosure. Test per-org, seat, client, GSTIN/TAN/return-volume or hybrid models with firms; do not pick a price from incomparable public prices or without willingness-to-pay evidence.

## 10. Strategic differentiation

The strongest coherent positioning is **a compliance work operating system** that unifies the client context and traces each service from obligation to proof of completion and collection. “One system from lead through payment” is a destination but too broad as an immediate promise; specialist engines and filing access are entrenched/gated. “Government filing” becomes a differentiator only after authorization, accuracy, submission and recovery are proven. “AI tax practice” is not defensible without source data, tested rules and human-review metrics.

Differentiate with:

1. Compliance 360 with provenance: service, period, owner/reviewer, request, source, submission, acknowledgement and invoice.
2. Exception-first queues: each mismatch, missing document or rejection has an owner and next action.
3. An evidence graph linking documents, messages, workpapers, approvals, filing receipts and invoices to client/service/period.
4. Configurable practice types/teams/locations without fragmenting controls or common data.
5. Interoperability: Excel/Tally/tax imports/exports, provider adapters, usable evidence export and coexistence.

This is an analyst hypothesis based on Taxoryn's workflow foundations and competitor category division. Validate through interviews and live migration/pilot work with solo, 5–20 staff and multi-partner practices.

## 11. Priority and sequencing

### The 80/20 product bet

Treat “20% of the product will cover 80% of the market” as a hypothesis to validate, not a measured market fact. For the first release, the 20% means one reliable end-to-end operating loop for the common individual and small-business compliance work handled by Indian CA practices. It does not mean implementing 20% of a feature checklist or covering 80% of every tax form and edge case.

The core loop is: enquiry → client/KYC → engagement/service → recurring obligation → calendar/task → document collection → reminders and communication → review/approval → filing evidence → billing. Make the client, service, period, owner, reviewer, status, due date and evidence traceable through that loop. This supports day-to-day practice operations and creates the reliable data needed for integrations, SaaS controls and AI later.

### Stage 2.5 P0 — ship in this order

| Sequence | P0 capability | Minimum useful scope / exit evidence |
|---|---|---|
| 1 | Lead / Enquiry | Capture source, contact, need, owner and outcome; convert a qualified lead without re-keying. |
| 2 | Client Onboarding / KYC | Create a client, capture required identity/KYC checklist and consent, record missing items and onboarding status. |
| 3 | Engagement / Service | Define service, period, scope, fee, preparer, reviewer and client acceptance; link it to client obligations. |
| 4 | Recurring Work Templates | Versioned templates generate one work item per client/service/period; prevent duplicates and support pause/cancel. |
| 5 | Calendar + Tasks | Assign owner, reviewer, due date, dependencies and status; surface today, overdue, blocked and awaiting-client work. |
| 6 | Document Collection | Request/upload documents against a client and work period; track missing, received, rejected and accepted with audit history. |
| 7 | Reminder / Automation | Schedule idempotent reminders from due dates and missing items; record delivery, failure, opt-out and next action. |
| 8 | Communication Timeline | Link client communications and system events to the client/work item; begin with usable manual notes and approved channels. |
| 9 | Review / Approval | Preparer submits, reviewer records approve/reject/comments, and changes retain actor/time/history; approval gates filing readiness. |
| 10 | Billing Foundation | Create a fee quote or invoice from engagement/service, record due/paid/part-paid status and export/share it; defer complex accounting. |

These are all P0 capabilities; the sequence expresses implementation dependencies. Validate the complete path with a pilot practice before expanding breadth. Pilot exit: a lead can become an onboarded client and accepted service; a recurring period creates work exactly once; documents and reminders move it to review; approval and filing evidence are recorded; the engagement fee becomes a trackable invoice. Include tenant isolation, audit, backup/restore, migration preview/export and access controls as launch gates even though they are platform foundations rather than visible P0 features.

### Stage 2.5 P1 — add selectively after the core loop

| P1 capability | 80/20 treatment |
|---|---|
| DSC | Track authorized signer, certificate expiry/readiness and signing outcome; use a supported signing flow. Never store or reproduce private keys. |
| UDIN | Capture applicable UDIN, document linkage and verification/status evidence; do not imitate the ICAI generation service. |
| Timesheet | Start with optional time against client/service/work and a basic effort summary; do not make timer usage a prerequisite for delivery. |
| Locations | Support practice locations and access/reporting filters when pilot firms need them; avoid location-specific pricing rules initially. |
| Client Portal | Pull forward the minimum secure document upload, request list, status and approval action if it reduces follow-up. Keep full self-service portal breadth in Stage 4. |
| Reports | Start with actionable due/overdue, awaiting-client, review and billing lists; defer configurable BI. |
| Team Workload | Show assigned, overdue and blocked items by person/team; add capacity planning only after assignment/status data is trusted. |

### Stage 3 — compliance breadth after workflow proof

1. **Government integration framework and simulator/provider first.** Define consent, authorization, environment, credentials/secrets, request/response versions, status polling, rate limits, retries, idempotency, outages and immutable receipts. Ship a simulator that exercises success, rejection, timeout, duplicate and provider outage paths before production adapters.
2. **GST slice.** Start with source import and a high-frequency reconciliation workflow (including GSTR-2B/IMS where supported): ingest → match → exceptions → reviewer decision → authorized submission/provider step → acknowledgement and evidence. Validate partner demand before committing to a GSP-dependent scope.
3. **ITR slice.** Select a narrow taxpayer/form/year cohort. Import consented Form 16/AIS/TIS/26AS data, show source-to-schedule reconciliation, validate computation and return fields, require practitioner review, then connect an authorized filing/pre-fill path and store acknowledgement. Expand forms only after this cohort works end to end.
4. **TDS slice.** Add TAN-aware obligations, challan/return workflow, corrections and TRACES/provider evidence for the segments evidenced by pilots.
5. **Compliance automation and advanced notices.** Add versioned rules, explainable triggers, deadline changes, notice intake/classification, assignment, response checklist, review and evidence. Keep human authorization in the loop.

GST-first versus ITR-first is a working sequencing choice, not a universal market ranking. Compare pilot firms’ recurring volume, pain, willingness to pay, integration readiness and statutory timing; switch the first production vertical slice if that evidence favors ITR or TDS.

### Stages 4–6 — scale only after usage proves the need

- **Stage 4 — Practice & Client Collaboration:** expand the secure client portal, communication channels/timeline, shared checklists, approvals, collaboration and review history. Reuse Stage 2.5’s minimal upload/status/review primitives rather than building a parallel workflow. Add DSC/UDIN tracking, timesheets, locations, reports and workload views according to pilot demand.
- **Stage 5 — SaaS & Commercial Scale:** add subscription plans, entitlement and usage metering, tenant provisioning/lifecycle, platform administration, support operations, migration/export, backup/restore and billing/reconciliation controls. Launch commercial packaging after onboarding, support burden, provider costs and retention are measurable.
- **Stage 6 — AI & Intelligence:** sequence document extraction with confidence and source links → reconciliation suggestions → current, licensed, citation-backed tax research/RAG → human-reviewed drafts for client/notice responses → bounded agents that propose actions. Keep tax conclusions, approval and filing under qualified human control until independently validated for the specific use case.

### Validate the 80/20 claim

Recruit 10–15 design-partner practices spanning solo/small, mid-size and different client/service mixes. Before and during the pilot, measure work items by service and period, time spent, follow-up count, overdue rate, rework, review turnaround, successful completion, willingness to pay and support effort. Define the denominator (for example, recurring work items in the target segment) before claiming coverage. Prioritize the smallest workflow set that handles the largest measured share of that work; do not present a target percentage as an achieved result without the data.

### Defer until evidence or a prerequisite exists

- General-ledger/accounting replacement; integrate or import from established accounting systems first.
- Broad payroll, inventory, banking core, unrelated HR, and feature parity without a target segment or measurable outcome.
- Portal scraping, shared portal passwords, stored DSC private keys, or filing without authority, consent, reviewer approval and recovery paths.
- Autonomous tax advice or filing; broad AI agents before reliable workflow data, evaluations and provenance.
- Advanced analytics, location-based pricing, native mobile/offline and white-label before repeated customer demand.

## 12. Roadmap reconciliation

The following reconciles the supplied Stage 2.5 P0/P1 list and Stages 3–6 with the market-gap findings. It is a recommended sequence, not a claim that the roadmap has already been implemented.

| Supplied stage | Assessment | Recommended adjustment and reason |
|---|---|---|
| Stage 2.5 P0 — ten capabilities | Highest near-term market coverage: operational workflow shared across many compliance services. | Build the ordered lead → KYC → engagement → recurring work → tasks → documents/reminders/communication → review → billing loop. Prove one complete pilot path before adding breadth. |
| Stage 2.5 P1 — DSC, UDIN, timesheet, locations, portal, reports, workload | Useful enhancers with uneven demand and dependencies on clean client/work records. | Deliver thin slices selectively: secure portal upload/status and actionable reports can pull forward; basic workload after task/status quality; timesheet/locations on demand; DSC/UDIN tracking where the filing cohort needs them. |
| Stage 3 — Government Integration Framework, GST / ITR / TDS, Simulator / Provider, Compliance Automation, Advanced Notice Workflow | High differentiation and high correctness/integration risk. | Build the simulator/framework before adapters. Validate partner onboarding early. Ship one narrow GST, ITR or TDS vertical slice selected by measured pilot volume; include review, authorization, receipt and evidence. Add automation/notices with versioning and human review. |
| Stage 4 — Practice & Client Collaboration | Extends the shared workflow to client self-service and team coordination. | Expand portal, communication, approvals and collaboration on top of Stage 2.5 primitives; avoid duplicate document/review systems. Add richer team workload and reports after status data is reliable. |
| Stage 5 — SaaS & Commercial Scale | Required to make repeatable sales and service operations sustainable. | Add subscriptions, entitlements, usage, tenant lifecycle and platform administration after pilot onboarding, retention, support load and unit economics are measurable. Begin long-lead provider/ERI/GSP discussions during Stage 3. |
| Stage 6 — AI & Intelligence | Potential efficiency gains depend on quality workflow and source data. | Sequence document AI → reconciliation assist → licensed/cited RAG → reviewed drafting → bounded agents. Evaluate against representative cases; no autonomous filing. |

**Dependency sequence:** tenant/security and client identity → lead/KYC/engagement → effective-dated obligations and recurring work → tasks/documents/reminders/communication → review and approval → source ingestion/reconciliation → authorized filing/signing → receipt/evidence → billing/payment → reporting → SaaS usage/entitlements → AI assistance.

**Parallel lead-time tracks:** migration tools and design-partner onboarding; current-year tax schemas and statutory change monitoring; provider/ERI/GSP discovery and approvals; signer/DSC model; support, security and restore readiness. These can start early without pulling the production compliance scope ahead of workflow validation.

**80/20 release gate:** define the target customer segment and denominator, then show in pilot evidence which Stage 2.5 workflows cover most repeated work. Release the next slice based on observed frequency, time/rework reduction, completion and willingness to pay—not on an assumed percentage or feature count.

## 13. Build, buy, integrate

| Area | Recommendation | Reason |
|---|---|---|
| Practice/client/work orchestration | Build | Core differentiation and existing data model. |
| Calendar/rules | Build versioned domain layer; license authoritative data if useful | Eligibility, effective dates, provenance and overrides must be explicit. |
| GST live APIs | Partner with GSP first; Taxoryn owns reconciliation/workflow | GSTN has ASP-through-GSP model; reduces connectivity lead time. |
| Reconciliation | Build match model and exception UX; evaluate licensed ingestion | Explainability/workflow are differentiating; commodity feeds may be bought. |
| ITR engine | Partner/license initially or build narrow forms with exhaustive tests | High change/error cost; current estimator is not return-grade. |
| ITR filing | Authorized ERI route or authorized partner | Registration, consent, security and operations exceed API coding. |
| TDS/TRACES | Partner/provider plus import fallback | Verify access and current form support first. |
| MCA/ROC | Build obligation/evidence; integrate or hand off filing | Public evidence for a universal filing API not found. |
| Email/WhatsApp | Buy approved transport; Taxoryn owns timeline, consent, linkage | Transport commodity; client audit history is product-specific. |
| Payment/SaaS billing | Buy payment rails; build entitlements/subscription ledger | Payment operations commodity; entitlement is product logic. |
| AI/OCR | Buy commodity OCR as appropriate; build validation/provenance | Value is reviewable, source-linked output, not raw extraction. |

## 14. Final recommendations and answers

1. Narrow the initial promise to operationally complete service-to-evidence-to-cash and state filing modes honestly.
2. Select a first practice segment (e.g. recurring GST/TDS plus individual ITR, 5–30 staff) and observe actual work.
3. Complete recurring obligation generation, migration and communication before more generic dashboards.
4. Build GST reconciliation and one production-grade ITR vertical slice; partner where government access is gated.
5. Make consent, reviewer, signer, source, receipt, version and audit first-class entities.
6. Pilot through a full monthly/annual cycle; measure deadline misses, rework, document wait time, exceptions closed, filing rejection recovery, WIP realization and response time.
7. Price from willingness-to-pay and migration cost, not incomparable competitor seat prices.
8. Preserve integration and exit paths; clients' books and government systems remain authoritative in many workflows.

### What must Taxoryn provide for a CA firm leaving Excel + WhatsApp + email + separate tax tools?

Trusted client/service records; recurring obligations generated once with owner/reviewer/deadline; client-side document collection and communication; source-linked data and actionable reconciliations; correct year/form preparation; authorized filing or controlled handoff; acknowledgement/evidence; searchable communication history; invoice/payment/WIP linkage; audit/export; migration; dependable security, backup and support. External filing can still be supported before Taxoryn files directly—but the handoff, state and evidence must be first-class, and product claims must be clear.

### Ten highest priority gaps before large-scale adoption

1. Repeatable migration and tenant export/restore.
2. Accurate, versioned calendar and idempotent recurring work.
3. GST 2B/IMS reconciliation and exception closure.
4. Form 16/AIS/TIS/26AS ingestion and source reconciliation.
5. Year-versioned ITR computation/form preparation beyond estimator v1.
6. Verified ERI/GSP filing strategy and acknowledgement loop.
7. Reviewer/partner/signatory gates and evidence.
8. Engagement/service renewal and communications timeline.
9. TDS/TRACES correction, challan and certificate lifecycle.
10. Integration monitoring, recoverability, security assurance, support and service levels.

### What should make Taxoryn meaningfully different?

Not module count. Every client obligation should have an owner; every exception a next action; every request a response trail; every review a recorded decision; every submission proof; every service a link to time, fee and collection. Specialist tax engines and official rails can be integrated as authorized and reliable. Taxoryn's durable advantage is the shared secure operating workflow and evidence chain across them.

## 15. Sources and research register

### Vendor product, help and pricing
- [Clear tax experts](https://cleartax.in/tax-experts); [Clear consumer price examples](https://cleartax.in/Meta/Pricing/1).
- [TallyPrime features](https://help.tallysolutions.com/tallyprime-features-release-wise/); [GST setup](https://help.tallysolutions.com/setting-up-gst-in-tallyprime/); [India pricing](https://tallysolutions.com/accounting-software/india/).
- [Zoho Books GST help](https://www.zoho.com/in/books/help/gst/); [GSTR-1 filing](https://www.zoho.com/in/books/help/gst/gstr1-filing.html); [India plans](https://www.zoho.com/in/books/gst-accounting-software/).
- [Zoho Practice plans](https://www.zoho.com/practice/pricing/); [Vyapar](https://vyapar.com/); [Vyapar pricing](https://vyapar.com/pricing).
- [KDK Spectrum](https://www.kdksoftware.com/spectrum-cloud-for-ca/); [KDK GST](https://www.kdksoftware.com/gst-software/).
- [Winman CA-ERP](https://www.winmansoftware.com/products/ca-erp/); [Winman price list](https://www.winmansoftware.com/files/Software_pricelist_winman.pdf).
- [CompuTax](https://www.computax.in/product/income-tax-software); [CompuOffice](https://www.computax.in/product.htm?id=pro7).
- [Saral TaxOffice](https://www.saraltaxoffice.com/tax-filing-suite); [Saral TDS pricing](https://www.saraltds.com/pricing/).
- [Taxmann One Solution](https://support.taxmann.com/onesolution-download-help.aspx); [Taxmann updates](https://support.taxmann.com/updates.aspx); [IRIS API documentation](https://developer.irisgst.com/sapphire/index.html).

### Government and professional body
- [Income Tax ERI APIs](https://www.incometax.gov.in/iec/foportal/api-specifications); [ERI registration](https://www.incometax.gov.in/iec/foportal/help/eri/registration?mobile-app=1); [ERI add client/consent](https://www.incometax.gov.in/iec/foportal/help/addclient?mobile-app=1); [verify ERI service request](https://www.incometax.gov.in/iec/foportal/help/verifyservicerequestofERIs?mobile-app=1).
- [GSTN GSP ecosystem](https://www.gstn.org.in/gsp-ecosystem); [GSTN ASP FAQ](https://www.gstn.org.in/faqs-category-details); [empanelled GSPs](https://www.gstn.org.in/empanelled-gsps); [GST portal GSTR-2B FAQ](https://tutorial.gst.gov.in/userguide/returns/FAQ_gstr2b.htm).
- [TRACES/e-Filing TDS FAQ](https://traces61contents.tdscpc.gov.in/en/faq-dedu-efiling.html).
- [ICAI UDIN FAQ](https://www.udin.icai.org/pdf/FAQs%20on%20UDIN%20%285th%20Edition%29.pdf); [UDIN portal](https://udin.icai.org/); [ICAI directorate](https://www.icai.org/post/udin-directorate).
- [MCA portal](https://www.mca.gov.in/content/mca/global/en/home.html); [MCA V3 FAQ](https://www.mca.gov.in/Ministry/pdf/V3_Consolidated_FAQ_Dated_26042022.pdf).

### Community signals
- [CA practice software discussion](https://www.reddit.com/r/CharteredAccountants/comments/1csckmh/).
- [GST 2B multi-client discussion](https://www.reddit.com/r/CharteredAccountants/comments/1u5hpwp/).

Community posts are anecdotal and search-selected; they formulate discovery questions but do not establish market share or consensus.

## Appendix A — Competitor capability crosswalk

Legend: **Doc** = documented on the linked official page; **Claim** = vendor marketing claim; **Unclear** = not evidenced by pages reviewed, not proof the product lacks it. Dots avoid implying products are identical or independently benchmarked.

| Product | Target / size | Client / practice workflows | GST / reconciliation | ITR / calculation | TDS | Notice handling | Books / billing | Docs / portal / comms | Government / imports | Mobile / AI | Commercial pattern |
|---|---|---|---|---|---|---|---|---|---|---|---|
| Clear practitioner | CA/tax experts, multi-client | Doc/Claim: multi-user client dashboard, team/bulk operations | Doc/Claim: GST returns, matching/reconciliation | Doc/Claim: ITR, source imports, filing | Doc/Claim: TDS, Form 16/FVU | Some notice/early warning claims; case workflow depth unclear | GST invoicing marketed; full firm accounting unclear | Client dashboard; consumer vault pages separate | Tax imports/filing marketed | Cloud/from-any-device claim; AI recon claim | Practitioner trial/demo; consumer return-priced plans separate |
| TallyPrime | SME accounting, often client ledger | Multi-company/user features, not a full CA engagement CRM | Doc: GST, reconciliation, e-invoice/e-way bill | Tax reports/calculations exist, not positioned as full practitioner ITR suite | Not core CA TDS suite | Not core notice workflow | Doc: accounting, inventory, payroll, invoicing | Document storage/client portal not primary positioning | GST connected features, Tally/Excel migration | Desktop/cloud access options; AI not established | License/renewal and subscription options |
| Zoho Books | SME/accounting + accountant access | Accountant/client organizations, review tasks | Doc: GSTR filing, 2B reconcile and approvals | Not a full CA multi-client ITR computation suite | Payroll/TDS accounting-related; practitioner TDS depth not established | Not core notice case management | Doc: full accounting/invoice/receivable/payable | Customer portal, documents, mobile/email ecosystem | GSTN filing setup; bank feeds/imports | Native mobile apps, automation | Tiered per org/month |
| Zoho Practice | Accounting practices; 5 users on listed plans | Doc: clients, collaboration, tasks, recurring, workflow, timesheets | Integrates with finance products; not tax filing engine by itself | Not tax calculation product | Not tax engine | Not statutory notice portal | Time/billing features, not full ledger | Doc: documents, client portal, chat, WhatsApp | Zoho ecosystem integrations | Cloud/mobile access; automation, not tax AI claim | Per org/month, user/ledger add-ons, partner program |
| Vyapar | Small business owner; solo/micro through SME | Party/customer data, limited practice workflow | GST billing/reports, business compliance tools | Not CA-firm ITR computation product | Not core | Not core | Doc: accounting, inventory, invoice, expense | WhatsApp invoice sharing; customer file vault depth unclear | Tally export | Desktop + mobile; AI not evidenced | Free/basic plus annual premium |
| KDK Spectrum | CA firms, high-volume/multi-client | Doc/Claim: common clients, dashboard, roles, tasks, bulk | Doc/Claim: GST, 2B/IMS and bulk reconciliation | Doc/Claim: ITR computation, client filing | Doc/Claim: TDS, certificates, TAN workflows | Vendor documents notice tracking | Invoice with filing; broader office management | Security/team collaboration claims; portal specifics vary by module | Tally/Excel imports; GST/ITD/TRACES features marketed | Cloud/from-anywhere; AI reconciliation claim | Trial/demo/quote; figures are vendor claims |
| Winman | CA/tax office, ITR/audit, desktop plus cloud features | Delegation/client summaries and reports documented | Separate GST product; imports Tally/Excel | Strong documented computation, ITR, audit, regime compare | Separate TDS product | Intimations/communication pickup documented; case workflow unclear | Billing add-on; not a general ledger | Backup/cloud options, document manager depth unclear | AIS/26AS/TIS/Form16, filing and migration documented | Desktop/cloud options; no verified AI claim | Product/license, edition and extra-user pricing |
| CompuTax / CompuOffice | CA/tax firms, tax suite | Vendor describes shared client dashboard, task/calendar and shared client record | GST suite and 2B claims in company material | Computation, ITR filing and 26AS import documented | TDS suite / forms documented | Intimation/notice-related handling claimed | Billing/payroll/audit suite claims; ledger depth unclear | Document workflows mentioned; portal details unclear | Tally/Excel, DSC, e-filing claims | Product form factor varies; AI claims not independently verified | Product/quote packaging |
| Saral TaxOffice / Saral TDS | CA offices and tax practitioners | Doc: client/non-client contacts, tasks and office automation | TaxOffice tax suite / GST products; depth by module | ITR and interconnected audit/balance-sheet flows | TDS, TRACES, certificates and multi-user options documented | Not established in cited pages | Billing and receipts in TaxOffice | Document/library management documented | Product-specific filing/import integrations | Product mix includes desktop/cloud elements; AI not evidenced | Product, module and user licenses |
| Taxmann One Solution | Tax professionals needing tax law/ITR/TDS tools | Practice workflow not established by reviewed pages | GST setup/download support; full suite breadth unclear | ITR software documented | TDS/TCS software and updates documented | Not established | Not established | Not established | Frequent software updates and product download/support | AI/mobile not established | Product/module licensing; public price unclear |
| IRIS GST | GST platform/API/integration users | API docs are not proof of full practice CRM | Doc: GSTR-2B, IMS and other reconciliations/APIs | Not an ITR suite based on reviewed docs | Not established in reviewed API docs | Not established | Not a practice ledger based on reviewed docs | API only does not prove portal/comms | GST APIs / reconciliation services documented | API/cloud tooling; AI claims not assessed | Enterprise/API terms not public in reviewed docs |

For the several “Unclear” cells, this means only that the cited public pages did not establish the capability. It is not a negative product test. Vendor statements such as accuracy, time saved, user count and “one-click” outcomes remain product claims.

## Appendix B — Severity and impact matrix for principal gaps

| Gap | Severity | Customer impact | Business impact | Recommended action | Phase |
|---|---|---|---|---|---|
| Obligation rules and recurring work generation | P0 | Deadline/assignment confidence; fewer missed periods | Retention, support burden and statutory risk | Version due-date rules, eligibility and idempotent job generation; exception monitor | Stage 2 |
| Migration and export/restore | P0 | Avoids re-keying and fear of lock-in | Conversion, onboarding cost and trust | Preview/mapping/rejects/reconciliation; tenant export and restore drills | Stage 2 |
| GST 2B/IMS reconciliation | P0 for GST-led target segment | Visible ITC mismatch and vendor follow-up | High-volume use, filing quality and retention | Build explainable match workpapers; integrate GSP after file-based pilot | Stage 3 |
| Complete ITR engine beyond estimator | P0 for ITR-led adoption | Professional cannot rely on estimator as return prep | Accuracy/reputation and seasonal revenue | Ship a narrow complete form/year with rule tests and review before breadth | Stage 3 |
| AIS/TIS/26AS/Form16 ingestion and reconciliation | P0 for ITR workflow | Less manual entry, source conflicts visible | Prep throughput and fewer rework loops | Consent-based import/source lineage and exception states | Stage 3 |
| ERI/GSP authorization and submission evidence | P0 for “file through Taxoryn” claim | User can know authorized submit/accepted state | Makes filing revenue/retention possible; high regulatory risk | Partner or apply; don't claim direct filing before approved and monitored | Stage 3–5 |
| TDS/TRACES correction lifecycle | P1 | Multi-TAN status, challans and corrections in one workflow | Keeps recurring compliance clients in product | Cover form/year, FVU, correction, certificates, receipts and fallback | Stage 3 |
| Engagement letter / active service renewal | P1 | Clear scope, authority and fee for each recurring service | Reduces scope leakage and improves renewals | Signed scope/version and effective-dated service plan | Stage 2–4 |
| Evidence/communication timeline | P1 | Fewer lost instructions and repeated client chases | Differentiation and dispute/support cost | Link email/WhatsApp, uploads, review and filing evidence to service/period | Stage 2–4 |
| Partner/reviewer gates and notice submission evidence | P1 | Clear accountability for high-stakes work | Risk control and auditability | Require named review/sign-off and immutable submitted version/receipt | Stage 2–3 |
| Security operations and recovery proof | P0 continuous | Confidence for sensitive PAN/financial records | Enterprise readiness and incident impact | Pen-test, access review, tenant exports, restore/incident exercises | Stage 1–5 |
| WIP/profitability/capacity analytics | P2 | Partners see overloaded teams and unbilled work | Margin and scale management | Join time, service, invoice, payment, staff-cost and write-off data | Stage 4 |
| UDIN/signatory tracking | P2, mandatory for applicable work | Reduces missed signer-specific compliance | Supports attestation service line | Capture official number/verification; no replica generation | Stage 4 |
| MCA/ROC filing module | P2 | Corporate clients can run more services in one workflow | More account share, but high scope cost | First calendar/evidence plus partner handoff; verify filing APIs before build | Stage 4–5 |
| Native mobile/offline workflow | P3 | Convenience for partner/client approval | Adoption for some segments | Responsive web first; measure usage before native/offline investment | Stage 5 |
| Broad AI tax assistant | P3/future | Potential faster prep, but confidence risk | Differentiation only if accurate | Narrow evaluated extraction/reconciliation with citations and human review | Stage 6 |

## Appendix C — Market capability classification key

The main matrix above classifies every requested capability. The classification is not universal across all software: **market standard** applies within the matching specialist category; **common** is frequently expected; **emerging** is becoming a buying expectation; **advanced/differentiator** is not baseline but can distinguish a product; **niche** applies to a specific practice/service type. For example, GST reconciliation is standard for GST software, while UDIN workflow is niche at the whole-market level but operationally important for firms doing covered attest work.



