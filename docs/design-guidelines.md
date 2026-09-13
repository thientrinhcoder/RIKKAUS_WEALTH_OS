# Rikkaus Wealth OS — Design System Guidelines

> Status: Approved design baseline  
> Owner: Product Design  
> Applies to: MVP 1A–1D, Expo/React Native/React Native Web  
> Last updated: 2026-09-13

## 1. Purpose and authority

This document is the shared UI/UX reference and review guardrail for Rikkaus Wealth OS. Product
design, frontend implementation, QA, and AI contributors must consult it before designing or
changing a screen, navigation pattern, component, visualization, or interaction.

The [MVP Product Backbone](../RIKKAUS_WEALTH_OS_MVP_BACKBONE.md) remains authoritative for product
scope and business behavior. The
[Architecture and Technology Decisions](../ARCHITECTURE_TECHNOLOGY_DECISIONS.md) remain
authoritative for the application stack. This document owns the visual language, interaction
model, responsive behavior, accessibility baseline, and design-review criteria. If a future task
needs to depart from these guidelines, record the reason and obtain Product Design approval; do
not introduce a silent page-level exception.

## 2. Product and audience

Rikkaus Wealth OS gives an individual one coherent, explainable view of wealth, debt, liquidity,
cash flow, obligations, and financial goals. MVP data is manually entered, VND is the reporting
currency, and USD is the only additional input currency.

The primary pilot audience is:

- Individuals in Vietnam aged 35–50.
- Financially experienced through expert-level users.
- People managing heterogeneous assets and liabilities who value control, traceability, and an
  efficient overview more than financial education or decorative novelty.
- A small pilot group of 2–5 users using mobile web first, with responsive desktop support.

Design for expert efficiency without assuming that users know the product's data model. Prefer
concise terminology, dense-on-demand views, and direct access to source records. Do not use a
patronizing tutorial tone. Explain system-specific calculations, thresholds, freshness, and
consequences even when the financial concept itself is familiar.

## 3. Experience principles

### 3.1 Manual entry must feel controlled, not laborious

- Put common required fields first and reveal type-specific fields progressively.
- Use the correct keyboard for currency, number, date, email, and text inputs.
- Preserve drafts when navigating back or when a long form is interrupted.
- Keep the primary save or continue action reachable near the bottom thumb zone.
- After saving, show what changed, the reporting-currency result, and the logical next action.

### 3.2 Snapshot before sophistication

- Lead with the current answer, then provide composition and history.
- Net worth is the primary dashboard metric; supporting KPIs must not compete with it.
- Show useful partial dashboards during onboarding instead of locking the dashboard until all
  steps are complete.
- Prefer a clear number and short interpretation over a decorative or overly dense chart.

### 3.3 Every result must be explainable

- Aggregates, forecasts, goal projections, and insights require a visible “Cách tính” or “Vì sao?”
  path.
- Explanations identify inputs, dates, exchange rate, applicable rule or reference threshold, and
  affected records.
- Use “ngưỡng tham chiếu của hệ thống,” never imply a universal financial standard.
- Rule-based output is called “Nhận định” or “Sức khỏe tài chính,” never AI Advisor or AI chat.

### 3.4 Freshness is part of the value

- Display the valuation/update date alongside important values.
- A stale state includes text and an icon; color alone is insufficient.
- Updating an asset value is described as adding a new valuation, not overwriting history.
- USD-derived values expose the original amount, exchange rate used, and exchange-rate date.

### 3.5 Calm confidence over spectacle

- The interface should feel private, precise, mature, and composed.
- Use restraint in color, motion, elevation, and promotional language.
- Never mimic trading terminals, crypto products, private-bank marketing, or casino-like reward
  patterns.
- Avoid false precision, guaranteed-outcome language, urgency theatre, and celebratory motion tied
  to changes in wealth.

## 4. Visual direction: Quiet Heritage

The approved direction combines **Quiet Ledger** as the functional foundation with **Modern
Heritage** as a restrained brand layer.

