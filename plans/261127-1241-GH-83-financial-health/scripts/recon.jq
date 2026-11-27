# Reconciles one GET /financial-health body (.) with the GET /cash-flow-reports/monthly ($rep) and
# GET /cash-flow-forecast ($fc) bodies read right after it for the same user and month. Prints an object
# of named booleans; every one must be true. A check about an UNAVAILABLE indicator is vacuously true.
$rep[0] as $r | $fc[0] as $f | . as $root |
(.assumptions.essentialCategories) as $ess |
(.assumptions.debtPaymentCategories) as $debt |
def ind($id): .indicators[] | select(.indicatorId == $id);
def rule($id): .rules[] | select(.ruleId == $id);
def total(rows): [rows[] | .amountVnd // 0] | add // 0;
def r6(x): (x * 1000000 | round) / 1000000;
def avail($id): (ind($id) | .availability) != "UNAVAILABLE";
{
  month_matches: (.assumptions.month == $r.month),
  forecast_horizon_matches: (.assumptions.forecastHorizon.from == $f.horizon.from and .assumptions.forecastHorizon.to == $f.horizon.to),
  rate_matches_report: (.assumptions.exchangeRate == $r.exchangeRate),
  five_indicators_in_order: ([.indicators[].indicatorId] == ["EMERGENCY_RESERVE_MONTHS","DEBT_PAYMENT_BURDEN","FORECAST_MINIMUM_CASH_BALANCE","PASSIVE_INCOME_SHARE","ESSENTIAL_EXPENSE_SHARE"]),
  six_rules_in_order: ([.rules[].ruleId] == ["SPENDING_EXCEEDS_INCOME","FORECAST_CASH_NEGATIVE","RESERVE_BELOW_REFERENCE","HIGH_DEBT_PAYMENT_BURDEN","LIQUIDITY_SHORT_FOR_UPCOMING_OBLIGATIONS","INCOME_CONCENTRATION"]),
  value_null_iff_unavailable: ([.indicators[] | ((.value == null) == (.availability == "UNAVAILABLE"))] | all),
  no_zero_for_unavailable: ([.indicators[] | select(.availability == "UNAVAILABLE") | .calculation.numeratorVnd == null and .calculation.denominatorVnd == null] | all),
  observed_null_iff_not_evaluated: ([.rules[] | ((.observed == null) == (.status == "NOT_EVALUATED"))] | all),
  no_rule_judged_on_incomplete_indicator: ([.rules[] | select(.dependsOn != null) as $rl | (($root | ind($rl.dependsOn)).availability == "COMPLETE") or ($rl.status == "NOT_EVALUATED")] | all),
  reserve_numerator_is_its_rows: ((avail("EMERGENCY_RESERVE_MONTHS") | not) or (ind("EMERGENCY_RESERVE_MONTHS") | .calculation.numeratorVnd == total([.calculation.sources[] | select(.role == "NUMERATOR")]))),
  reserve_denominator_is_monthly_equivalents: ((avail("EMERGENCY_RESERVE_MONTHS") | not) or (ind("EMERGENCY_RESERVE_MONTHS") | .calculation.denominatorVnd == ([.calculation.sources[] | select(.role == "DENOMINATOR") | .monthlyEquivalentVnd // 0] | add))),
  reserve_value_is_ratio: ((avail("EMERGENCY_RESERVE_MONTHS") | not) or (ind("EMERGENCY_RESERVE_MONTHS") | .value == ((.calculation.numeratorVnd / .calculation.denominatorVnd * 10 | round) / 10))),
  debt_denominator_is_report_income: ((avail("DEBT_PAYMENT_BURDEN") | not) or (ind("DEBT_PAYMENT_BURDEN").calculation.denominatorVnd == $r.totals.income)),
  debt_numerator_is_report_debt_lines: ((avail("DEBT_PAYMENT_BURDEN") | not) or (ind("DEBT_PAYMENT_BURDEN").calculation.numeratorVnd == ([$r.occurrences[] | select(.entryType == "EXPENSE" and (.category as $c | $debt | index($c))) | .amount // 0] | add // 0))),
  debt_value_is_ratio: ((avail("DEBT_PAYMENT_BURDEN") | not) or (ind("DEBT_PAYMENT_BURDEN") | .value == r6(.calculation.numeratorVnd / .calculation.denominatorVnd))),
  passive_is_report_passive_over_income: ((avail("PASSIVE_INCOME_SHARE") | not) or (ind("PASSIVE_INCOME_SHARE") | .calculation.numeratorVnd == $r.passiveIncome.total and .calculation.denominatorVnd == $r.totals.income and .value == r6($r.passiveIncome.total / $r.totals.income))),
  passive_rows_sum: ((avail("PASSIVE_INCOME_SHARE") | not) or (ind("PASSIVE_INCOME_SHARE") | .calculation.numeratorVnd == total([.calculation.sources[] | select(.role == "SHARE_PART")]) and .calculation.denominatorVnd == total(.calculation.sources))),
  essential_is_report_composition: ((avail("ESSENTIAL_EXPENSE_SHARE") | not) or (ind("ESSENTIAL_EXPENSE_SHARE") | .calculation.denominatorVnd == $r.totals.expense and .calculation.numeratorVnd == ([$r.expenseComposition[] | select(.category as $c | $ess | index($c)) | .amount] | add // 0))),
  essential_rows_sum: ((avail("ESSENTIAL_EXPENSE_SHARE") | not) or (ind("ESSENTIAL_EXPENSE_SHARE") | .calculation.numeratorVnd == total([.calculation.sources[] | select(.role == "SHARE_PART")]) and .calculation.denominatorVnd == total(.calculation.sources) and .value == r6(.calculation.numeratorVnd / .calculation.denominatorVnd))),
  minimum_is_forecast_minimum: ((avail("FORECAST_MINIMUM_CASH_BALANCE") | not) or (ind("FORECAST_MINIMUM_CASH_BALANCE") | .value == $f.minimumBalance.amount and .calculation.observedOn == $f.minimumBalance.date)),
  minimum_opening_plus_path: ((avail("FORECAST_MINIMUM_CASH_BALANCE") | not) or (ind("FORECAST_MINIMUM_CASH_BALANCE") | .value == (total([.calculation.sources[] | select(.role == "OPENING_BALANCE")]) + total([.calculation.sources[] | select(.role == "PROJECTED_INCOME")]) - total([.calculation.sources[] | select(.role == "PROJECTED_EXPENSE")])))),
  minimum_opening_is_forecast_opening: ((avail("FORECAST_MINIMUM_CASH_BALANCE") | not) or (total([ind("FORECAST_MINIMUM_CASH_BALANCE").calculation.sources[] | select(.role == "OPENING_BALANCE")]) == $f.openingBalance.amount)),
  spending_is_report_net: ((rule("SPENDING_EXCEEDS_INCOME").status == "NOT_EVALUATED") or (rule("SPENDING_EXCEEDS_INCOME").observed.value == $r.totals.net)),
  liquidity_cash_is_forecast_opening: ((rule("LIQUIDITY_SHORT_FOR_UPCOMING_OBLIGATIONS").status == "NOT_EVALUATED") or (rule("LIQUIDITY_SHORT_FOR_UPCOMING_OBLIGATIONS").observed.value == $f.openingBalance.amount)),
  liquidity_threshold_is_due_rows: ((rule("LIQUIDITY_SHORT_FOR_UPCOMING_OBLIGATIONS").threshold.value == null) or (rule("LIQUIDITY_SHORT_FOR_UPCOMING_OBLIGATIONS") | .threshold.value == total([.evidence[] | select(.role == "PAYMENT_DUE" or .role == "MATURITY_DUE")]))),
  concentration_is_largest_composition_row: ((rule("INCOME_CONCENTRATION").status == "NOT_EVALUATED") or (rule("INCOME_CONCENTRATION").observed | .subject == $r.incomeComposition[0].category and .value == r6($r.incomeComposition[0].amount / $r.totals.income))),
  every_line_source_is_a_report_occurrence: ([.indicators[] | select(.indicatorId != "EMERGENCY_RESERVE_MONTHS" and .indicatorId != "FORECAST_MINIMUM_CASH_BALANCE") | .calculation.sources[] | . as $s | any($r.occurrences[]; .sourceEntryId == $s.sourceId and .occurrenceDate == $s.date and .amount == $s.amountVnd)] | all)
}
