# Rikkaus Wealth OS - MVP Product Backbone

> Canonical project memory for product, design, engineering, QA, and AI agents.
>
> Status: Approved MVP scope baseline  
> Market: Vietnam  
> Pilot audience: 2-5 individual users  
> Last updated: 2026-09-02

## 1. Purpose of this document

This is the primary scope and product-decision reference for the Rikkaus Wealth OS MVP. Agents and contributors should read this file before proposing product behavior, architecture, UI flows, schemas, or implementation tasks.

If another document conflicts with this file on MVP scope, use this file as the current decision baseline unless the product owner explicitly changes it.

## 2. Product definition

Rikkaus Wealth OS is a personal wealth management application for the Vietnamese market. It allows an individual to manually record assets, liabilities, income, expenses, and financial goals; the system then consolidates those inputs into a financial snapshot, visualizes key indicators, and provides basic explainable observations and recommendations.

The product is not an expense-tracking app, a brokerage platform, a bank, an accounting system, or a full Family Office product in the MVP.

### Core value proposition

> Give an individual one coherent view of net worth, asset allocation, debt, liquidity, cash flow, upcoming obligations, and goal progress - without requiring integrations or real-time market data.

### MVP success questions

After using the MVP, a user should be able to answer:

1. How much total wealth do I own?
2. How much do I owe?
3. What is my current net worth?
4. Where is my wealth concentrated, and how liquid is it?
5. Can my near-term cash flow cover upcoming obligations?
6. Is monthly income higher than monthly spending?
7. What financial conditions require my attention?
8. Am I on track for my financial goals?

## 3. Approved product decisions

| Decision area | MVP decision |
|---|---|
| Initial market | Vietnam |
| Initial customer | Individual users |
| Pilot size | 2-5 users |
| Data entry | Manual entry only |
| Excel/CSV import | Deferred to Phase 2 |
| External APIs | Deferred to Phase 2 |
| Real-time data | Not required |
| Valuation | User-entered valuation |
| Supported currencies | VND and USD |
| Exchange rate | User enters USD/VND rate and update date |
| Asset ownership | One asset belongs to one user |
| Household/Family Office | Out of MVP |
| Roles and permissions | Out of MVP; each user sees only their own data |
| AI Advisor | Out of MVP |
| Recommendations | Deterministic, explainable rules only |
| Sensitive document storage | Out of MVP |
| User-facing backup/restore | Out of MVP |

## 4. Product principles for the MVP

1. **Manual-first:** Make manual entry fast and understandable before adding integrations.
2. **Snapshot before sophistication:** Correct net worth and cash-flow views matter more than real-time prices.
3. **Explain every result:** Every metric and recommendation must show its inputs or reason.
4. **Progressive disclosure:** Start with a short common form; show type-specific fields only when useful.
5. **Data freshness is visible:** User-entered values must show their valuation/update date.
6. **No false precision:** Illiquid assets are estimates, not real-time market values.
7. **Independent users:** There is no shared household, co-ownership, or advisor access in the MVP.
8. **Pilot learning first:** Optimize for early feedback rather than feature completeness.

## 5. Core user journey

```mermaid
flowchart LR
    A[Create account] --> B[Set USD/VND rate]
    B --> C[Enter assets]
    C --> D[Enter liabilities]
    D --> E[View net worth dashboard]
    E --> F[Enter recurring income and expenses]
    F --> G[View cash flow and liquidity]
    G --> H[Create a financial goal]
    H --> I[Review rules-based insights]
```

## 6. MVP delivery roadmap

The MVP is divided into four independently testable increments. Do not build later increments before the earlier increment is usable and feedback has been collected.

### MVP 1A - Wealth Snapshot

**Outcome:** A user can enter assets and liabilities and immediately understand current net worth and asset structure.

#### Account and personal settings

- Sign up, sign in, sign out.
- Each account accesses only its own records.
- Basic personal profile.
- Base reporting currency is VND.
- User-entered USD/VND exchange rate.
- Store and display the exchange-rate update date.

No admin UI, household, invitations, shared access, or role editor is required.

#### Assets

Supported categories:

- Cash.
- Bank account.
- Term deposit.
- Listed securities.
- Bond.
- Investment fund.
- Real estate.
- Private business equity.
- Vehicle or valuable asset.
- Collectible.
- Other asset.

Common required fields:

- Asset name.
- Asset category.
- Current value.
- Currency: VND or USD.
- Valuation date.
- Liquidity: high, medium, or low.