Quiet Ledger governs forms, navigation, tables, charts, lists, states, and everyday use. Modern
Heritage may appear in the wordmark, authentication/onboarding moments, the net-worth hero, major
section titles, and printable summaries. When the two conflict, clarity and accessibility win.

### Required characteristics

- Warm light canvas, white data surfaces, deep ink text, and restrained borders.
- Deep teal is the primary interactive color; heritage navy anchors the brand.
- Antique gold is a decorative/highlight accent, not the default CTA or a status color.
- Typography and spacing establish hierarchy; shadows and gradients do not.
- Financial figures use tabular numerals.
- Cards use moderate rounding and quiet elevation rather than glass effects.

### Prohibited characteristics

- Gold text for body copy, input values, links, or small labels.
- Gold as a substitute for success, warning, selection, or focus states.
- Serif type in forms, navigation, tables, charts, or body copy.
- Dark-first screens for long forms.
- Purple/pink crypto gradients, glassmorphism, neon, excessive blur, or metallic textures.
- Ornamental dividers, crests, coins, crowns, or wealth/luxury clichés.
- Emoji as structural icons.

## 5. Foundation tokens

Token names are semantic. Components must consume semantic roles rather than hard-coded color
values. Values below are the design baseline; implementation may tune a value only after contrast
and cross-platform rendering are verified.

### 5.1 Light color roles

| Token | Baseline | Use |
|---|---:|---|
| `color.canvas` | `#F7F5EF` | Warm application background |
| `color.surface` | `#FFFFFF` | Cards, forms, sheets, dialogs |
| `color.surfaceSubtle` | `#EFEBE2` | Grouped sections and quiet highlights |
| `color.ink` | `#17212B` | Primary text and financial values |
| `color.inkSecondary` | `#52606D` | Secondary text and metadata |
| `color.border` | `#D7D2C7` | Decorative dividers and card separation |
| `color.controlBorder` | `#8B877E` | Input and interactive-control boundaries |
| `color.brandNavy` | `#182B45` | Brand anchors and heritage moments |
| `color.primary` | `#0F5C5A` | Primary actions, active navigation, links |
| `color.primaryPressed` | `#0B4746` | Pressed primary state |
| `color.primaryContainer` | `#D9EFEC` | Selected and informational tonal surfaces |
| `color.heritageGold` | `#A87428` | Large decorative accents and editorial rules only |
| `color.success` | `#1E6A45` | Positive state with text/icon label |
| `color.warning` | `#8A5A00` | Attention/stale state with text/icon label |
| `color.error` | `#B42318` | Error and destructive actions |
| `color.info` | `#2458A6` | Neutral informational state |
| `color.focus` | `#147D79` | Visible focus ring |

Light mode is the required MVP baseline. A dark theme is not implied by this document. If dark
mode enters approved scope, define and contrast-test a complete semantic token set; do not invert
the light palette mechanically.

#### React Native Paper MD3 handoff

Production components consume the project semantic roles first; the Paper theme is an adapter,
not the design authority. Use these approved mappings:

| Project role | React Native Paper MD3 role |
|---|---|
| `color.canvas` | `colors.background` |
| `color.surface` | `colors.surface`, `colors.onPrimary`, and `colors.onError` |
| `color.surfaceSubtle` | `colors.surfaceVariant` |
| `color.ink` | `colors.onBackground` and `colors.onSurface` |
| `color.inkSecondary` | `colors.onSurfaceVariant` |
| `color.primary` | `colors.primary` and `colors.onPrimaryContainer` |
| `color.primaryContainer` | `colors.primaryContainer` |
| `color.error` | `colors.error` |
| `color.border` | `colors.outlineVariant`; decorative separation only |
| `color.controlBorder` | `colors.outline`; interactive boundaries |

Keep `color.brandNavy`, `color.primaryPressed`, `color.heritageGold`, `color.success`,
`color.warning`, `color.info`, and `color.focus` as typed project-theme extensions because MD3 has
no single faithful role for their approved meaning. Frontend issue #37 owns the implementation of
this adapter and must keep the MVP theme light-only.

