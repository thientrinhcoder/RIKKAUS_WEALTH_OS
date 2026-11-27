#!/usr/bin/env bash
# Runs the cases of phase-01-test-scenarios.md against $BASE and prints one PASS/FAIL line per check.
# Usage: cases.sh main | usd | custom | restored   (see the run order in phase-03-manual-verification.md)
# Bodies are kept in $OUT/bodies for the evidence report.
set -uo pipefail
S="$(cd "$(dirname "$0")" && pwd)"
BASE=${BASE:-http://127.0.0.1:8083/api/v1}
OUT=${OUT:?set OUT to the directory setup.sh wrote}
source "$OUT/env.sh"
mkdir -p "$OUT/bodies"
B=$OUT/bodies
PASS=0; FAIL=0
PHASE=${1:-main}

token() { local e="EMAIL_$1"; curl -sf -X POST "$BASE/auth/dev-sign-in" -H 'Content-Type: application/json' \
  -d "{\"email\":\"${!e}\"}" | jq -r .accessToken; }
for u in B W S P Z I E U L; do printf -v "T_$u" '%s' "$(token $u)"; done
# fh ID TOKEN [QUERY] -> body in $B/ID.json, status in $B/ID.status
fh() { curl -s -o "$B/$1.json" -w '%{http_code}' "$BASE/financial-health${3:-}" -H "Authorization: Bearer $2" > "$B/$1.status"; }
ck() {
  if jq -e "$3" "$2" > /dev/null 2>&1; then PASS=$((PASS+1)); echo "$1 PASS";
  else FAIL=$((FAIL+1)); echo "$1 FAIL :: $(jq -c "${4:-.}" "$2" 2>/dev/null | cut -c1-900)"; fi
}
st() { if [[ "$(cat "$B/$2.status")" == "$3" ]]; then PASS=$((PASS+1)); echo "$1 PASS status $3"; else FAIL=$((FAIL+1)); echo "$1 FAIL status $(cat "$B/$2.status") != $3"; fi; }
same() { if cmp -s "$2" "$3"; then PASS=$((PASS+1)); echo "$1 PASS identical"; else FAIL=$((FAIL+1)); echo "$1 FAIL differs"; fi; }
# recon ID TOKEN MONTH: reconcile the body with the report and the forecast read right after it
recon() {
  curl -s -o "$B/$1-report.json" "$BASE/cash-flow-reports/monthly?month=$3" -H "Authorization: Bearer $2"
  curl -s -o "$B/$1-forecast.json" "$BASE/cash-flow-forecast" -H "Authorization: Bearer $2"
  jq --slurpfile rep "$B/$1-report.json" --slurpfile fc "$B/$1-forecast.json" -f "$S/recon.jq" "$B/$1.json" > "$B/$1-recon.json"
  ck "$1-recon" "$B/$1-recon.json" '[.[]] | all' 'with_entries(select(.value == false))'
}
# Compact views: per indicator [availability, value, band, bandNotDeterminedReason, unavailableReason, partialReason];
# per rule [status, observed.value, observed.subject, notEvaluatedReason].
IND='[.indicators[] | [.availability, .value, .band, .bandNotDeterminedReason, .unavailableReason, .partialReason]]'
RUL='[.rules[] | [.status, .observed.value, .observed.subject, .notEvaluatedReason]]'
DEFAULT_T='{reserveOkMinMonths: 6, minReserveMonths: 3, debtBurdenOkMax: 0.25, maxDebtPaymentBurden: 0.35, passiveIncomeHighMin: 0.5, passiveIncomeMediumMin: 0.2, essentialShareOkMax: 0.7, essentialShareWatchMax: 0.8, dueWindowDays: 30, maxIncomeSourceShare: 0.6}'
ESS7='["LIVING_COSTS","HOUSING","HEALTHCARE","INSURANCE","TAX_AND_FEES","DEBT_PRINCIPAL_PAYMENT","INTEREST_PAYMENT"]'
ind() { echo ".indicators[] | select(.indicatorId == \"$1\")"; }
rul() { echo ".rules[] | select(.ruleId == \"$1\")"; }
SRC='[.calculation.sources[] | [.sourceId, .role, .amountVnd]]'

echo "phase $PHASE $(date '+%Y-%m-%d %H:%M %Z')"

baseline_checks() { # baseline_checks FILE
  local f=$1
  ck H01-head "$f" ".assumptions == {asOfDate: \"2026-11-27\", timeZone: \"Asia/Ho_Chi_Minh\", month: \"2026-11\", monthFrom: \"2026-11-01\", monthTo: \"2026-11-30\",
      forecastHorizon: {from: \"2026-12-01\", to: \"2027-02-28\"}, obligationWindow: {from: \"2026-11-27\", to: \"2026-12-27\"},
      liquidAssetCategories: [\"CASH\",\"BANK_ACCOUNT\",\"TERM_DEPOSIT\"], cashAccountCategories: [\"CASH\",\"BANK_ACCOUNT\"],
      essentialCategories: $ESS7, debtPaymentCategories: [\"DEBT_PRINCIPAL_PAYMENT\",\"INTEREST_PAYMENT\"],
      exchangeRate: {usdToVndRate: 25100, effectiveDate: \"2026-09-12\"}} and .thresholds == $DEFAULT_T" '.assumptions, .thresholds'
  ck H02-indicators "$f" "$IND == [[\"COMPLETE\",30.0,\"OK\",null,null,null],[\"COMPLETE\",0.090278,\"OK\",null,null,null],[\"COMPLETE\",626600000,\"OK\",null,null,null],[\"COMPLETE\",0.119806,\"LOW\",null,null,null],[\"COMPLETE\",0.833696,\"ATTENTION\",null,null,null]]" "$IND"
  ck H02-calculations "$f" "[.indicators[].calculation | [.formula, .numeratorVnd, .denominatorVnd, .referenceVnd, .observedOn]] == [
      [\"LIQUID_ASSETS_OVER_MONTHLY_RECURRING_ESSENTIAL_EXPENSE\",1850000000,61600000,null,null],
      [\"DEBT_PAYMENTS_OVER_INCOME\",16250000,180000000,null,null],
      [\"LOWEST_PROJECTED_CASH_BALANCE\",null,null,61600000,\"2026-12-03\"],
      [\"ASSET_LINKED_INCOME_OVER_INCOME\",21565000,180000000,null,null],
      [\"ESSENTIAL_EXPENSE_OVER_EXPENSE\",76700000,92000000,null,null]]" '[.indicators[].calculation | del(.sources)]'
  ck H02-reserve-sources "$f" "$(ind EMERGENCY_RESERVE_MONTHS) | [.calculation.sources[] | select(.role == \"NUMERATOR\") | [.sourceId, .amount, .currency, .amountVnd]] as \$n
      | [.calculation.sources[] | select(.role == \"DENOMINATOR\") | [.sourceId, .cadence, .amountVnd, .monthlyEquivalentVnd]] as \$d
      | (\$n | sort) == ([[\"$B_CASH\",148000000,\"VND\",148000000],[\"$B_USD\",20000,\"USD\",502000000],[\"$B_TERM\",1200000000,\"VND\",1200000000]] | sort)
      and (\$d | sort) == ([[\"$B_LIVING\",\"MONTHLY\",23400000,23400000],[\"$B_HOUSING\",\"MONTHLY\",18750000,18750000],[\"$B_INSURANCE\",\"YEARLY\",5400000,450000],[\"$B_TAX\",\"QUARTERLY\",8250000,2750000],[\"$B_PRINCIPAL\",\"MONTHLY\",10000000,10000000],[\"$B_INTEREST\",\"MONTHLY\",6250000,6250000]] | sort)" "$(ind EMERGENCY_RESERVE_MONTHS) | $SRC"
  ck H02-debt-sources "$f" "$(ind DEBT_PAYMENT_BURDEN) | ([.calculation.sources[] | select(.role == \"NUMERATOR\") | .sourceId] | sort) == ([\"$B_PRINCIPAL\",\"$B_INTEREST\"] | sort)
      and ([.calculation.sources[] | select(.role == \"DENOMINATOR\")] | length) == 7 and all(.calculation.sources[]; .sourceId != \"$B_HOUSING\")" "$(ind DEBT_PAYMENT_BURDEN) | $SRC"
  ck H02-passive-sources "$f" "$(ind PASSIVE_INCOME_SHARE) | ([.calculation.sources[] | select(.role == \"SHARE_PART\") | .sourceId] | sort) == ([\"$B_RENT\",\"$B_DIVIDEND\",\"$B_INTEREST_IN\",\"$B_BOND\"] | sort)
      and ([.calculation.sources[] | select(.sourceId == \"$B_DIVIDEND\") | [.amount, .currency, .amountVnd]] == [[150,\"USD\",3765000]])" "$(ind PASSIVE_INCOME_SHARE) | $SRC"
  ck H02-essential-sources "$f" "$(ind ESSENTIAL_EXPENSE_SHARE) | ([.calculation.sources[] | select(.role == \"SHARE_REST\") | .sourceId] | sort) == ([\"$B_EDU\",\"$B_OTHER_EX\"] | sort) and (.calculation.sources | length) == 9" "$(ind ESSENTIAL_EXPENSE_SHARE) | $SRC"
  ck H02-minimum-sources "$f" "$(ind FORECAST_MINIMUM_CASH_BALANCE) | [.calculation.sources[] | [.sourceId, .role, .date, .amountVnd]] == [[\"$B_CASH\",\"OPENING_BALANCE\",\"2026-11-25\",148000000],[\"$B_USD\",\"OPENING_BALANCE\",\"2026-11-24\",502000000],[\"$B_LIVING\",\"PROJECTED_EXPENSE\",\"2026-12-03\",23400000]]
      or [.calculation.sources[] | [.sourceId, .role, .date, .amountVnd]] == [[\"$B_USD\",\"OPENING_BALANCE\",\"2026-11-24\",502000000],[\"$B_CASH\",\"OPENING_BALANCE\",\"2026-11-25\",148000000],[\"$B_LIVING\",\"PROJECTED_EXPENSE\",\"2026-12-03\",23400000]]" "$(ind FORECAST_MINIMUM_CASH_BALANCE) | $SRC"
  ck H03-rules "$f" "$RUL == [[\"OK\",88000000,null,null],[\"OK\",626600000,null,null],[\"OK\",30.0,null,null],[\"OK\",0.090278,null,null],[\"OK\",650000000,null,null],[\"ATTENTION\",0.733333,\"SALARY\",null]]" "$RUL"
  ck H03-thresholds "$f" "[.rules[] | [.dependsOn, .threshold.comparison, .threshold.value, .threshold.unit, .threshold.property]] == [
      [null,\"LESS_THAN\",0,\"VND\",null],[\"FORECAST_MINIMUM_CASH_BALANCE\",\"LESS_THAN\",0,\"VND\",null],
      [\"EMERGENCY_RESERVE_MONTHS\",\"LESS_THAN\",3.0,\"MONTHS\",\"min-reserve-months\"],[\"DEBT_PAYMENT_BURDEN\",\"GREATER_THAN\",0.35,\"FRACTION\",\"max-debt-payment-burden\"],
      [null,\"LESS_THAN\",50750000,\"VND\",\"due-window-days\"],[null,\"GREATER_THAN\",0.6,\"FRACTION\",\"max-income-source-share\"]]" '[.rules[] | [.dependsOn, .threshold]]'
  ck H03-liquidity-evidence "$f" "$(rul LIQUIDITY_SHORT_FOR_UPCOMING_OBLIGATIONS) | [.evidence[] | select(.sourceType == \"LIABILITY\") | [.sourceId, .role, .date, .amountVnd]] == [[\"$B_CARD\",\"MATURITY_DUE\",\"2026-11-30\",32000000],[\"$B_LOAN\",\"PAYMENT_DUE\",\"2026-12-20\",18750000]]
      and ([.evidence[] | select(.role == \"CASH_ACCOUNT\") | .amountVnd] | add) == 650000000" "$(rul LIQUIDITY_SHORT_FOR_UPCOMING_OBLIGATIONS) | [.evidence[] | [.sourceId, .role, .date, .amountVnd]]"
  ck H03-concentration-evidence "$f" "$(rul INCOME_CONCENTRATION) | [.evidence[] | [.sourceId, .role, .date, .amountVnd]] == [[\"$B_SALARY\",\"INCOME\",\"2026-11-05\",132000000]]" "$(rul INCOME_CONCENTRATION) | .evidence"
  ck H03-spending-evidence "$f" "$(rul SPENDING_EXCEEDS_INCOME) | (.evidence | length) == 16 and ([.evidence[] | select(.role == \"INCOME\") | .amountVnd] | add) == 180000000" "$(rul SPENDING_EXCEEDS_INCOME) | .evidence | length"
}

case "$PHASE" in
main)
  fh H01 "$T_B"; st H01-status H01 200
  baseline_checks "$B/H01.json"
  recon H01 "$T_B" 2026-11
  fh H05a "$T_B" '?month=2026-11'; same H05-explicit-month "$B/H01.json" "$B/H05a.json"
  fh H05b "$T_B"; same H05-second-read "$B/H01.json" "$B/H05b.json"
  fh H06 "$T_B" '?month=2026-10'; st H06-status H06 200
  ck H06-october "$B/H06.json" ".assumptions.month == \"2026-10\" and .assumptions.forecastHorizon == {from: \"2026-12-01\", to: \"2027-02-28\"}
      and $IND == [[\"COMPLETE\",31.4,\"OK\",null,null,null],[\"COMPLETE\",0.109575,\"OK\",null,null,null],[\"COMPLETE\",626600000,\"OK\",null,null,null],[\"COMPLETE\",0.109912,\"LOW\",null,null,null],[\"COMPLETE\",1.0,\"ATTENTION\",null,null,null]]
      and ($(ind EMERGENCY_RESERVE_MONTHS) | .calculation.denominatorVnd == 58850000 and all(.calculation.sources[]; .sourceId != \"$B_TAX\"))
      and $RUL == [[\"OK\",89900000,null,null],[\"OK\",626600000,null,null],[\"OK\",31.4,null,null],[\"OK\",0.109575,null,null],[\"OK\",650000000,null,null],[\"ATTENTION\",0.890088,\"SALARY\",null]]" "$IND, $RUL"
  recon H06 "$T_B" 2026-10
  fh H07 "$T_B" "?userId=$UID_S"; same H07-userId-ignored "$B/H01.json" "$B/H07.json"

  fh H08 "$T_W"
  ck H08-watch "$B/H08.json" "$IND == [[\"COMPLETE\",4.7,\"WATCH\",null,null,null],[\"COMPLETE\",0.285088,\"WATCH\",null,null,null],[\"COMPLETE\",40000000,\"WATCH\",null,null,null],[\"COMPLETE\",0.210526,\"MEDIUM\",null,null,null],[\"COMPLETE\",0.751773,\"WATCH\",null,null,null]]
      and $RUL == [[\"OK\",600000,null,null],[\"OK\",40000000,null,null],[\"OK\",4.7,null,null],[\"OK\",0.285088,null,null],[\"OK\",40000000,null,null],[\"ATTENTION\",0.789474,\"SALARY\",null]]
      and ($(ind FORECAST_MINIMUM_CASH_BALANCE) | .calculation.referenceVnd == 42400000 and .calculation.observedOn == \"2026-12-01\" and [.calculation.sources[].role] == [\"OPENING_BALANCE\"])
      and ($(rul LIQUIDITY_SHORT_FOR_UPCOMING_OBLIGATIONS) | .threshold.value == 16250000)" "$IND, $RUL"
  recon H08 "$T_W" 2026-11

  fh H09 "$T_S"
  ck H09-stress "$B/H09.json" "$IND == [[\"COMPLETE\",0.7,\"ATTENTION\",null,null,null],[\"COMPLETE\",1.097973,\"ATTENTION\",null,null,null],[\"COMPLETE\",-97050000,\"ATTENTION\",null,null,null],[\"COMPLETE\",1.0,\"HIGH\",null,null,null],[\"COMPLETE\",1.0,\"ATTENTION\",null,null,null]]
      and $RUL == [[\"ATTENTION\",-57250000,null,null],[\"ATTENTION\",-97050000,null,null],[\"ATTENTION\",0.7,null,null],[\"ATTENTION\",1.097973,null,null],[\"ATTENTION\",42000000,null,null],[\"ATTENTION\",1.0,\"RENTAL_INCOME\",null]]
      and ($(ind FORECAST_MINIMUM_CASH_BALANCE) | .calculation.observedOn == \"2027-02-22\") and ($(rul LIQUIDITY_SHORT_FOR_UPCOMING_OBLIGATIONS) | .threshold.value == 50750000)
      and all(.calculation.sources[]?; true) and all(.indicators[].calculation.sources[]; .sourceId != \"$S_SALARY\")" "$IND, $RUL"
  recon H09 "$T_S" 2026-11

  fh H10 "$T_P"
  ck H10-partial "$B/H10.json" ".assumptions.exchangeRate == null
      and $IND == [[\"PARTIAL\",21.9,null,\"PARTIAL_INPUTS\",null,\"MISSING_EXCHANGE_RATE\"],[\"PARTIAL\",0.092206,null,\"PARTIAL_INPUTS\",null,\"MISSING_EXCHANGE_RATE\"],[\"PARTIAL\",124600000,null,\"PARTIAL_INPUTS\",null,\"MISSING_EXCHANGE_RATE\"],[\"PARTIAL\",0.101002,null,\"PARTIAL_INPUTS\",null,\"MISSING_EXCHANGE_RATE\"],[\"COMPLETE\",0.833696,\"ATTENTION\",null,null,null]]
      and $RUL == [[\"NOT_EVALUATED\",null,null,\"MISSING_EXCHANGE_RATE\"],[\"NOT_EVALUATED\",null,null,\"INDICATOR_PARTIAL\"],[\"NOT_EVALUATED\",null,null,\"INDICATOR_PARTIAL\"],[\"NOT_EVALUATED\",null,null,\"INDICATOR_PARTIAL\"],[\"NOT_EVALUATED\",null,null,\"MISSING_EXCHANGE_RATE\"],[\"NOT_EVALUATED\",null,null,\"MISSING_EXCHANGE_RATE\"]]" "$IND, $RUL"
  ck H10-usd-null "$B/H10.json" "([.indicators[].calculation.sources[], .rules[].evidence[] | select(.currency == \"USD\") | .amountVnd] | (length > 0) and all(. == null))
      and ($(ind EMERGENCY_RESERVE_MONTHS) | .calculation.numeratorVnd == 1348000000)
      and ($(ind FORECAST_MINIMUM_CASH_BALANCE) | [.calculation.sources[] | select(.role == \"OPENING_BALANCE\") | [.sourceId, .amountVnd]] | sort == ([[\"$P_CASH\",148000000],[\"$P_USD\",null]] | sort))
      and ($(rul SPENDING_EXCEEDS_INCOME) | [.evidence[].sourceId] == [\"$P_DIVIDEND\"])" '[.indicators[].calculation.sources[] | select(.currency == "USD")]'
  recon H10 "$T_P" 2026-11

  fh H11 "$T_Z"
  ck H11-zero "$B/H11.json" "$IND == [[\"UNAVAILABLE\",null,null,\"INDICATOR_UNAVAILABLE\",\"NO_RECURRING_ESSENTIAL_EXPENSES\",null],[\"UNAVAILABLE\",null,null,\"INDICATOR_UNAVAILABLE\",\"NO_INCOME\",null],[\"COMPLETE\",50000000,null,\"NO_RECURRING_ESSENTIAL_EXPENSES\",null,null],[\"UNAVAILABLE\",null,null,\"INDICATOR_UNAVAILABLE\",\"NO_INCOME\",null],[\"COMPLETE\",0.604651,\"OK\",null,null,null]]
      and $RUL == [[\"ATTENTION\",-38700000,null,null],[\"OK\",50000000,null,null],[\"NOT_EVALUATED\",null,null,\"INDICATOR_UNAVAILABLE\"],[\"NOT_EVALUATED\",null,null,\"INDICATOR_UNAVAILABLE\"],[\"NOT_EVALUATED\",null,null,\"NO_LIABILITIES\"],[\"NOT_EVALUATED\",null,null,\"NO_INCOME\"]]
      and ($(ind EMERGENCY_RESERVE_MONTHS) | [.calculation.sources[] | [.sourceId, .role]] == [[\"$Z_CASH\",\"NUMERATOR\"]])
      and ($(ind FORECAST_MINIMUM_CASH_BALANCE) | .calculation.referenceVnd == null)" "$IND, $RUL"
  recon H11 "$T_Z" 2026-11

  fh H12 "$T_I"
  ck H12-income-only "$B/H12.json" "$IND == [[\"UNAVAILABLE\",null,null,\"INDICATOR_UNAVAILABLE\",\"NO_LIQUID_ASSETS\",null],[\"COMPLETE\",0.0,\"OK\",null,null,null],[\"UNAVAILABLE\",null,null,\"INDICATOR_UNAVAILABLE\",\"NO_CASH_ACCOUNTS\",null],[\"COMPLETE\",0.0,\"LOW\",null,null,null],[\"UNAVAILABLE\",null,null,\"INDICATOR_UNAVAILABLE\",\"NO_EXPENSES\",null]]
      and $RUL == [[\"OK\",30000000,null,null],[\"NOT_EVALUATED\",null,null,\"INDICATOR_UNAVAILABLE\"],[\"NOT_EVALUATED\",null,null,\"INDICATOR_UNAVAILABLE\"],[\"OK\",0.0,null,null],[\"NOT_EVALUATED\",null,null,\"NO_CASH_ACCOUNTS\"],[\"ATTENTION\",1.0,\"SALARY\",null]]" "$IND, $RUL"
  recon H12 "$T_I" 2026-11

  fh H13 "$T_E"; st H13-status H13 200
  ck H13-empty "$B/H13.json" "$IND == [[\"UNAVAILABLE\",null,null,\"INDICATOR_UNAVAILABLE\",\"NO_LIQUID_ASSETS\",null],[\"UNAVAILABLE\",null,null,\"INDICATOR_UNAVAILABLE\",\"NO_INCOME\",null],[\"UNAVAILABLE\",null,null,\"INDICATOR_UNAVAILABLE\",\"NO_CASH_ACCOUNTS\",null],[\"UNAVAILABLE\",null,null,\"INDICATOR_UNAVAILABLE\",\"NO_INCOME\",null],[\"UNAVAILABLE\",null,null,\"INDICATOR_UNAVAILABLE\",\"NO_EXPENSES\",null]]
      and $RUL == [[\"NOT_EVALUATED\",null,null,\"NO_CASH_FLOW_IN_MONTH\"],[\"NOT_EVALUATED\",null,null,\"INDICATOR_UNAVAILABLE\"],[\"NOT_EVALUATED\",null,null,\"INDICATOR_UNAVAILABLE\"],[\"NOT_EVALUATED\",null,null,\"INDICATOR_UNAVAILABLE\"],[\"NOT_EVALUATED\",null,null,\"NO_CASH_ACCOUNTS\"],[\"NOT_EVALUATED\",null,null,\"NO_INCOME\"]]
      and all(.indicators[].calculation; .numeratorVnd == null and .denominatorVnd == null and .sources == [])
      and all(.rules[]; .evidence == []) and .assumptions.exchangeRate == null" "$IND, $RUL"
  recon H13 "$T_E" 2026-11

  for q in 'month=2026-13' 'month=11%2F2026' 'month=2026-9' 'month=' 'month=2026-11&month=2026-12'; do
    id="H14-$(echo "$q" | tr -c 'A-Za-z0-9-' '_')"
    fh "$id" "$T_B" "?$q"; st "$id-status" "$id" 400
    ck "$id-body" "$B/$id.json" '.type == "urn:rikkaus:problem:validation-failed" and .errors == [{field: "month", message: "must be a calendar month in the form YYYY-MM"}] and (tostring | test("2026-13|11/2026|2026-9\"") | not)'
  done
  curl -s -o "$B/H15a.json" -w '%{http_code}' "$BASE/financial-health" > "$B/H15a.status"; st H15-no-token H15a 401
  ck H15-no-token-body "$B/H15a.json" '.type == "urn:rikkaus:problem:unauthorized"'
  curl -s -o "$B/H15b.json" -w '%{http_code}' "$BASE/financial-health" -H 'Authorization: Bearer not-a-token' > "$B/H15b.status"; st H15-bad-token H15b 401
  curl -s -o "$B/H16.json" -w '%{http_code}' -X POST "$BASE/financial-health" -H "Authorization: Bearer $T_B" > "$B/H16.status"; st H16-post H16 405
  ;;
usd)
  fh H21 "$T_U"; st H21-status H21 200
  ck H21-usd-essential "$B/H21.json" "($(ind EMERGENCY_RESERVE_MONTHS) | [.availability, .value, .band, .bandNotDeterminedReason, .partialReason, .calculation.numeratorVnd, .calculation.denominatorVnd]
        == [\"PARTIAL\", 4.3, null, \"PARTIAL_INPUTS\", \"MISSING_EXCHANGE_RATE\", 100000000, 23400000]
      and ([.calculation.sources[] | select(.role == \"DENOMINATOR\") | [.sourceId, .amount, .currency, .amountVnd, .monthlyEquivalentVnd]] | sort)
        == ([[\"$U_LIVING\", 23400000, \"VND\", 23400000, 23400000], [\"$U_INSURANCE\", 200, \"USD\", null, null]] | sort))
      and ($(rul RESERVE_BELOW_REFERENCE) | [.status, .observed, .notEvaluatedReason, .dependsOn] == [\"NOT_EVALUATED\", null, \"INDICATOR_PARTIAL\", \"EMERGENCY_RESERVE_MONTHS\"])
      and ($(ind FORECAST_MINIMUM_CASH_BALANCE) | [.availability, .value, .band, .partialReason, .calculation.referenceVnd] == [\"PARTIAL\", 29800000, null, \"MISSING_EXCHANGE_RATE\", null])
      and ($(ind ESSENTIAL_EXPENSE_SHARE) | [.availability, .value, .band] == [\"PARTIAL\", 1.0, null])
      and ($(rul FORECAST_CASH_NEGATIVE) | .notEvaluatedReason == \"INDICATOR_PARTIAL\")
      and ($(rul SPENDING_EXCEEDS_INCOME) | [.status, .notEvaluatedReason, [.evidence[].sourceId]] == [\"NOT_EVALUATED\", \"MISSING_EXCHANGE_RATE\", [\"$U_INSURANCE\"]])
      and all(.indicators[].calculation.sources[], .rules[].evidence[]; .currency != \"USD\" or .amountVnd == null)" "$IND, $RUL, ($(ind EMERGENCY_RESERVE_MONTHS) | $SRC)"
  recon H21 "$T_U" 2026-11

  fh H22 "$T_L"; st H22-status H22 200
  ck H22-usd-obligation "$B/H22.json" "($(rul LIQUIDITY_SHORT_FOR_UPCOMING_OBLIGATIONS) | [.status, .observed, .notEvaluatedReason, .threshold.value, .threshold.property]
        == [\"NOT_EVALUATED\", null, \"MISSING_EXCHANGE_RATE\", null, \"due-window-days\"]
      and [.evidence[] | [.sourceId, .role, .date, .amount, .currency, .amountVnd, .cadence]]
        == [[\"$L_CASH\", \"CASH_ACCOUNT\", \"2026-11-26\", 80000000, \"VND\", 80000000, null], [\"$L_LOAN\", \"PAYMENT_DUE\", \"2026-12-10\", 500, \"USD\", null, \"MONTHLY\"]])
      and ($(ind FORECAST_MINIMUM_CASH_BALANCE) | [.availability, .value, .partialReason] == [\"PARTIAL\", 80000000, \"MISSING_EXCHANGE_RATE\"])
      and ($(rul FORECAST_CASH_NEGATIVE) | .status == \"NOT_EVALUATED\")" "$IND, $RUL, ($(rul LIQUIDITY_SHORT_FOR_UPCOMING_OBLIGATIONS) | .evidence)"
  recon H22 "$T_L" 2026-11
  ;;
custom)
  fh C01 "$T_B"; fh C02 "$T_W"
  ck H17-thresholds "$B/C01.json" ".thresholds.minReserveMonths == 5 and .thresholds.maxIncomeSourceShare == 0.8 and .thresholds.reserveOkMinMonths == 6
      and .assumptions.essentialCategories == [\"LIVING_COSTS\",\"HOUSING\",\"EDUCATION\",\"HEALTHCARE\",\"INSURANCE\",\"TAX_AND_FEES\",\"DEBT_PRINCIPAL_PAYMENT\",\"INTEREST_PAYMENT\"]" '.thresholds, .assumptions.essentialCategories'
  ck H17-base "$B/C01.json" "($(ind ESSENTIAL_EXPENSE_SHARE) | [.value, .band, .calculation.numeratorVnd] == [0.972826, \"ATTENTION\", 89500000])
      and ($(rul INCOME_CONCENTRATION) | [.status, .observed.value, .threshold.value] == [\"OK\", 0.733333, 0.8])
      and ($(rul RESERVE_BELOW_REFERENCE) | [.status, .threshold.value] == [\"OK\", 5.0])" "$IND, $RUL"
  ck H17-watch "$B/C02.json" "($(ind EMERGENCY_RESERVE_MONTHS) | [.value, .band] == [4.7, \"ATTENTION\"])
      and ($(rul RESERVE_BELOW_REFERENCE) | [.status, .observed.value, .threshold.value] == [\"ATTENTION\", 4.7, 5.0])
      and ($(rul INCOME_CONCENTRATION) | .status == \"OK\")" "$IND, $RUL"
  recon C01 "$T_B" 2026-11
  ;;
restored)
  fh R01 "$T_B"; same H18-restored "$B/H01.json" "$B/R01.json"
  ;;
esac
echo "phase $PHASE: $PASS PASS, $FAIL FAIL"