Common optional fields:

- Purchase value/cost basis.
- Purchase date.
- Quantity.
- Institution or storage location.
- Notes.

Optional security/fund/bond fields:

- Symbol or instrument code.
- Quantity.
- Cost per unit.
- Current price per unit.
- Interest rate or expected yield.

All prices are manually entered. No market-price integration is allowed in MVP 1A.

#### Asset valuation history

Every meaningful change to an asset's current value creates a new valuation record. This provides an auditable manual history and enables trend charts without real-time data.

Each valuation record contains:

- Asset ID.
- Value.
- Currency.
- Effective date.
- Exchange rate used when applicable.
- Optional note.

#### Liabilities

Supported categories:

- Bank loan.
- Personal loan.
- Margin loan.
- Credit card balance.
- Guarantee obligation.
- Other liability.

Fields:

- Liability name and category.
- Current outstanding balance.
- Currency.
- Interest rate.
- Start date.
- Maturity date.
- Recurring payment amount and frequency.
- Next payment date.
- Optional linked collateral asset.
- Notes.

#### Dashboard

Required KPIs:

- Total assets.
- Total liabilities.
- Net worth.
- Debt-to-asset ratio.
- Liquid asset value.
- Last data update time.

Required visualizations:

- Donut chart: assets by category.
- Comparison bar: total assets versus total liabilities.
- Donut chart: assets by liquidity level.
- List: five largest assets.
- List: upcoming liability due dates.

#### Initial insight rules

- One asset category exceeds the configured concentration threshold.
- Debt-to-asset ratio exceeds the configured reference threshold.
- Low-liquidity assets represent a high share of total assets.
- A debt is approaching its payment or maturity date.
- An asset valuation is stale.
- The USD/VND exchange rate is stale.

Thresholds must be centrally configurable. The UI must call them system reference thresholds, not universal financial standards.

#### MVP 1A acceptance criteria

- A new user can complete the first financial snapshot without assistance.
- VND and USD assets consolidate correctly into VND.
- Net worth equals converted assets minus converted liabilities.
- Charts reconcile with the underlying records.
- Editing an asset value adds valuation history without deleting prior values.
- Users cannot access another user's records.
- The user can understand why each insight was generated.

### MVP 1B - Cash Flow and Financial Health

**Outcome:** A user can see whether income and liquid assets can cover spending and upcoming obligations.

#### Cash-flow data

Income categories:

- Salary.
- Business income.
- Rental income.
- Dividend.
- Deposit interest.
- Bond or fund income.
- Other income.

Expense categories:

- Living costs.
- Housing.
- Education.
- Healthcare.
- Insurance.
- Tax and fees.
- Debt principal payment.
- Interest payment.
- Other expense.

Each entry supports:

- Amount and VND/USD currency.
- Entry type: income or expense.
- Category.
- One-time or recurring.
- Monthly, quarterly, or yearly recurrence.
- Effective/start date and optional end date.
- Optional link to an asset or liability.
- Notes.

#### Cash-flow reporting

- Monthly income versus expenses.
- Monthly net cash flow.
- Income composition.
- Expense composition.
- Passive income by linked asset.
- Three-month forecast from recurring entries and scheduled liabilities.

#### Financial-health indicators

- Emergency reserve coverage in months.
- Recurring debt-payment burden.
- Forecast minimum cash balance.
- Share of passive income in total income.
- Share of essential expenses in total expenses.

#### Insight rules

- Spending exceeds income.
- Forecast cash flow becomes negative.
- Emergency reserve is below the configured reference range.
- Recurring debt payments consume a high share of income.
- Liquid assets may not cover an upcoming obligation.
- Income is overly dependent on one source.

#### MVP 1B acceptance criteria

- A user can record one-time and recurring income/expenses.
- Monthly cash-flow totals reconcile with individual entries.
- The forecast uses only visible recurring and scheduled records.
- A user can trace every forecast amount to its source.
- Linked passive income is displayed under the correct asset.

### MVP 1C - Goals and Basic Advisor

**Outcome:** A user can connect the current financial position to a future goal and receive explainable next-step suggestions.

#### Financial goals

Use one generic goal model with templates for:

- Emergency fund.
- Home purchase.
- Education.
- Retirement.
- Net-worth growth.
- Debt reduction.
- Other goal.

Goal fields:

- Name and type.
- Target amount.
- Current allocated amount.
- Target date.
- Planned monthly contribution.
- User-entered expected annual return.