### 5.2 Typography

- Primary family: **IBM Plex Sans**, with system sans-serif fallback.
- Brand/editorial family: **Noto Serif Display**, with a suitable system serif fallback.
- Serif is optional and restricted to wordmark, display titles at 24pt or larger, printable cover
  titles, and the net-worth hero label. Critical values remain sans-serif.
- All Vietnamese diacritics must render correctly at every supported weight before a font is
  approved for production.
- Use tabular figures for amounts, percentages, dates, rates, and chart labels.

| Role | Size / line height | Weight | Typical use |
|---|---:|---:|---|
| Display | 32 / 40 | 600 | Net-worth value, rare hero moments |
| Heading 1 | 24 / 32 | 600 | Screen title |
| Heading 2 | 20 / 28 | 600 | Section title |
| Title | 18 / 24 | 600 | Cards and dialogs |
| Body | 16 / 24 | 400 | Default copy and form values |
| Body strong | 16 / 24 | 600 | Important labels and row values |
| Supporting | 14 / 20 | 400–500 | Dates, helper text, metadata |
| Label | 12 / 16 | 500 | Compact labels; never critical body content |

Map these roles to Paper typography as follows: Display to `displaySmall`, Heading 1 to
`headlineSmall`, Heading 2 to `titleLarge`, Title to `titleMedium`, Body to `bodyLarge`, Body
strong to `labelLarge`, Supporting to `bodyMedium`, and Label to `labelSmall`. Font loading and
weight availability must still be verified on every production platform by issue #37.

Support platform text scaling. Prefer wrapping over truncation. At large text sizes, reflow cards
and rows vertically rather than hiding or clipping amounts and labels.

### 5.3 Spacing, sizing, and layout

- Base spacing rhythm: 4pt; preferred gaps and padding use 8pt increments.
- Screen horizontal padding: 16pt on phones, 24pt on tablets, 32pt on desktop containers.
- Section spacing: 24–32pt; related control spacing: 8–16pt.
- Minimum touch target: 44×44pt on iOS/web and 48×48dp on Android.
- Minimum gap between adjacent touch targets: 8pt.
- Form control height: at least 48pt; primary button height: 52–56pt.
- Card radius: 12–16pt; input radius: 10–12pt; sheet/dialog radius: 20–24pt.
- Use a restrained elevation scale: flat, raised card, modal. Borders are preferred over shadows
  for ordinary grouping.
- Interactive-control boundaries use `color.controlBorder`; the quieter `color.border` must not
  be the only visual boundary for an input or control.
- Long forms and explanatory text use a maximum content width of 640–720px on large screens.

The approved elevation recipes are:

- Flat: level 0, no shadow, with the ordinary `color.border` separation where grouping is needed.
- Raised card: Paper elevation level 1 or the platform equivalent of
  `0 2px 8px rgba(24, 43, 69, 0.12)`; use only when interaction or hierarchy needs separation.
- Modal: Paper elevation level 3 or the platform equivalent of
  `0 12px 32px rgba(24, 43, 69, 0.22)` over an `rgba(23, 33, 43, 0.56)` scrim.

### 5.4 Icons and imagery

- Use one consistent vector icon family with matching stroke weight.
- Navigation icons always include text labels.
- Icon-only actions require an accessible label and a full-size hit area.
- Core financial workflows should not depend on illustrations.
- If illustrations are introduced for onboarding or empty states, keep them abstract, mature, and
  culturally neutral; never depict guaranteed growth or effortless wealth.

### 5.5 Motion

- Tap feedback appears within 100ms.
- Micro-interactions last approximately 150–300ms and communicate cause and effect.
- Forward/back transitions preserve spatial direction and platform conventions.
- Loading longer than 300ms uses a stable skeleton or progress indicator with reserved space.
- Avoid animating financial values in a way that delays reading or dramatizes gains/losses.
- Respect reduced-motion settings; no workflow may depend on animation for comprehension.

## 6. Navigation and responsive shell

### 6.1 Primary destinations

The mobile bottom navigation contains exactly five labeled destinations, in this order:

