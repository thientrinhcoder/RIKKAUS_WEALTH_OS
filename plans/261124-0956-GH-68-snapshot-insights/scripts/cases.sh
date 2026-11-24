#!/usr/bin/env bash
# Runs the cases of phase-01-test-scenarios.md against $BASE and prints one PASS/FAIL line per check.
# Usage: cases.sh main | custom | restored | resolved   (see the run order in phase-03-manual-verification.md)
# Bodies are kept in $OUT/bodies for the evidence report.
set -uo pipefail
S="$(cd "$(dirname "$0")" && pwd)"
BASE=${BASE:-http://127.0.0.1:8068/api/v1}
OUT=${OUT:?set OUT to the directory setup.sh wrote}
source "$OUT/env.sh"
mkdir -p "$OUT/bodies"
B=$OUT/bodies
PASS=0; FAIL=0

token() { local e="EMAIL_$1"; curl -sf -X POST "$BASE/auth/dev-sign-in" -H 'Content-Type: application/json' \
  -d "{\"email\":\"${!e}\"}" | jq -r .accessToken; }
for u in A O E0 E1 C H Z N; do printf -v "T_$u" '%s' "$(token $u)"; done
api() { curl -s -H "Authorization: Bearer $1" -H 'Content-Type: application/json' -X "$2" "$BASE$3" ${4:+-d "$4"}; }
# ins ID TOKEN [QUERY] -> body in $B/ID.json, status in $B/ID.status
ins() { curl -s -o "$B/$1.json" -w '%{http_code}' "$BASE/wealth-snapshot/insights${3:-}" -H "Authorization: Bearer $2" > "$B/$1.status"; }
snap() { curl -s -o "$B/$1-snapshot.json" "$BASE/wealth-snapshot" -H "Authorization: Bearer $2"; }
ck() {
  if jq -e "$4" "$3" > /dev/null 2>&1; then PASS=$((PASS+1)); echo "$1 PASS $2";
  else FAIL=$((FAIL+1)); echo "$1 FAIL $2 :: $(jq -c . "$3" 2>/dev/null | cut -c1-700)"; fi
}
same() { if cmp -s "$3" "$4"; then PASS=$((PASS+1)); echo "$1 PASS $2"; else FAIL=$((FAIL+1)); echo "$1 FAIL $2"; fi; }
# recon ID: insights body ID against the snapshot read right after it
recon() {
  jq --slurpfile s "$B/$1-snapshot.json" '$s[0] as $snap | '"$(cat "$S/recon.jq")" "$B/$1.json" > "$B/$1-recon.json"
  ck "$1" "reconciles with GET /wealth-snapshot (all true)" "$B/$1-recon.json" '[.[]] | all'
}
# r RULE -> jq path to that insight
r() { echo ".insights[] | select(.ruleId == \"$1\")"; }
# ev: compact evidence rows [sourceId, dateKind, date, ageDays, daysUntilDue, overdue, amount, currency, amountVnd]
EV='[.evidence[] | [.sourceId, .dateKind, .date, .ageDays, .daysUntilDue, .overdue, .amount, .currency, .amountVnd]]'
DEFAULT='{maxCategoryShare: 0.5, maxDebtToAssetRatio: 0.4, maxLowLiquidityShare: 0.6, dueWindowDays: 30, maxValuationAgeDays: 30, maxExchangeRateAgeDays: 5}'

echo "phase ${1:-main} $(date '+%Y-%m-%d %H:%M %Z') D=$D"

fixture_a() { # $1 case id prefix for the body to check
  local f="$B/$1.json"
  ck I02 "concentration ATTENTION 0.624896 REAL_ESTATE" "$f" "$(r CATEGORY_CONCENTRATION) |
    .status == \"ATTENTION\" and .observed == {value: 0.624896, unit: \"FRACTION\", subject: \"REAL_ESTATE\", numeratorVnd: 7500000000, denominatorVnd: 12002000000, date: null}
    and .threshold == {comparison: \"GREATER_THAN\", value: 0.5, unit: \"FRACTION\"} and .unavailableReason == null
    and $EV == [[\"$A4\", \"VALUATION_DATE\", \"$DM48\", 48, null, null, 7500000000, \"VND\", 7500000000]]"
  ck I03 "debt OK 0.127645, two liabilities" "$f" "$(r DEBT_TO_ASSET_RATIO) |
    .status == \"OK\" and .observed.value == 0.127645 and .observed.numeratorVnd == 1532000000 and .observed.denominatorVnd == 12002000000
    and [.evidence[] | [.sourceId, .dateKind, .amount]] == [[\"$AL1\", \"UPDATED_ON\", 1500000000], [\"$AL2\", \"UPDATED_ON\", 32000000]]"
  ck I04 "low liquidity ATTENTION 0.624896" "$f" "$(r LOW_LIQUIDITY_SHARE) |
    .status == \"ATTENTION\" and .observed.value == 0.624896 and .observed.numeratorVnd == 7500000000
    and .threshold.value == 0.6 and [.evidence[].sourceId] == [\"$A4\"]"
  ck I05 "due ATTENTION 12 days, mortgage payment only" "$f" "$(r UPCOMING_LIABILITY_DUE) |
    .status == \"ATTENTION\" and .observed == {value: 12, unit: \"DAYS\", subject: null, numeratorVnd: null, denominatorVnd: null, date: \"$DP12\"}
    and .threshold == {comparison: \"AT_MOST\", value: 30, unit: \"DAYS\"}
    and $EV == [[\"$AL1\", \"NEXT_PAYMENT_DATE\", \"$DP12\", null, 12, false, 18750000, \"VND\", 18750000]]"
  ck I06 "valuation ATTENTION 48 days, apartment" "$f" "$(r STALE_VALUATION) |
    .status == \"ATTENTION\" and .observed.value == 48 and .observed.date == \"$DM48\" and .threshold.value == 30
    and [.evidence[] | [.sourceId, .ageDays]] == [[\"$A4\", 48]]"
  ck I07 "rate ATTENTION, age 12/09/2026 -> D, rate row + USD deposit" "$f" "$(r STALE_EXCHANGE_RATE) |
    .status == \"ATTENTION\" and .observed.date == \"2026-09-12\" and .observed.value == ((\"$D\" + \"T00:00:00Z\" | fromdateiso8601) - (\"2026-09-12T00:00:00Z\" | fromdateiso8601)) / 86400
    and .threshold.value == 5
    and [.evidence[] | [.sourceType, .sourceId, .dateKind, .date, .usdToVndRate, .amount, .currency, .amountVnd]]
      == [[\"EXCHANGE_RATE\", null, \"RATE_EFFECTIVE_DATE\", \"2026-09-12\", 25100, null, null, null],
          [\"ASSET\", \"$A5\", \"VALUATION_DATE\", \"$DM5\", null, 20000, \"USD\", 502000000]]"
}

case "${1:-main}" in
main)
  ins I01 "$T_A"; snap I01 "$T_A"
  ck I01 "status 200" "$B/I01.status" '. == 200'
  ck I01 "header, default thresholds, six rules in order" "$B/I01.json" "
    .asOfDate == \"$D\" and .timeZone == \"Asia/Ho_Chi_Minh\" and .complete == true and .thresholds == $DEFAULT
    and [.insights[].ruleId] == [\"CATEGORY_CONCENTRATION\",\"DEBT_TO_ASSET_RATIO\",\"LOW_LIQUIDITY_SHARE\",\"UPCOMING_LIABILITY_DUE\",\"STALE_VALUATION\",\"STALE_EXCHANGE_RATE\"]"
  fixture_a I01
  ck I05 "credit card D+40 is not in the window" "$B/I01.json" "[$(r UPCOMING_LIABILITY_DUE) | .evidence[].sourceId] | index(\"$AL2\") == null"
  cp "$B/I01.json" "$B/I08.json"; cp "$B/I01-snapshot.json" "$B/I08-snapshot.json"; recon I08
  ins I09 "$T_A"; same I09 "second read byte-identical" "$B/I01.json" "$B/I09.json"

  ins I10 "$T_O"; snap I10 "$T_O"
  ck I10 "all six OK with their figures" "$B/I10.json" "
    [.insights[].status] == [\"OK\",\"OK\",\"OK\",\"OK\",\"OK\",\"OK\"]
    and [.insights[].observed.value] == [0.38059, 0.126863, 0.38059, 45, 20, 2]
    and ($(r CATEGORY_CONCENTRATION) | .observed.subject == \"REAL_ESTATE\" and .observed.denominatorVnd == 3153000000)
    and ($(r UPCOMING_LIABILITY_DUE) | $EV == [[\"$OL1\", \"NEXT_PAYMENT_DATE\", \"$DP45\", null, 45, false, 12500000, \"VND\", 12500000]])
    and ($(r STALE_VALUATION) | [.evidence[] | [.sourceId, .ageDays]] == [[\"$O3\", 20]])
    and ($(r STALE_EXCHANGE_RATE) | [.evidence[] | [.sourceType, .sourceId, .usdToVndRate, .amount, .amountVnd]]
         == [[\"EXCHANGE_RATE\", null, 25300, null, null], [\"ASSET\", \"$O4\", null, 10000, 253000000]])"
  recon I10

  ins I11 "$T_E0"; snap I11 "$T_E0"
  ck I11 "at the threshold: all six OK" "$B/I11.json" "
    [.insights[].status] == [\"OK\",\"OK\",\"OK\",\"OK\",\"OK\",\"OK\"]
    and [.insights[].observed.value] == [0.5, 0.4, 0.6, 31, 30, 5]
    and ($(r CATEGORY_CONCENTRATION) | .observed.subject == \"BOND\" and [.evidence[].sourceId] == [\"$E02\"])
    and ($(r STALE_VALUATION) | [.evidence[] | [.ageDays]] == [[30]])"
  recon I11

  ins I12 "$T_E1"; snap I12 "$T_E1"
  ck I12 "one unit past: all six ATTENTION" "$B/I12.json" "
    [.insights[].status] == [\"ATTENTION\",\"ATTENTION\",\"ATTENTION\",\"ATTENTION\",\"ATTENTION\",\"ATTENTION\"]
    and [.insights[].observed.value] == [0.501, 0.401, 0.601, -5, 31, 6]
    and ($(r UPCOMING_LIABILITY_DUE) | $EV == [[\"$E1L1\", \"NEXT_PAYMENT_DATE\", \"$DM5\", null, -5, true, null, \"VND\", null],
                                               [\"$E1L2\", \"NEXT_PAYMENT_DATE\", \"$DP30\", null, 30, false, 3000000, \"VND\", 3000000]])
    and ($(r STALE_VALUATION) | [.evidence[] | [.sourceId, .ageDays]] == [[\"$E12\", 31]])
    and ($(r LOW_LIQUIDITY_SHARE) | [.evidence[].sourceId] == [\"$E12\", \"$E13\"])"
  recon I12

  ins I13 "$T_C"; snap I13 "$T_C"
  ck I13 "missing rate: three shares and the rate UNAVAILABLE, due and valuation judged" "$B/I13.json" "
    .complete == false and [.insights[].status] == [\"UNAVAILABLE\",\"UNAVAILABLE\",\"UNAVAILABLE\",\"ATTENTION\",\"OK\",\"UNAVAILABLE\"]
    and [.insights[].unavailableReason] == [\"MISSING_EXCHANGE_RATE\",\"MISSING_EXCHANGE_RATE\",\"MISSING_EXCHANGE_RATE\",null,null,\"MISSING_EXCHANGE_RATE\"]
    and ([.insights[] | select(.status == \"UNAVAILABLE\") | .observed] | all(. == null))
    and ($(r CATEGORY_CONCENTRATION) | $EV == [[\"$C2\", \"VALUATION_DATE\", \"$DM3\", 3, null, null, 20000, \"USD\", null]])
    and ($(r DEBT_TO_ASSET_RATIO) | [.evidence[] | [.sourceId, .amount, .currency, .amountVnd]] == [[\"$C2\", 20000, \"USD\", null], [\"$CL1\", 5000, \"USD\", null]])
    and ($(r LOW_LIQUIDITY_SHARE) | [.evidence[].sourceId] == [\"$C2\"])
    and ($(r UPCOMING_LIABILITY_DUE) | .observed.value == 20 and $EV == [[\"$CL1\", \"MATURITY_DATE\", \"$DP20\", null, 20, false, 5000, \"USD\", null]])
    and ($(r STALE_VALUATION) | .observed.value == 3)
    and ($(r STALE_EXCHANGE_RATE) | [.evidence[].sourceId] == [\"$C2\", \"$CL1\"])"
  recon I13

  api "$T_C" PUT /settings/exchange-rate "{\"usdToVndRate\":\"25100\",\"effectiveDate\":\"$D\"}" > /dev/null
  ins I14 "$T_C"; snap I14 "$T_C"
  ck I14 "rate declared: every rule judged" "$B/I14.json" "
    .complete == true and [.insights[].status] == [\"ATTENTION\",\"OK\",\"OK\",\"ATTENTION\",\"OK\",\"OK\"]
    and [.insights[].observed.value] == [0.772308, 0.193077, 0, 20, 3, 0]
    and ($(r CATEGORY_CONCENTRATION) | .observed.subject == \"BANK_ACCOUNT\" and .observed.numeratorVnd == 502000000 and .observed.denominatorVnd == 650000000)
    and ($(r LOW_LIQUIDITY_SHARE) | .observed.numeratorVnd == 0 and .evidence == [])
    and ($(r UPCOMING_LIABILITY_DUE) | .evidence[0].amountVnd == 125500000)"
  recon I14

  ins I15 "$T_H"; snap I15 "$T_H"
  ck I15 "VND only, no liability, no rate" "$B/I15.json" "
    [.insights[].status] == [\"ATTENTION\",\"OK\",\"OK\",\"UNAVAILABLE\",\"OK\",\"UNAVAILABLE\"]
    and [.insights[].unavailableReason] == [null,null,null,\"NO_LIABILITIES\",null,\"MISSING_EXCHANGE_RATE\"]
    and [.insights[].observed.value] == [0.533333, 0, 0, null, 2, null]
    and ($(r CATEGORY_CONCENTRATION) | .observed.subject == \"TERM_DEPOSIT\")
    and ($(r DEBT_TO_ASSET_RATIO) | .observed.numeratorVnd == 0 and .observed.denominatorVnd == 1500000000 and .evidence == [])
    and ($(r STALE_EXCHANGE_RATE) | .evidence == [])"
  recon I15

  ins I16 "$T_Z"; snap I16 "$T_Z"
  ck I16 "total assets of zero, no due date" "$B/I16.json" "
    [.insights[].status] == [\"UNAVAILABLE\",\"UNAVAILABLE\",\"UNAVAILABLE\",\"UNAVAILABLE\",\"OK\",\"UNAVAILABLE\"]
    and [.insights[].unavailableReason] == [\"ZERO_TOTAL_ASSETS\",\"ZERO_TOTAL_ASSETS\",\"ZERO_TOTAL_ASSETS\",\"NO_DUE_DATES\",null,\"MISSING_EXCHANGE_RATE\"]
    and ([.insights[0:3][] | [.evidence[] | [.sourceId, .amount]]] | all(. == [[\"$Z1\", 0]]))
    and ($(r UPCOMING_LIABILITY_DUE) | .evidence == []) and ($(r STALE_VALUATION) | .observed.value == 1)"
  recon I16

  ins I17 "$T_N"; snap I17 "$T_N"
  ck I17 "status 200" "$B/I17.status" '. == 200'
  ck I17 "no record: six UNAVAILABLE with reasons, no evidence" "$B/I17.json" "
    .complete == true and .thresholds == $DEFAULT and [.insights[].status] == [\"UNAVAILABLE\",\"UNAVAILABLE\",\"UNAVAILABLE\",\"UNAVAILABLE\",\"UNAVAILABLE\",\"UNAVAILABLE\"]
    and [.insights[].unavailableReason] == [\"NO_ASSETS\",\"NO_ASSETS\",\"NO_ASSETS\",\"NO_LIABILITIES\",\"NO_ASSETS\",\"MISSING_EXCHANGE_RATE\"]
    and ([.insights[] | .observed == null and .evidence == [] and .threshold != null] | all)"
  recon I17

  ck I18 "A's evidence has none of O's ids" "$B/I01.json" "
    [.insights[].evidence[].sourceId | select(. != null)] as \$a | [\"$O1\",\"$O2\",\"$O3\",\"$O4\",\"$OL1\"] | all(. as \$o | \$a | index(\$o) == null)"
  ins I18 "$T_A" "?userId=$U_O"; same I18 "?userId=<O> ignored: body equals I01" "$B/I01.json" "$B/I18.json"

  curl -s -o "$B/I19.json" -w '%{http_code}' "$BASE/wealth-snapshot/insights" > "$B/I19.status"
  ck I19 "no token -> 401" "$B/I19.status" '. == 401'
  ck I19 "Problem Details unauthorized" "$B/I19.json" '.status == 401 and (.type | endswith("unauthorized"))'
  ins I20 "not-a-real-token"; ck I20 "bad token -> 401" "$B/I20.status" '. == 401'
  curl -s -o "$B/I21.json" -w '%{http_code}' -X POST "$BASE/wealth-snapshot/insights" -H "Authorization: Bearer $T_A" > "$B/I21.status"
  ck I21 "POST -> 405" "$B/I21.status" '. == 405'
  curl -s -o "$B/I22.json" -w '%{http_code}' "$BASE/wealth-snapshot/insights" -H "Authorization: Bearer $T_A" -H 'Accept: application/xml' > "$B/I22.status"
  ck I22 "Accept xml -> 406" "$B/I22.status" '. == 406'
  ;;
custom) # API started with 0.7, 0.1, 0.65, 7, 60, 90
  ins I23 "$T_A"; snap I23 "$T_A"
  ck I23 "thresholds echo the new configuration" "$B/I23.json" '.thresholds == {maxCategoryShare: 0.7, maxDebtToAssetRatio: 0.1, maxLowLiquidityShare: 0.65, dueWindowDays: 7, maxValuationAgeDays: 60, maxExchangeRateAgeDays: 90}'
  ck I23 "every status flips, observations unchanged" "$B/I23.json" "
    [.insights[].status] == [\"OK\",\"ATTENTION\",\"OK\",\"OK\",\"OK\",\"OK\"]
    and [.insights[].observed.value] == [0.624896, 0.127645, 0.624896, 12, 48, 73]
    and [.insights[].threshold.value] == [0.7, 0.1, 0.65, 7, 60, 90]
    and ($(r UPCOMING_LIABILITY_DUE) | [.evidence[] | [.sourceId, .daysUntilDue]] == [[\"$AL1\", 12]])
    and ($(r STALE_VALUATION) | [.evidence[].sourceId] == [\"$A4\"])"
  ck I23 "GET /wealth-snapshot keeps its 30-day window" "$B/I23-snapshot.json" '.upcomingLiabilityDues.windowDays == 30 and (.upcomingLiabilityDues.items | length) == 1'
  recon I23
  ;;
restored) # API started again with application.yml as committed
  ins I24 "$T_A"
  same I24 "default configuration again: body equals I01" "$B/I01.json" "$B/I24.json"
  ;;
resolved)
  api "$T_A" POST "/assets/$A4/valuations" "{\"value\":\"7500000000\",\"currency\":\"VND\",\"effectiveDate\":\"$D\"}" > "$B/I28-valuation.json"
  ins I28a "$T_A"
  ck I28 "new valuation: STALE_VALUATION OK, oldest is now the term deposit (10 days)" "$B/I28a.json" "
    ($(r STALE_VALUATION) | .status == \"OK\" and .observed.value == 10 and [.evidence[] | [.sourceId, .ageDays]] == [[\"$A2\", 10]])
    and [.insights[0:3][].observed.value] == [0.624896, 0.127645, 0.624896]"
  api "$T_A" PUT /settings/exchange-rate "{\"usdToVndRate\":\"25100\",\"effectiveDate\":\"$D\"}" > /dev/null
  ins I28b "$T_A"; snap I28b "$T_A"
  ck I28 "rate declared today: STALE_EXCHANGE_RATE OK 0, shares unchanged" "$B/I28b.json" "
    ($(r STALE_EXCHANGE_RATE) | .status == \"OK\" and .observed.value == 0 and .observed.date == \"$D\")
    and [.insights[].status] == [\"ATTENTION\",\"OK\",\"ATTENTION\",\"ATTENTION\",\"OK\",\"OK\"]
    and [.insights[0:3][].observed.value] == [0.624896, 0.127645, 0.624896]"
  recon I28b
  ;;
esac
echo "TOTAL pass=$PASS fail=$FAIL"
