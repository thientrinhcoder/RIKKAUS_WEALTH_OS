# Reconciles one GET /advisor-report body (.) with the GET /wealth-snapshot/insights ($snap),
# GET /financial-health?month= ($fh) and GET /goal-projections ($gp) bodies read right after it for the
# same user and month, and checks the report's own invariants (scenario A24). Prints an object of named
# booleans; every one must be true.
$snap[0] as $s | $fh[0] as $h | $gp[0] as $g | . as $root |
(.unevaluated.findings + .positive + .attention) as $all |
def r6(x): (x * 1000000 | round) / 1000000;
def near(a; b): (a == null and b == null) or (a != null and b != null and ((a - b) | fabs) < 0.0000011);
def key: [.sourceType, .sourceId];
def keyset(rows): [rows[] | key] | unique;
def assum($p; $k): [$p.assumptions[] | select(.key == $k) | .value][0];
def smap: {OK: "OK", ATTENTION: "ATTENTION", UNAVAILABLE: "NOT_EVALUATED"};
def sprop: {CATEGORY_CONCENTRATION: "max-category-share", DEBT_TO_ASSET_RATIO: "max-debt-to-asset-ratio",
  LOW_LIQUIDITY_SHARE: "max-low-liquidity-share", UPCOMING_LIABILITY_DUE: "due-window-days",
  STALE_VALUATION: "max-valuation-age-days", STALE_EXCHANGE_RATE: "max-exchange-rate-age-days"};
def place: {UPCOMING_LIABILITY_DUE: [1, "RECONCILE_CASH_WITH_OBLIGATIONS"],
  LIQUIDITY_SHORT_FOR_UPCOMING_OBLIGATIONS: [1, "RECONCILE_CASH_WITH_OBLIGATIONS"],
  FORECAST_CASH_NEGATIVE: [1, "RECONCILE_CASH_WITH_OBLIGATIONS"],
  SPENDING_EXCEEDS_INCOME: [2, "REVIEW_PERIOD_SPENDING"], HIGH_DEBT_PAYMENT_BURDEN: [2, "REVIEW_DEBT_PAYMENTS"],
  RESERVE_BELOW_REFERENCE: [3, "RECONCILE_CASH_WITH_RESERVE"], LOW_LIQUIDITY_SHARE: [3, "RECONCILE_CASH_WITH_RESERVE"],
  INCOME_CONCENTRATION: [3, "REVIEW_INCOME_SOURCES"], CATEGORY_CONCENTRATION: [3, "REVIEW_ASSET_CONCENTRATION"],
  DEBT_TO_ASSET_RATIO: [3, "REVIEW_DEBT_LEVEL"], GOAL_CONTRIBUTION_COVERAGE: [4, "REVIEW_GOAL_PLAN"],
  STALE_VALUATION: [5, "REFRESH_RECORD"], STALE_EXCHANGE_RATE: [5, "REFRESH_RECORD"]};
def action_id: {RECONCILE_CASH_WITH_OBLIGATIONS: "COMPARE_CASH_WITH_UPCOMING_OBLIGATIONS",
  REVIEW_PERIOD_SPENDING: "REVIEW_LARGEST_PERIOD_EXPENSES", REVIEW_DEBT_PAYMENTS: "CHECK_DEBT_PAYMENTS_AGAINST_INCOME",
  RECONCILE_CASH_WITH_RESERVE: "COMPARE_LIQUID_ASSETS_WITH_RESERVE", REVIEW_INCOME_SOURCES: "REVIEW_INCOME_SOURCE_MIX",
  REVIEW_ASSET_CONCENTRATION: "REVIEW_ASSET_CATEGORY_SHARE", REVIEW_DEBT_LEVEL: "REVIEW_LIABILITIES_AGAINST_ASSETS",
  REVIEW_GOAL_PLAN: "CONSIDER_GOAL_PLAN_OPTIONS", REFRESH_RECORD: "UPDATE_STALE_RECORD"};
def route_of($r): {ASSET: "/api/v1/assets/", LIABILITY: "/api/v1/liabilities/", CASH_FLOW_ENTRY: "/api/v1/cash-flow-entries/",
  GOAL: "/api/v1/goals/"}[$r.sourceType] as $p | if $r.sourceType == "EXCHANGE_RATE" then "/api/v1/settings/exchange-rate" else $p + $r.sourceId end;
def finding($id): $all[] | select(.findingId == $id);
def deviation($f): if $f.observed == null or $f.observed.value == null or $f.threshold.value == null or $f.threshold.value == 0 then null
  else r6((($f.observed.value - $f.threshold.value) | fabs) / ($f.threshold.value | fabs)) end;