1. **Tổng quan**
2. **Tài sản & Nợ**
3. **Dòng tiền**
4. **Mục tiêu**
5. **Nhận định**

Profile and settings open from the top app bar avatar. Do not add a hamburger menu for primary
destinations. Do not mix a drawer and bottom navigation at the same hierarchy level.

### 6.2 Global create action

A labeled **Thêm** action remains reachable from the lower thumb zone and opens a short action
sheet for:

- Tài sản.
- Công nợ.
- Thu nhập.
- Chi phí.
- Mục tiêu.

The action sheet only selects the record type. The actual form opens as a navigable screen so
back behavior, drafts, keyboard avoidance, validation, and deep links remain predictable.

### 6.3 Responsive behavior

- Design and review at 360px and 375px first, then 768px, 1024px, and 1440px.
- Widths below 768px use bottom navigation and a single main content column.
- Widths from 768px through 1023px use a navigation rail and may use one or two content columns
  when this improves comparison.
- Widths of 1024px and above use a stable left sidebar and a centered, bounded content area.
- Names, order, selected state, and information hierarchy remain consistent across breakpoints.
- Avoid nested scroll areas and horizontal page scrolling.
- Fixed bars reserve space for safe-area insets and never cover content or primary actions.
- System back, iOS swipe-back, and Android predictive back must not be intercepted by custom
  gestures.

## 7. Screen hierarchy guardrails

### 7.1 Tổng quan

Mobile order:

1. Net worth and last update time.
2. Up to three highest-priority conditions requiring attention.
3. Total assets, total liabilities, and debt-to-asset ratio.
4. Contextual quick actions.
5. Asset composition and liquidity.
6. Five largest assets.
7. Upcoming liability payments or maturity dates.

Do not give all KPIs equal visual weight. Charts appear after the primary answer and must reconcile
with the visible underlying records.

### 7.2 Tài sản & Nợ

- Use a segmented control for **Tài sản** and **Công nợ** within one top-level destination.
- Rows show name, category, reporting-currency value, original currency when relevant, and
  valuation/update date.
- Search is immediately available; filters use a bottom sheet on mobile.
- Asset detail prioritizes current value and freshness, then metadata, valuation history, linked
  cash flow, and secondary actions.
- Edit and delete belong in a predictable overflow area; deletion requires confirmation and, when
  feasible, a recoverable undo state.

### 7.3 Dòng tiền

- Default to the current month with a clearly labeled period switcher.
- Lead with income, expense, and net cash flow.
- Forecast months are interactive and traceable to recurring entries and scheduled liabilities.
- Negative forecasts use text, icon, and color together and provide a clear inspection path.

### 7.4 Mục tiêu

- Goal cards show name, progress, time remaining, monthly contribution need, and a text status.
- Status labels are **Đúng kế hoạch**, **Có rủi ro**, and **Chậm kế hoạch**, or approved equivalents;
  never rely on green/amber/red alone.
- Projection views expose user-entered expected return and state that it is not guaranteed.

### 7.5 Nhận định

Group results into **Đang ổn**, **Cần chú ý**, and **Hành động có thể cân nhắc**. Each item follows:

> Quan sát → Ngưỡng tham chiếu → Dữ liệu liên quan → Hành động đề xuất → Lưu ý

Every item links to the affected record or metric when one exists. Avoid chat bubbles, assistant
avatars, or conversational AI styling.

## 8. Forms and data entry

- Use a short common form first; type-specific details are collapsed until relevant.
- Keep labels permanently visible. Place units and currency adjacent to values, not only in
  placeholders.
- Validate on blur for most fields and again on submit. Show cause and recovery directly below the
  field.
- Focus the first invalid field after submission and provide an accessible error summary when
  several fields fail.
- Mark required fields explicitly; group related fields semantically and visually.
- Show loading on the submitting action, prevent duplicate submission, and confirm success.
- Warn before dismissing a form with unsaved changes.
- Never preselect a financially meaningful answer merely to shorten the form.
- Dates use a Vietnamese-friendly display while preserving an unambiguous stored value.
- Negative values are accepted only where the product contract explicitly permits them.