Goal output:

- Completion percentage.
- Remaining amount.
- Remaining time.
- Required monthly contribution.
- Status: on track, at risk, or behind.

#### Basic Advisor

The Basic Advisor is a rule-based report, not an AI chat interface. It groups output into:

- Positive conditions.
- Conditions requiring attention.
- Suggested next actions.

Each suggestion must include:

- The observed metric.
- The rule or threshold that triggered it.
- The affected asset, liability, cash-flow item, or goal.
- A plain-language action to consider.
- A disclaimer that the output is informational and based on user-entered data.

Examples:

- Increase liquid reserves before a scheduled loan payment.
- Increase monthly contribution to close a goal shortfall.
- Review a highly concentrated asset category.
- Refresh a valuation older than the configured limit.

#### MVP 1C acceptance criteria

- Goal progress and shortfall calculations are reproducible.
- Changing target date or monthly contribution immediately changes projections.
- Every recommendation is linked to a visible metric and rule.
- Recommendations never claim guaranteed outcomes.

### MVP 1D - Pilot Readiness

**Outcome:** The product is stable and understandable enough for 2-5 pilot users to use repeatedly and provide feedback.

Required improvements:

- Guided onboarding and financial-profile checklist.
- Helpful empty states and example data.
- Numeric, currency, and date validation.
- Search and basic filters.
- Safe edit/delete confirmation flows.
- Responsive desktop and mobile-web layout.
- In-product feedback entry.
- Minimal error logging without financial data or secrets.
- Printable dashboard or PDF summary if implementation cost is low.

Suggested onboarding sequence:

1. Set USD/VND exchange rate.
2. Add cash and bank accounts.
3. Add major assets.
4. Add liabilities.
5. Add recurring income and expenses.
6. Review the first dashboard.
7. Create the first financial goal.

#### MVP 1D acceptance criteria

- A pilot user can complete onboarding without developer intervention.
- Core workflows work on desktop and mobile web.
- Validation prevents invalid currencies, dates, and negative values where not allowed.
- Feedback can be associated with a user and screen without recording sensitive financial payloads.

## 7. Core calculations

All calculations must use decimal-safe money arithmetic. Do not use binary floating-point for stored monetary values.

### Currency conversion

```text
asset_value_vnd = asset_value_usd * user_entered_usd_vnd_rate
```

The conversion must retain the rate and date used for traceability.

### Total assets and liabilities

```text
total_assets_vnd = sum(latest_asset_valuation_converted_to_vnd)
total_liabilities_vnd = sum(current_liability_balance_converted_to_vnd)
net_worth_vnd = total_assets_vnd - total_liabilities_vnd
```

### Debt-to-asset ratio

```text
debt_to_asset_ratio = total_liabilities_vnd / total_assets_vnd
```

If total assets are zero, display the ratio as unavailable; do not divide by zero.

### Monthly net cash flow

```text
monthly_net_cash_flow = monthly_income - monthly_expenses
```

### Emergency reserve coverage

```text
reserve_months = high_liquidity_assets_vnd / average_monthly_essential_expenses_vnd
```

### Goal calculations

The exact projection method must be documented alongside the implementation. MVP may use a transparent future-value formula based on:

- Current allocated amount.
- Monthly contribution.
- User-entered expected annual return.
- Remaining number of months.

Do not imply that expected return is guaranteed.

## 8. Minimal domain model

| Entity | Responsibility |
|---|---|
| User | Individual account and ownership boundary |
| UserSetting | Base currency, USD/VND rate, rate update date, display preferences |
| Asset | Asset identity, classification, ownership, liquidity, current metadata |
| AssetValuation | Append-only manual valuation history |
| Liability | Debt/obligation and optional collateral relationship |
| CashFlowEntry | One-time or recurring income/expense and optional domain link |
| Goal | Target, timing, contribution, and projection inputs |
| DocumentRecord | Metadata and physical/external storage reference only |
| InsightRule | Configurable threshold and message template |
| InsightResult | Reproducible result tied to rule and source metrics |

### Relationship constraints

- A user owns many assets, liabilities, cash-flow entries, goals, and document records.
- An asset belongs to exactly one user in the MVP.
- A liability may reference one collateral asset.
- A cash-flow entry may reference one asset or one liability.
- A document record may reference one asset or one liability.
- Cross-user relationships are prohibited.

## 9. Document handling in the MVP

Because encrypted file storage, backup, advanced permissions, and access audit are deferred, the MVP must not position itself as a secure digital vault.