def shares($a; $b): any($a.affectedRecords[] | key; . as $k | any($b.affectedRecords[]; key == $k));
def connected($fs): reduce range($fs | length) as $_ ([$fs[0]]; . as $in | $in + [$fs[] | select(. as $c | ($in | index([$c]) | not) and any($in[]; shares(.; $c)))] | unique_by(.findingId)) | length == ($fs | length);
def sort_key: [.tier, (.dueDate // "9999-12-31"), (if .relativeDeviation == null then 1e18 else -.relativeDeviation end), .suggestionId];
{
  month_is_health_month: (.month == $h.assumptions.month),
  as_of_is_health_day: (.asOfDate == $h.assumptions.asOfDate and .timeZone == $h.assumptions.timeZone),
  three_sources: ([.sources[] | [.source, .route, .findingCount]] == [["WEALTH_SNAPSHOT", "/api/v1/wealth-snapshot/insights", ($s.insights | length)],
    ["FINANCIAL_HEALTH", ("/api/v1/financial-health?month=" + $h.assumptions.month), ($h.rules | length)], ["GOAL_PROJECTION", "/api/v1/goal-projections", ($g | length)]]),
  priority_marker_count_is_3: (.priorityMarkerCount == 3),
  disclaimer_present: ((.disclaimer | type) == "string" and (.disclaimer | test("not financial advice"))),
  every_upstream_result_once: (($all | length) == ($s.insights | length) + ($h.rules | length) + ($g | length) and ([$all[].findingId] | unique | length) == ($all | length)),
  unevaluated_count: (.unevaluated.count == (.unevaluated.findings | length)),
  groups_by_status: (all(.unevaluated.findings[]; .status == "NOT_EVALUATED") and all(.positive[]; .status == "OK") and all(.attention[]; .status == "ATTENTION")),
  reason_iff_unevaluated: (all($all[]; (.reason != null) == (.status == "NOT_EVALUATED"))),
  observed_null_iff_unevaluated: (all($all[]; (.observed == null) == (.status == "NOT_EVALUATED"))),
  snapshot_echo: ([$s.insights[] as $i | finding("WEALTH_SNAPSHOT:" + $i.ruleId) as $f |
    $f.source == "WEALTH_SNAPSHOT" and $f.ruleId == $i.ruleId and $f.status == smap[$i.status]
    and (if $i.observed == null then $f.observed == null else $f.observed == ($i.observed | {value, unit, subject, date, numeratorVnd, denominatorVnd}) end)
    and $f.threshold == ($i.threshold + {property: ("rikkaus.snapshot.insights." + sprop[$i.ruleId])})
    and $f.reason == $i.unavailableReason
    and ([$f.affectedRecords[] | key] | sort) == keyset($i.evidence) and ([$f.affectedRecords[] | key] | unique | length) == ($f.affectedRecords | length)
    and all($f.affectedRecords[]; . as $r | any($i.evidence[]; key == ($r | key) and .name == $r.name))
    and $f.dueDate == (if $i.ruleId == "UPCOMING_LIABILITY_DUE" then $i.observed.date else null end)
    and $f.noteId == (if any($i.evidence[]; .sourceType == "EXCHANGE_RATE" or .currency == "USD") then "USER_DECLARED_EXCHANGE_RATE" else "MANUAL_ENTRY_NOT_RECONCILED" end)
  ] | length == ($s.insights | length) and all),
  health_echo: ([$h.rules[] as $r | finding("FINANCIAL_HEALTH:" + $r.ruleId) as $f |
    (if $r.dependsOn == null then null else ($h.indicators[] | select(.indicatorId == $r.dependsOn) | .calculation) end) as $c |
    $f.source == "FINANCIAL_HEALTH" and $f.ruleId == $r.ruleId and $f.status == $r.status
    and (if $r.observed == null then $f.observed == null else $f.observed == ($r.observed + {numeratorVnd: $c.numeratorVnd, denominatorVnd: $c.denominatorVnd}) end)
    and $f.threshold == ($r.threshold + {property: (if $r.threshold.property == null then null else "rikkaus.cashflow.health." + $r.threshold.property end)})
    and $f.reason == $r.notEvaluatedReason
    and ([$f.affectedRecords[] | key] | sort) == keyset($r.evidence) and ([$f.affectedRecords[] | key] | unique | length) == ($f.affectedRecords | length)
    and all($f.affectedRecords[]; .name == null)
    and $f.dueDate == (if $r.ruleId == "LIQUIDITY_SHORT_FOR_UPCOMING_OBLIGATIONS" then ([$r.evidence[] | select(.role == "PAYMENT_DUE" or .role == "MATURITY_DUE") | .date] | min) else null end)
    and $f.noteId == (if $r.ruleId == "FORECAST_CASH_NEGATIVE" then "FORECAST_FROM_ENTERED_SCHEDULES" elif any($r.evidence[]; .currency == "USD") then "USER_DECLARED_EXCHANGE_RATE" else "MANUAL_ENTRY_NOT_RECONCILED" end)
  ] | length == ($h.rules | length) and all),
  goal_echo: ([$g[] as $p | finding("GOAL_PROJECTION:GOAL_CONTRIBUTION_COVERAGE:" + $p.goalId) as $f |
    $f.source == "GOAL_PROJECTION" and $f.ruleId == "GOAL_CONTRIBUTION_COVERAGE"
    and $f.status == (if $p.status == null then "NOT_EVALUATED" elif $p.status == "ON_TRACK" then "OK" else "ATTENTION" end)
    and (if $p.status == null then $f.observed == null else $f.observed == {value: $p.contributionCoveragePercent, unit: "PERCENT", subject: null, date: null,
      numeratorVnd: (assum($p; "PLANNED_MONTHLY_CONTRIBUTION") | tonumber), denominatorVnd: $p.requiredMonthlyContribution} end)
    and $f.threshold == {comparison: "LESS_THAN", value: (assum($p; "ON_TRACK_MIN_COVERAGE_PERCENT") | tonumber), unit: "PERCENT", property: "rikkaus.goals.projection.on-track-min-coverage-percent"}
    and $f.dueDate == assum($p; "TARGET_DATE")
    and $f.affectedRecords == [{sourceType: "GOAL", sourceId: $p.goalId, name: $p.goalName, route: ("/api/v1/goals/" + $p.goalId)}]
    and $f.noteId == "USER_ASSUMED_RETURN"
    and $f.reason == (if $p.status != null then null else ($p.unavailableReasons[0] // ("TIMELINE_" + $p.timeline)) end)
  ] | length == ($g | length) and all),
  tier_and_family_iff_attention: (all($all[]; ((.tier != null) == (.status == "ATTENTION")) and ((.actionFamily != null) == (.status == "ATTENTION")))),
  tier_and_family_by_rule: (all(.attention[]; [.tier, .actionFamily] == place[.ruleId])),
  deviation_formula: (all($all[]; if .source == "GOAL_PROJECTION" then near(.relativeDeviation; (if .observed == null then null else r6((100 - .observed.value) / 100) end)) else near(.relativeDeviation; deviation(.)) end)),
  record_routes: (all($all[].affectedRecords[], .suggestions[].affectedRecords[]; .route == route_of(.))),
  suggestions_cover_attention_once: (([.suggestions[].findingIds[]] | sort) == ([.attention[].findingId] | sort)),
  suggestion_fields: (all(.suggestions[]; . as $sg | [$sg.findingIds[] as $id | finding($id)] as $fs |
    $sg.suggestionId == ($sg.findingIds | join("+"))
    and $sg.tier == ([$fs[].tier] | min)
    and all($fs[]; .actionFamily == $sg.actionFamily) and $sg.actionId == action_id[$sg.actionFamily]
    and $sg.dueDate == ([$fs[].dueDate | select(. != null)] | min)
    and near($sg.relativeDeviation; ([$fs[].relativeDeviation | select(. != null)] | max))
    and ($sg.noteIds | sort) == ([$fs[].noteId] | unique)
    and ([$sg.affectedRecords[] | key] | sort) == keyset([$fs[].affectedRecords[]]) and ([$sg.affectedRecords[] | key] | unique | length) == ($sg.affectedRecords | length)
    and ($sg.findingIds == ($fs | sort_by([.tier, (.dueDate // "9999-12-31"), (if .relativeDeviation == null then 1e18 else -.relativeDeviation end), .findingId]) | map(.findingId)))
    and connected($fs))),
  no_missed_merge: ([.suggestions as $ss | range($ss | length) as $i | range($i + 1; $ss | length) as $j | $ss[$i] as $a | $ss[$j] as $b |
    ($a.actionFamily != $b.actionFamily) or (([$a.affectedRecords[] | key] - [$b.affectedRecords[] | key]) == [$a.affectedRecords[] | key])] | all),
  ranks: ([.suggestions[].rank] == [range(1; (.suggestions | length) + 1)]),
  priority_on_first_three: ([.suggestions[].priority] == [range(.suggestions | length) | . < 3]),
  rank_order: ((.suggestions | map(sort_key)) == (.suggestions | map(sort_key) | sort)),
  action_language: (all(.suggestions[].action; test("^You may ") and endswith("then review the result.")
    and (test("(?i)\\b(guarantee|guaranteed|will|ensure|must|should|urgent|immediately)\\b") | not) and (contains("!") | not)))
}