## 9. Money, dates, and financial data

- VND is the primary reporting value. Use locale-aware grouping and the `₫` or `VND` unit
  consistently; never mix conventions within one view.
- USD inputs display the original USD amount and the VND conversion where decisions are made.
- Do not show unnecessary decimal places. Preserve calculation precision without implying visual
  precision that the manual valuation cannot support.
- Use en dashes or an explicit “Không khả dụng” state when a ratio cannot be calculated; never
  display zero as a fallback for missing/invalid data.
- Timestamps and valuation dates must be distinguishable. Use absolute dates for audit-relevant
  data; relative time may supplement but not replace them.
- Masking financial values may be offered as a display preference, but the hidden state must be
  clear and reversible.

## 10. Charts and data visualization

- Trend over time: line chart.
- Assets versus liabilities: comparison bar.
- Category or liquidity composition: donut only when there are five or fewer visible groups;
  otherwise use a sorted bar chart or group small categories as “Khác.”
- Direct-label small datasets to reduce eye travel.
- Use shape, label, or pattern in addition to color for meaningful distinctions.
- Interactive points and segments receive a 44pt-equivalent touch target or expand on touch.
- Provide exact values on tap/focus and a text/table alternative for assistive technology.
- Every chart has a short accessible summary describing its primary insight.
- Empty and failed charts show guidance or retry action, never an empty axis frame.
- Keep grid lines and decoration subordinate to the data.

## 11. Required component states

Every reusable data or interactive component defines, where applicable:

- Default, pressed, focused, selected, and disabled.
- Loading and skeleton.
- Empty with a relevant next action.
- Partial data.
- Stale data.
- Validation error and request error with recovery.
- Success confirmation.
- Offline or unreachable service state when the platform can detect it.

Do not hide a destination solely because it has no data. Show the destination, explain the empty
state, and offer the correct next action.

### 11.1 Shared state anatomy and ownership

- Empty identifies what is absent and provides the relevant next action.
- Loading reserves stable space and does not delay reading with animated financial values.
- Partial identifies available information, missing inputs, affected interpretation, and a
  completion path.
- Stale combines text with an icon or shape, includes an absolute date, identifies the affected
  value, and provides an update path.
- Validation errors identify the cause and correction inline; multi-error submissions include a
  summary and focus the first invalid field.
- Request and offline errors state what failed, what was preserved, and the retry or safe-return
  path.
- Success states identify what changed and the logical next action.
- Disabled controls use native semantics and show the reason nearby when it is not self-evident.
- Destructive and unsaved-change states name the consequence, keep a safe cancel route, and use
  undo when feasible.
- An unavailable target uses the non-enumerating message “Không thể mở mục này” and a safe return
  path; it does not confirm record existence, account identity, or permissions.

Issue #36 owns this shared visual anatomy. Issue #37 owns its production component and shell
implementation. Identity issue #40 owns session, account, ownership, and access behavior. Later
domain work owns record-specific validation, transport errors, destructive consequences, and undo
policy.

## 12. Accessibility baseline

- Meet WCAG 2.2 AA contrast: 4.5:1 for normal text and 3:1 for large text and meaningful UI
  graphics.
- Screen reader order follows visual order. Route changes move focus to the new main heading.
- All interactive elements expose the correct role, name, value, state, and hint when needed.
- Focus indicators remain visible and are never removed without an accessible replacement.
- Support keyboard operation on web and platform accessibility actions on native targets.
- Do not disable browser zoom.
- Test Dynamic Type/font scaling at the largest practical setting without losing critical data or
  actions.
- Modals and sheets have a clear close/cancel route; focus returns to the invoking element.
- Toasts do not steal focus and are announced politely.
- Destructive actions are separated spatially and visually from ordinary actions.

## 13. Content and tone

- Vietnamese is the primary product language for the pilot. Prefer direct, professional,
  non-promotional sentences.
