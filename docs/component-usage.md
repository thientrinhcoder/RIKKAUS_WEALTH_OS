# Component usage

How to build a feature screen out of the MVP 0 component kit, and when you are allowed to add
something new.

The design contract is [`design-guidelines.md`](design-guidelines.md). This document does not
restate it; it maps the concerns a feature team actually hits to the primitive that owns each one.
Section references below point into the contract.

The executable version of this document is the catalog under `apps/mobile/src/app/(catalog)/`.
Every example there composes production primitives only, and a test fails if one forks.

## The rule that governs everything else

Section 15: **shared tokens and components are the default. A new variant may be introduced only
when an existing variant cannot express a real product state.**

"Cannot express" means the primitive has no prop, no slot and no composition that produces the
state. It does not mean the existing component is inconvenient, is one prop short of ideal, or
would need a wrapper. If you need a wrapper, write the wrapper in your feature; that is
composition, not a new variant.

If a primitive genuinely cannot express your state, that is a finding against the kit. Raise it on
the owning issue rather than building a second version beside it. A second version is how two
components that look alike start behaving differently.

## Where each concern lives

### Colour, type, spacing and elevation

| Concern | Use | Notes |
|---|---|---|
| Any colour | `useAppTheme().colors` | Never a hex literal. A test fails the build if one appears outside `src/ui/tokens.ts`. |
| Any text style | Paper's `variant` on `Text` | The eight approved roles are mapped in `src/ui/typography.ts`. |
| Spacing, radius, touch targets | `spacing`, `radius`, `sizing` from `@/ui/tokens` | Section 5.3. |
| Card and modal depth | `elevation` from `@/ui/elevation` | Three recipes only: flat, raised card, modal. |

Read the theme with `useAppTheme()`, not Paper's `useTheme()`. The typed version is what makes a
misspelled role a compile error instead of an undefined at runtime, and a test enforces it.

### Actions

| Concern | Use |
|---|---|
| Any button | `ActionButton` from `@/components/action` |
| A destructive button | the same component, `variant="destructive"` |
| An icon-only button | the same component with `icon` and a required `accessibilityLabel` |

There is one button family. The destructive variant carries a leading icon by default, because
section 12 requires the distinction to survive without colour.

### Forms

| Concern | Use |
|---|---|
| Text, currency, number, date, select, search, long text | the seven fields in `@/components/form` |
| Label, helper, error, required, disabled, read-only | `FieldShell`, which every field already composes |
| Submit, cancel, validation summary, focus-first-invalid, duplicate-submit prevention | `FormShell` |
| Whether a form holds unsaved work | `useUnsavedChanges` |
| Display grouping and date parsing | `formatGrouped`, `parseVietnameseDate` and friends |

`FormShell` takes your `validate` function. The kit decides *when* validation runs and what happens
to the result; it never learns what a valid valuation is. Your schema, your thresholds, your copy.

Read-only is not disabled. A converted amount the user may read and copy but not edit is read-only;
presenting it as disabled tells them it is broken.

### Lists, cards and discovery

| Concern | Use |
|---|---|
| A headline figure | `KpiCard` |
| One record as a card or a row | `RecordCard`, `RecordRow` |
| An insight, a goal, an empty slot | `InsightCard`, `GoalCard`, `EmptyCard` |
| A list with dividers and an empty state | `ListView` |
| Sibling sections of a screen | `Tabs` |
| Narrowing one set of content | `SegmentedControl` |
| Search and applied filters | `FilterBar` |
| Filters on a phone | `FilterSheet` with `BottomSheet` as its container |

There is one row primitive. The five row shapes in section 16.2 differ in which metadata they
carry, not in structure, so `RecordRow` is configured by props rather than copied per record type.

A value you do not have renders as unavailable, never as a zero. Pass `undefined`, not `0`.

### States and feedback

| Concern | Use | Section 11.1 requires |
|---|---|---|
| Loading | `LoadingState` | Reserved space, no value on screen |
| Nothing yet | `EmptyState` | What is absent, plus the next action |
| Incomplete inputs | `PartialState` | What is available, what is missing, the impact, the completion path |
| Out-of-date figure | `StaleState` | An absolute date, the affected value, an update path |
| Request or offline failure | `ErrorState` | What failed, what was preserved, retry or safe return |
| Something changed | `SuccessState` | What changed, plus the next action |
| A short status | `StatusIndicator` | Icon and text, label supplied by you |
| A persistent notice | `Banner` | Dismissible |
| A transient notice | `useSnackbar().show` | At least 5 seconds, queued, never the only carrier of a failure |

Each state makes its required parts required props, so a state missing its completion path does not
compile. That is deliberate: these are the clauses most often forgotten.

Goal vocabulary is yours. `StatusIndicator` ships five generic tones and takes your label;
on-track, at-risk and behind belong to the goals feature.

### Overlays and consequential actions

| Concern | Use |
|---|---|
| An ordinary confirmation | `useConfirm().confirm({ title, body, confirmLabel })` |
| A destructive confirmation | the same call plus `consequence` |
| Leaving a dirty form | `UnsavedChangesDialog`, driven by `useUnsavedChanges` |
| An error with one way out | `BlockingErrorDialog` |
| Any sheet | `BottomSheet` |

`confirm()` returns a promise. You do not manage open state, and a second call while one is open
resolves false rather than stacking a second dialog.

Name the consequence yourself. The kit requires that you do; it will not invent what deleting your
record costs.

Overlays that lose nothing close on a tap outside. Overlays that would discard data or confirm a
destructive action do not, because an accidental tap must not choose for the user. That is the
`dismissPolicy` prop, and the defaults are already right for each dialog.

### The shell

The shell renders itself around your screen. A feature screen renders its own content and one
`ScreenHeading`; it does not render navigation, the app bar or the create action.

## What a feature screen looks like

A screen in this product is a composition, not a set of new components:

```text
ScreenHeading            the destination name, which also receives focus on arrival
FilterBar                when the screen is a catalog
ListView + RecordRow     the records, or an EmptyState when there are none
LoadingState             while they are arriving
ErrorState               when they did not
useConfirm               before anything consequential
useSnackbar              after something small succeeded
```

If your screen needs something this list does not cover, check the catalog first. If the catalog
does not cover it either, that is the finding worth raising.

## The gates that enforce this

These run in the ordinary test suite. They are listed so you know what will stop a merge and why.

| Gate | What fails it |
|---|---|
| `__tests__/conformance/no-forks.test.ts` | A catalog screen defining its own input, pressable, dialog, colour or radius |
| `__tests__/conformance/no-domain-logic.test.ts` | Any shared component importing from `src/features/`, a route, or a fixture |
| `__tests__/conformance/catalog-excluded.test.ts` | The catalog becoming reachable from production navigation |
| `__tests__/conformance/accessible-names.test.tsx` | Any interactive element in the catalog without an accessible name |
| `__tests__/ui/token-discipline.test.ts` | A raw colour outside the token module, or Paper's untyped `useTheme` |
| `__tests__/ui/contrast.test.ts` | A token change dropping any required pair below its WCAG threshold |

The per-kit boundary scans under `__tests__/form`, `__tests__/data`, `__tests__/feedback` and
`__tests__/overlay` add the same discipline inside each slice.