It may store document metadata only:

- Document name and category.
- Reference number.
- Issue and expiry dates.
- Linked asset or liability.
- Physical or external storage location.
- Notes.

Actual uploads, OCR, semantic search, and document Q&A are Phase 2 capabilities.

## 10. Minimum technical safety baseline

There is no dedicated security module in the MVP, but these controls are non-negotiable implementation hygiene:

- Hash passwords with an established password-hashing algorithm.
- Enforce server-side ownership checks for every user record.
- Do not log passwords, tokens, or financial payloads.
- Expire and invalidate authentication sessions correctly.
- Use HTTPS in any production or remote pilot environment.
- Do not use real sensitive documents in an environment not designed to protect them.

These are engineering controls, not user-facing MVP features.

## 11. Explicit MVP non-goals

- Real-time market prices.
- Bank or brokerage integrations.
- Excel/CSV import.
- Automatic real-estate valuation.
- Automatic private-company valuation.
- Collectible price discovery.
- Automated tax advice or tax calendar.
- AI Advisor or investment chat.
- OCR, document upload, semantic search, or document Q&A.
- Household and Family Office.
- Shared assets or multiple owners.
- Advisor access.
- Role and permission management UI.
- Beneficiaries and succession planning.
- Macro scenario analysis, stress testing, or Monte Carlo simulation.
- Currencies other than VND and USD.
- Native mobile applications.
- User-facing backup and restore.

## 12. Phase 2 direction

### Phase 2A - Reduce manual entry

- Excel/CSV templates, import, validation, duplicate detection, and export.
- Automatic USD/VND exchange-rate retrieval.

### Phase 2B - External data integrations

- Securities prices and portfolio synchronization.
- Bank-account synchronization.
- Transaction, dividend, and margin data.
- Scheduled price refresh and source-quality monitoring.

### Phase 2C - Secure Documents

- Encrypted file storage.
- Backup and restore.
- Access audit.
- OCR and structured field extraction.
- Semantic search and document Q&A.

### Phase 2D - Advanced Planning and AI

- Dedicated retirement and education planners.
- Scenario analysis and stress testing.
- Explainable AI Advisor and portfolio-risk analysis.
- Proactive recommendations grounded in traceable user data.

### Phase 2E - Family Office

- Household members and shared ownership.
- Legal entities.
- Advisor access and detailed roles.
- Beneficiaries and succession planning.
- Consolidated family/entity reporting.

## 13. Pilot learning plan

### Primary questions

- Will users complete manual entry to obtain a consolidated wealth snapshot?
- Which asset categories and fields are actually used?
- Which charts or insights change user understanding or behavior?
- Do users trust self-entered valuations and system calculations?
- What recurring workflow motivates users to return?
- Which manual-entry step creates the most friction?

### Suggested pilot metrics

- Onboarding completion rate.
- Time to first net-worth dashboard.
- Number of assets and liabilities entered per user.
- Percentage of users who enter cash flow after completing the snapshot.
- Percentage who create at least one goal.
- Weekly returning users during the pilot.
- Number of stale/incorrect-data corrections.
- Qualitative usefulness score for each dashboard and insight.

Do not optimize vanity metrics before validating data-entry willingness and repeat usage.

## 14. Guidance for AI agents and contributors

Before adding or proposing a feature:

1. Identify the MVP sub-phase it supports.
2. State the user question it helps answer.
3. Prefer the simplest manual workflow.
4. Do not introduce real-time data, AI, multi-owner modeling, Family Office, or external integrations into MVP work.
5. Preserve VND as the reporting currency and support only VND/USD inputs.
6. Ensure calculations are explainable and reproducible.
7. Keep valuation date, exchange-rate date, and data freshness visible.
8. Treat financial recommendations as informational, rule-based, and dependent on user-entered data.
9. Flag any request involving formal personalized investment advice for product and legal review.
10. Update this backbone when the product owner approves a scope change.

## 15. Definition of MVP completion

The MVP is complete when a pilot user can independently:

1. Create an account and set an exchange rate.
2. Enter and maintain assets and liabilities.
3. See a reconciled VND net-worth dashboard.
4. Record recurring income and expenses.
5. See cash-flow health and upcoming obligations.
6. Create a goal and understand the calculated gap.
7. Receive explainable rules-based observations and actions.
8. Repeat the workflow without developer support.

Completion does not depend on integrations, AI, real-time pricing, document uploads, or Family Office capabilities.