- Assume financial fluency, but explain application-specific calculations and rule triggers.
- Use “bạn” sparingly and naturally. Avoid childish coaching, exclamation marks, and gamification.
- Distinguish facts, estimates, forecasts, reference thresholds, and user-entered assumptions.
- Recommendations are informational and conditional. Never promise outcomes or imply fiduciary
  advice.
- Error messages state what happened, what was preserved, and what the user can do next.

## 14. Design review checklist

A design task is ready for frontend implementation only when the review can answer **yes** to all
applicable items:

### Product integrity

- Does the screen support an approved MVP 1A–1D behavior?
- Are VND reporting, USD conversion, source dates, and manual-data limitations visible where
  relevant?
- Can every aggregate, forecast, projection, and insight be traced to its inputs?

### Mobile usability

- Does the 360/375px layout work without horizontal scrolling?
- Is the primary action reachable with one hand and clear above the keyboard/safe area?
- Are all touch targets and gaps large enough?
- Does back navigation preserve draft, filters, and scroll position as appropriate?

### Responsive behavior

- Are phone, tablet, desktop, and landscape behaviors specified?
- Does navigation retain the same hierarchy and labels at every breakpoint?
- Are long forms and explanations bounded on larger screens?

### Accessibility

- Are contrast, text scaling, keyboard navigation, screen-reader order, focus behavior, and
  reduced motion specified?
- Does every color-coded state also have a label or icon?
- Is there an accessible alternative to each chart?

### State coverage

- Are loading, empty, partial, stale, error, success, disabled, and destructive states designed?
- Do errors include a recovery path and preserve valid user input?

### Visual consistency

- Does Quiet Ledger govern the functional UI?
- Is Modern Heritage limited to approved brand moments?
- Are semantic tokens and shared components used instead of page-specific styling?
- Are gold, serif, motion, elevation, and decoration within the guardrails above?

## 15. Governance and extension

- Shared tokens and components are the default. A design task may introduce a new variant only
  when an existing variant cannot express a real product state.
- Page-specific decisions should be added under a clearly named section in this document or a
  linked page guideline when the page has enough durable exceptions to justify one.
- Do not copy token values into task descriptions. Link to this file and document only the
  task-specific behavior.
- Accessibility and responsive behavior are part of design acceptance, not a final polish phase.
- Pilot feedback may change these guidelines. Record approved changes here with the affected
  principle or token; do not preserve obsolete alternatives in production guidance.
- The approved issue #36 evidence is the
  [`design-system-responsive-shell-review.html`](design-system-responsive-shell-review.html)
  design-contract review board. It illustrates this contract but is not a second source of truth
  and does not approve the complete MVP clickable prototype described in section 16.

## 16. Product Owner clickable mockup contract

### 16.1 Objective and delivery boundary

When a design task requests a mockup, the required outcome is a **high-fidelity clickable
prototype for Product Owner review and approval**. It should feel coherent and realistic enough
for the Product Owner to experience the proposed UX/UI, but it is not production frontend
implementation.

The prototype must include:

- A complete MVP screen inventory and a visible map of how screens relate.
- Navigation between all major screens.
- Realistic, internally consistent mock financial data.
- Main happy-path interactions and traceable drill-down paths.
- Important dialog and bottom-sheet states.
- Relevant empty, loading, error, partial, stale, and success states.
- Form validation examples and recovery behavior.
- Mobile-first layouts, with representative responsive behavior where it changes the experience.

The prototype must not:

- Integrate real APIs or require a running backend.
- Implement real authentication, authorization, session renewal, or account recovery.
- Introduce production state management, caching, persistence, analytics, or telemetry.
- Recreate backend calculations as production business logic.
- Over-engineer application architecture, abstractions, routing, or component infrastructure.
- Be described as production-ready frontend code.

Static sign-in and sign-up screens may be represented to make the end-to-end journey reviewable,
but their actions only transition to mock states. Simulated delays and outcomes are acceptable
when they exist to demonstrate loading, error, validation, and success behavior.

### 16.2 Required separation of design deliverables

