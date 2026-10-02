# Repository Agent Instructions

## UI and design work

Before planning, designing, implementing, reviewing, or testing any user interface, user flow,
responsive behavior, visual component, chart, form, or clickable mockup, read and follow
[`docs/design-guidelines.md`](docs/design-guidelines.md) in full.

The design guideline is the repository-level source of truth for the approved Quiet Heritage
direction, mobile-first navigation, reusable components, accessibility, responsive behavior, and
Product Owner clickable-mockup review contract. The MVP Product Backbone remains authoritative
for product scope.

### Design-only HTML review artifacts

When a design task changes only a static or self-contained HTML review artifact and does not
modify production application code or runtime configuration:

- Skip code-reviewer, tester, lint, typecheck, build, and other source-code verification gates.
- Do not spend tokens auditing implementation details that are not visible in the rendered
  artifact.
- Verify only by opening the HTML successfully and visually checking the requested layouts,
  states, interactions, and design direction at the task's target viewport sizes.
- Treat Product Owner visual acceptance as the completion gate.

If the task related to Backend-FrontEnd team, also changes production code, shared runtime contracts, dependencies, or
configuration, the normal implementation, review, and verification rules still apply to those
changes.

## Decisions and questions

Before asking the user to choose between approaches (`AskUserQuestion` or otherwise), present the
options and the reasoning behind them in visible response text first. Never ask the user to decide
on analysis they have not seen, and write every option so it stands alone. Internal reasoning is
invisible to the user, so externalize it before any decision point.

Every option must carry a real, detailed example drawn from this repository rather than an
abstract description: the actual file path, the real component name, the concrete label. For any
user-facing surface, product copy or financial value, write that example in Vietnamese with
realistic Vietnamese-market figures and formatting, such as `12.002.000.000 ₫` or
`cập nhật 12/09/2026`. No lorem ipsum, placeholder numbers, or English stand-ins for text that
will ship in Vietnamese.

## Hook responses

If the privacy-block hook emits a marker between `@@PRIVACY_PROMPT_START@@` and
`@@PRIVACY_PROMPT_END@@`, parse the JSON and ask the user for approval with `AskUserQuestion`.
If access is denied, continue without that file.