Every clickable-mockup deliverable must organize its artifact or accompanying handoff into these
six explicitly labeled layers. Do not mix screen-specific styling into the UX map or bury global
component decisions inside individual screens.

#### 1. UX structure

- Information architecture and complete screen map.
- Primary and secondary navigation model.
- Screen hierarchy and mobile content priority.
- Entry points, back paths, modal boundaries, and responsive navigation changes.

#### 2. Design system

- Color roles from section 5.1.
- Typography hierarchy from section 5.2.
- Spacing scale, layout widths, touch sizes, and border-radius rules from section 5.3.
- Icon, elevation, motion, accessibility, and data-visualization rules.
- A compact token reference available inside or beside the prototype for review.

The mockup may implement only the tokens it actually uses, but it must not invent conflicting
page-specific values.

#### 3. Reusable components

The prototype component inventory must cover the following minimum set and demonstrate the states
that materially change appearance or behavior:

| Component family | Required variants or behavior |
|---|---|
| Buttons | Primary, secondary, tonal, text/icon, destructive; default, pressed, focused, disabled, loading |
| Inputs | Text, currency, number, date, select, search, textarea; label, helper, error, disabled, read-only |
| Cards | KPI, record summary, insight, goal, empty/next-action; tappable and non-tappable states |
| Lists | Asset, liability, cash-flow, obligation, valuation-history rows; dividers, metadata, empty state |
| Tabs | Top/section tabs and segmented controls; selected, unselected, focused, disabled when relevant |
| Bottom navigation | Five approved destinations, labels, active state, safe-area behavior, optional badge |
| Dialogs | Confirmation, destructive confirmation, unsaved changes, blocking error |
| Bottom sheets | Global create menu, filters, lightweight selection, contextual details; clear dismiss route |
| Status indicators | Success, warning, error, info, stale, on-track/at-risk/behind; icon and text, never color only |
| Feedback | Skeleton, progress, inline validation, banner, snackbar/toast, retry and success confirmation |

Components should be reusable enough to keep the mockup visually consistent, but they do not need
production-grade APIs, exhaustive configurability, or a standalone component package.

#### 4. Screens

The screen inventory below is the baseline for a complete MVP mockup. A task may deliver it in
reviewable increments, but the master inventory must remain visible and mark each screen as not
started, drafted, review-ready, approved, or intentionally out of scope.

**Entry and onboarding**

- Welcome/sign-in and sign-up simulation.
- Personal profile setup.
- USD/VND exchange-rate setup.
- Financial-profile checklist.
- Guided add-cash/bank-account step.
- Guided add-major-assets step.
- Guided add-liabilities step.
- Guided add-recurring-cash-flow step.
- First-dashboard review and onboarding completion.

**Tổng quan**

- Wealth snapshot dashboard.
- KPI calculation/source explanation.
- Data-freshness overview.
- Upcoming-obligation detail.
- Representative empty, partial, loading, error, and stale dashboard states.

**Tài sản & Nợ**

- Asset catalog, search, and filter.
- Liability catalog, search, and filter.
- Global create-action sheet.
- Asset category selection and common asset form.
- Type-specific asset details.
- Asset detail and edit.
- Add/update valuation and valuation history.
- Liability category selection and liability form.
- Liability detail and edit, including schedule and linked collateral.
- Safe-delete, unsaved-change, validation-error, loading, empty, and success states.

**Dòng tiền**

- Monthly overview and period selection.
- Income/expense catalog, search, and filter.
- Add/edit one-time entry.
- Add/edit recurring entry and recurrence details.
- Income and expense composition drill-down.
- Three-month forecast.
- Forecast-month source breakdown and upcoming-obligation linkage.
- Empty, negative-forecast, loading, error, validation, and success states.

**Mục tiêu**

- Goal list.
- Goal-template selection.
- Create/edit goal form.
- Goal detail, progress, shortfall, and projection explanation.
- On-track, at-risk, behind, empty, validation, and success states.

**Nhận định**

- Grouped rule-based report.
- Insight detail with observed metric, reference threshold, source data, suggested action, and
  disclaimer.
- Linked-record navigation.
- Positive, attention, resolved, empty, loading, and error states.

**Hồ sơ & cài đặt**

- Personal profile.
- Exchange rate and update date.
- Display preferences supported by approved scope.
- Privacy-safe in-product feedback form.
- Sign-out confirmation simulation.

#### 5. User flows

At minimum, connect and demonstrate these complete happy paths:

1. Enter the simulated account, set the exchange rate, add initial wealth records, and reach the
   first useful dashboard.
2. Add an asset through progressive disclosure and inspect its converted value.
3. Update a stale asset valuation and verify that valuation history is retained.
4. Add a liability, including its next payment date and optional collateral.
5. Add recurring income and expense records, inspect the monthly result, then trace a forecast
   month to its source records.
6. Create a goal from a template, adjust an assumption, and inspect the updated projection.
7. Open a rule-based insight, inspect its metric and threshold, navigate to the affected record,
   update it, and see a simulated resolved state.
8. Trigger and recover from a representative validation error, request error, unsaved-change
   warning, and destructive confirmation.

Each flow must identify its start state, user actions, decision points, completion state, and safe
way back. The primary happy path must not depend on a hidden gesture or hover interaction.

#### 6. Mock interactions

- All primary navigation items, major cards, list rows, CTAs, back controls, and explicit close
  controls are clickable.
- Forms demonstrate progressive disclosure, correct input type, inline validation, disabled and
  loading submission, and a success result.
- Dialogs and bottom sheets demonstrate open, dismiss, confirm, cancel, and unsaved-change paths.
- Filters and segmented controls visibly change the presented mock state.
- Charts expose values and drill-down on tap/focus; an accessible textual equivalent is present.
- Simulated loading must resolve deterministically to a designed state; avoid random outcomes.
- Prototype state may reset on reload unless persistence itself is under review.
- Links or routes should be stable enough for reviewers to return directly to major screens.

### 16.3 Mock data baseline

Use fictional, non-sensitive, Vietnamese-market data. Avoid lorem ipsum, round-number-only
dashboards, impossible dates, and values that do not reconcile across screens. A recommended
baseline fixture is:

- Reporting currency: VND.
- User-entered exchange rate: 1 USD = 25,100 VND, updated 12/09/2026.
- Assets: 650,000,000 VND cash/bank; 1,200,000,000 VND term deposit; 2,150,000,000 VND listed
  securities; 7,500,000,000 VND real estate; 20,000 USD deposit converted to 502,000,000 VND.
- Total assets: 12,002,000,000 VND.
- Liabilities: 1,500,000,000 VND mortgage and 32,000,000 VND credit-card balance.
- Total liabilities: 1,532,000,000 VND; net worth: 10,470,000,000 VND.
- Monthly income: 180,000,000 VND; monthly expenses: 92,000,000 VND; net cash flow:
  88,000,000 VND.
- At least one stale valuation, one upcoming payment, one concentration insight, and one goal with
  an at-risk projection.

Alternative fixtures are acceptable when a task requires them, but every displayed aggregate,
chart, insight, and projection must reconcile with its source records.

### 16.4 Prototype review gate

A clickable mockup is ready for Product Owner review only when:

- The master screen inventory is present and coverage is explicit.
- All major screens are reachable without dead-end navigation.
- The eight required happy/recovery flows can be completed from their documented start states.
- The prototype demonstrates important dialogs, bottom sheets, validation, and system states.
- Mock values reconcile across lists, KPIs, charts, forecasts, goals, and insights.
- The 360px and 375px experiences have been reviewed for thumb reach, keyboard overlap, safe areas,
  text wrapping, and horizontal overflow.
- At least one tablet/desktop view demonstrates the intended adaptive navigation and content width.
- The prototype is clearly labeled as a design-review artifact using simulated data and behavior.
- The Product Owner can record approval, requested changes, or rejection by screen and by flow.

Approval of the prototype authorizes UX/UI direction only. It does not certify production
architecture, real calculations, API behavior, security, authentication, or implementation
readiness beyond the design contract.
