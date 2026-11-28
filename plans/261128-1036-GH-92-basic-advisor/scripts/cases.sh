#!/usr/bin/env bash
# Runs the cases of phase-01-test-scenarios.md against $BASE and prints one PASS/FAIL line per check.
# Bodies are kept in $OUT/bodies for the evidence report. Run setup.sh first, on the same day.
set -uo pipefail
S="$(cd "$(dirname "$0")" && pwd)"
BASE=${BASE:-http://127.0.0.1:8092/api/v1}
OUT=${OUT:?set OUT to the directory setup.sh wrote}
source "$OUT/env.sh"
mkdir -p "$OUT/bodies"
B=$OUT/bodies
PASS=0; FAIL=0

token() { local e="EMAIL_$1"; curl -sf -X POST "$BASE/auth/dev-sign-in" -H 'Content-Type: application/json' \
  -d "{\"email\":\"${!e}\"}" | jq -r .accessToken; }
for u in B S Z U E; do printf -v "T_$u" '%s' "$(token $u)"; done
# adv ID TOKEN [QUERY] -> body in $B/ID.json, status in $B/ID.status
adv() { curl -s -o "$B/$1.json" -w '%{http_code}' "$BASE/advisor-report${3:-}" -H "Authorization: Bearer $2" > "$B/$1.status"; }
XA=()
ck() {
  if jq -e ${XA[@]+"${XA[@]}"} "$3" "$2" > /dev/null 2>&1; then PASS=$((PASS+1)); echo "$1 PASS";
  else FAIL=$((FAIL+1)); echo "$1 FAIL :: $(jq -c "${4:-.}" "$2" 2>/dev/null | cut -c1-900)"; fi
}
st() { if [[ "$(cat "$B/$2.status")" == "$3" ]]; then PASS=$((PASS+1)); echo "$1 PASS status $3"; else FAIL=$((FAIL+1)); echo "$1 FAIL status $(cat "$B/$2.status") != $3"; fi; }
same() { if cmp -s "$2" "$3"; then PASS=$((PASS+1)); echo "$1 PASS identical"; else FAIL=$((FAIL+1)); echo "$1 FAIL differs"; fi; }
# recon ID TOKEN MONTH: reconcile the body with the three upstream routes read right after it
recon() {
  curl -s -o "$B/$1-insights.json" "$BASE/wealth-snapshot/insights" -H "Authorization: Bearer $2"
  curl -s -o "$B/$1-health.json" "$BASE/financial-health?month=$3" -H "Authorization: Bearer $2"
  curl -s -o "$B/$1-goals.json" "$BASE/goal-projections" -H "Authorization: Bearer $2"
  jq --slurpfile snap "$B/$1-insights.json" --slurpfile fh "$B/$1-health.json" --slurpfile gp "$B/$1-goals.json" \
    -f "$S/recon.jq" "$B/$1.json" > "$B/$1-recon.json"
  ck "$1-recon" "$B/$1-recon.json" '[.[]] | all' 'with_entries(select(.value == false))'
}
g() { echo "\"GOAL_PROJECTION:GOAL_CONTRIBUTION_COVERAGE:$1\""; }
IDS='[.unevaluated.findings[], .positive[], .attention[]] | map(.findingId)'
ATT='[.attention[].findingId]'
SUG='[.suggestions[] | [.rank, .priority, .tier, .suggestionId, .dueDate, .relativeDeviation]]'

echo "run $(date '+%Y-%m-%d %H:%M %Z')"

# --- B, the baseline -------------------------------------------------------------------------------------
adv A01 "$T_B"; st A01-status A01 200
ck A01-head "$B/A01.json" '.asOfDate == "2026-11-28" and .timeZone == "Asia/Ho_Chi_Minh" and .month == "2026-11" and .priorityMarkerCount == 3
    and .disclaimer == "This report is informational, based on the data you entered and the system'"'"'s reference thresholds; it is not financial advice and does not guarantee any outcome."
    and .sources == [{source: "WEALTH_SNAPSHOT", route: "/api/v1/wealth-snapshot/insights", findingCount: 6}, {source: "FINANCIAL_HEALTH", route: "/api/v1/financial-health?month=2026-11", findingCount: 6}, {source: "GOAL_PROJECTION", route: "/api/v1/goal-projections", findingCount: 6}]' '{asOfDate, month, disclaimer, sources}'
ck A02-unevaluated "$B/A01.json" "[.unevaluated.findings[] | [.findingId, .reason]] == [[$(g "$B_G5"), \"TARGET_DATE_PASSED\"], [$(g "$B_G4"), \"NO_CONTRIBUTION_MONTH\"], [$(g "$B_G6"), \"PLANNED_MONTHLY_CONTRIBUTION_MISSING\"]] and .unevaluated.count == 3" '.unevaluated'
ck A02-positive "$B/A01.json" "[.positive[].findingId] == [\"WEALTH_SNAPSHOT:DEBT_TO_ASSET_RATIO\", \"FINANCIAL_HEALTH:SPENDING_EXCEEDS_INCOME\", \"FINANCIAL_HEALTH:FORECAST_CASH_NEGATIVE\", \"FINANCIAL_HEALTH:RESERVE_BELOW_REFERENCE\", \"FINANCIAL_HEALTH:HIGH_DEBT_PAYMENT_BURDEN\", \"FINANCIAL_HEALTH:LIQUIDITY_SHORT_FOR_UPCOMING_OBLIGATIONS\", $(g "$B_G1")]" '[.positive[].findingId]'
ck A02-attention "$B/A01.json" "$ATT == [\"WEALTH_SNAPSHOT:CATEGORY_CONCENTRATION\", \"WEALTH_SNAPSHOT:LOW_LIQUIDITY_SHARE\", \"WEALTH_SNAPSHOT:UPCOMING_LIABILITY_DUE\", \"WEALTH_SNAPSHOT:STALE_VALUATION\", \"WEALTH_SNAPSHOT:STALE_EXCHANGE_RATE\", \"FINANCIAL_HEALTH:INCOME_CONCENTRATION\", $(g "$B_G2"), $(g "$B_G3")]" "$ATT"
recon A01 "$T_B" 2026-11
ck A04-order "$B/A01.json" "$SUG == [[1, true, 1, \"WEALTH_SNAPSHOT:UPCOMING_LIABILITY_DUE\", \"2026-11-30\", 0.933333],
    [2, true, 3, \"WEALTH_SNAPSHOT:CATEGORY_CONCENTRATION\", null, 0.249792], [3, true, 3, \"FINANCIAL_HEALTH:INCOME_CONCENTRATION\", null, 0.222222],
    [4, false, 3, \"WEALTH_SNAPSHOT:LOW_LIQUIDITY_SHARE\", null, 0.041493], [5, false, 4, $(g "$B_G2"), \"2029-11-30\", 0.1051],
    [6, false, 4, $(g "$B_G3"), \"2034-08-31\", 0.331], [7, false, 5, \"WEALTH_SNAPSHOT:STALE_EXCHANGE_RATE\", null, 14.4],
    [8, false, 5, \"WEALTH_SNAPSHOT:STALE_VALUATION\", null, 0.6]]" "$SUG"
ck A04-goal-figures "$B/A01.json" "[.attention[] | select(.source == \"GOAL_PROJECTION\") | [.observed.value, .observed.numeratorVnd, .observed.denominatorVnd, .threshold.value]] == [[89.49, 21000000, 23466000, 100], [66.9, 5000000, 7473000, 100]]
    and [.positive[] | select(.source == \"GOAL_PROJECTION\") | [.observed.value, .observed.denominatorVnd, .relativeDeviation]] == [[110.4, 9239000, -0.104]]" '[.attention[], .positive[] | select(.source == "GOAL_PROJECTION") | .observed]'
ck A05-same-record-other-family "$B/A01.json" "[.suggestions[] | select(any(.affectedRecords[]; .sourceId == \"$B_FLAT\")) | [.findingIds, .actionFamily]] == [[[\"WEALTH_SNAPSHOT:CATEGORY_CONCENTRATION\"], \"REVIEW_ASSET_CONCENTRATION\"], [[\"WEALTH_SNAPSHOT:LOW_LIQUIDITY_SHARE\"], \"RECONCILE_CASH_WITH_RESERVE\"], [[\"WEALTH_SNAPSHOT:STALE_VALUATION\"], \"REFRESH_RECORD\"]]
    and (.attention[] | select(.ruleId == \"CATEGORY_CONCENTRATION\") | .affectedRecords == [{sourceType: \"ASSET\", sourceId: \"$B_FLAT\", name: \"Căn hộ Thảo Điền\", route: \"/api/v1/assets/$B_FLAT\"}])" '[.suggestions[] | [.findingIds, .actionFamily, [.affectedRecords[].name]]]'
ck A06-notes "$B/A01.json" "[.attention[] | [.findingId, .noteId]] | map({(.[0]): .[1]}) | add | .[\"WEALTH_SNAPSHOT:STALE_EXCHANGE_RATE\"] == \"USER_DECLARED_EXCHANGE_RATE\" and .[$(g "$B_G2")] == \"USER_ASSUMED_RETURN\" and .[$(g "$B_G3")] == \"USER_ASSUMED_RETURN\" and .[\"WEALTH_SNAPSHOT:CATEGORY_CONCENTRATION\"] == \"MANUAL_ENTRY_NOT_RECONCILED\"" '[.attention[] | [.findingId, .noteId]]'
ck A06-action-names "$B/A01.json" '[.suggestions[].action] == [
    "You may compare your cash and bank balances with the upcoming payments entered for Thẻ tín dụng, then review the result.",
    "You may review the share of the largest asset category, including Căn hộ Thảo Điền, alongside your expected liquidity needs, then review the result.",
    "You may review how the income of the period is spread across its sources, then review the result.",
    "You may compare your cash reserve with the assets that are slower to turn into cash, such as Căn hộ Thảo Điền, then review the result.",
    "You may consider raising the monthly contribution, moving the target date, or adjusting the target amount of Mua nhà phố Thủ Đức, then review the result.",
    "You may consider raising the monthly contribution, moving the target date, or adjusting the target amount of Học phí đại học cho con, then review the result.",
    "You may update the declared USD/VND exchange rate so the report reads a current value, then review the result.",
    "You may update Căn hộ Thảo Điền so the report reads a current value, then review the result."]' '[.suggestions[].action]'
adv A07 "$T_B" '?month=2026-11'; same A07-explicit-month "$B/A01.json" "$B/A07.json"
adv A08 "$T_B"; same A08-second-read "$B/A01.json" "$B/A08.json"
adv A09 "$T_B" "?userId=$UID_S"; same A09-userId-ignored "$B/A01.json" "$B/A09.json"
adv A10 "$T_B" '?month=2026-10'; st A10-status A10 200
XA=(--slurpfile a "$B/A01.json")
ck A10-october "$B/A10.json" ".month == \"2026-10\" and .sources[1].route == \"/api/v1/financial-health?month=2026-10\"
    and ([.unevaluated.findings[], .positive[], .attention[] | select(.source != \"FINANCIAL_HEALTH\")] == ([\$a[0] | .unevaluated.findings[], .positive[], .attention[] | select(.source != \"FINANCIAL_HEALTH\")]))
    and (.attention[] | select(.ruleId == \"INCOME_CONCENTRATION\") | .observed.value != 0.733333)" '{month, health: [.attention[], .positive[] | select(.source == "FINANCIAL_HEALTH") | [.ruleId, .observed.value]]}'
XA=()
recon A10 "$T_B" 2026-10

# --- S, stress ---------------------------------------------------------------------------------------------
adv A11 "$T_S"; st A11-status A11 200
ck A11-groups "$B/A11.json" "$ATT == [\"WEALTH_SNAPSHOT:CATEGORY_CONCENTRATION\", \"WEALTH_SNAPSHOT:LOW_LIQUIDITY_SHARE\", \"WEALTH_SNAPSHOT:UPCOMING_LIABILITY_DUE\", \"WEALTH_SNAPSHOT:STALE_VALUATION\", \"WEALTH_SNAPSHOT:STALE_EXCHANGE_RATE\",
    \"FINANCIAL_HEALTH:SPENDING_EXCEEDS_INCOME\", \"FINANCIAL_HEALTH:FORECAST_CASH_NEGATIVE\", \"FINANCIAL_HEALTH:RESERVE_BELOW_REFERENCE\", \"FINANCIAL_HEALTH:HIGH_DEBT_PAYMENT_BURDEN\", \"FINANCIAL_HEALTH:LIQUIDITY_SHORT_FOR_UPCOMING_OBLIGATIONS\", \"FINANCIAL_HEALTH:INCOME_CONCENTRATION\"]
    and [.positive[].findingId] == [\"WEALTH_SNAPSHOT:DEBT_TO_ASSET_RATIO\"] and .unevaluated.count == 0" "{att: $ATT, pos: [.positive[].findingId], unev: .unevaluated.count}"
recon A11 "$T_S" 2026-11
ck A12-merge "$B/A11.json" ".suggestions[0] | .findingIds == [\"WEALTH_SNAPSHOT:UPCOMING_LIABILITY_DUE\", \"FINANCIAL_HEALTH:LIQUIDITY_SHORT_FOR_UPCOMING_OBLIGATIONS\", \"FINANCIAL_HEALTH:FORECAST_CASH_NEGATIVE\"]
    and .tier == 1 and .dueDate == \"2026-11-30\" and .actionFamily == \"RECONCILE_CASH_WITH_OBLIGATIONS\"
    and ([.affectedRecords[] | .sourceId] | index(\"$S_LOAN\") != null and index(\"$S_CARD\") != null and index(\"$S_CASH\") != null and index(\"$S_BANK\") != null)
    and (.noteIds | sort) == [\"FORECAST_FROM_ENTERED_SCHEDULES\", \"MANUAL_ENTRY_NOT_RECONCILED\"]
    and .action == \"You may compare your cash and bank balances with the upcoming payments entered for Thẻ tín dụng, then review the result.\"" '.suggestions[0] | del(.affectedRecords)'
ck A12-shared-liability "$B/A11.json" "[.attention[] | select(.ruleId == \"UPCOMING_LIABILITY_DUE\" or .ruleId == \"LIQUIDITY_SHORT_FOR_UPCOMING_OBLIGATIONS\") | [.affectedRecords[] | select(.sourceType == \"LIABILITY\") | .sourceId] | sort] | .[0] == .[1] and (.[0] | length) == 2" '[.attention[] | select(.tier == 1) | [.ruleId, [.affectedRecords[] | [.sourceType, .name]]]]'
ck A13-separate "$B/A11.json" "[.suggestions[] | select(any(.affectedRecords[]; .sourceId == \"$S_RENTAL\")) | .findingIds] == [[\"WEALTH_SNAPSHOT:CATEGORY_CONCENTRATION\"], [\"WEALTH_SNAPSHOT:LOW_LIQUIDITY_SHARE\"], [\"WEALTH_SNAPSHOT:STALE_VALUATION\"]]
    and ([.suggestions[] | select(.actionFamily == \"RECONCILE_CASH_WITH_RESERVE\") | .findingIds] | length) == 2" '[.suggestions[] | [.findingIds, .actionFamily]]'
ck A14-order "$B/A11.json" "($SUG | map(.[3])) == [\"WEALTH_SNAPSHOT:UPCOMING_LIABILITY_DUE+FINANCIAL_HEALTH:LIQUIDITY_SHORT_FOR_UPCOMING_OBLIGATIONS+FINANCIAL_HEALTH:FORECAST_CASH_NEGATIVE\",
    \"FINANCIAL_HEALTH:HIGH_DEBT_PAYMENT_BURDEN\", \"FINANCIAL_HEALTH:SPENDING_EXCEEDS_INCOME\", \"WEALTH_SNAPSHOT:CATEGORY_CONCENTRATION\", \"FINANCIAL_HEALTH:RESERVE_BELOW_REFERENCE\",
    \"FINANCIAL_HEALTH:INCOME_CONCENTRATION\", \"WEALTH_SNAPSHOT:LOW_LIQUIDITY_SHARE\", \"WEALTH_SNAPSHOT:STALE_EXCHANGE_RATE\", \"WEALTH_SNAPSHOT:STALE_VALUATION\"]
    and ($SUG | map(.[5])) == [0.933333, 2.137066, null, 0.980654, 0.766667, 0.666667, 0.650545, 14.4, 0.733333]
    and ($SUG | map(.[1])) == [true, true, true, false, false, false, false, false, false]" "$SUG"

# --- Z, partial; U, USD without a rate; E, empty --------------------------------------------------------------
adv A15 "$T_Z"; st A15-status A15 200
ck A15-partial "$B/A15.json" "[.unevaluated.findings[] | [.findingId, .reason]] == [[\"WEALTH_SNAPSHOT:UPCOMING_LIABILITY_DUE\", \"NO_LIABILITIES\"], [\"WEALTH_SNAPSHOT:STALE_EXCHANGE_RATE\", \"MISSING_EXCHANGE_RATE\"],
      [\"FINANCIAL_HEALTH:RESERVE_BELOW_REFERENCE\", \"INDICATOR_UNAVAILABLE\"], [\"FINANCIAL_HEALTH:HIGH_DEBT_PAYMENT_BURDEN\", \"INDICATOR_UNAVAILABLE\"],
      [\"FINANCIAL_HEALTH:LIQUIDITY_SHORT_FOR_UPCOMING_OBLIGATIONS\", \"NO_LIABILITIES\"], [\"FINANCIAL_HEALTH:INCOME_CONCENTRATION\", \"NO_INCOME\"]]
    and [.positive[].findingId] == [\"WEALTH_SNAPSHOT:DEBT_TO_ASSET_RATIO\", \"WEALTH_SNAPSHOT:LOW_LIQUIDITY_SHARE\", \"WEALTH_SNAPSHOT:STALE_VALUATION\", \"FINANCIAL_HEALTH:FORECAST_CASH_NEGATIVE\"]
    and $ATT == [\"WEALTH_SNAPSHOT:CATEGORY_CONCENTRATION\", \"FINANCIAL_HEALTH:SPENDING_EXCEEDS_INCOME\"]
    and [.suggestions[].suggestionId] == [\"FINANCIAL_HEALTH:SPENDING_EXCEEDS_INCOME\", \"WEALTH_SNAPSHOT:CATEGORY_CONCENTRATION\"]
    and all(.unevaluated.findings[]; .tier == null and .actionFamily == null and .observed == null)" '{unev: [.unevaluated.findings[] | [.findingId, .reason]], pos: [.positive[].findingId], att: [.attention[].findingId], sug: [.suggestions[].suggestionId]}'
recon A15 "$T_Z" 2026-11
adv A16 "$T_U"; st A16-status A16 200
ck A16-usd "$B/A16.json" "(.unevaluated.count == 8)
    and ([.unevaluated.findings[] | select(.source == \"WEALTH_SNAPSHOT\" and .reason == \"MISSING_EXCHANGE_RATE\") | .ruleId] == [\"CATEGORY_CONCENTRATION\", \"DEBT_TO_ASSET_RATIO\", \"LOW_LIQUIDITY_SHARE\", \"STALE_EXCHANGE_RATE\"])
    and ([.unevaluated.findings[] | select(.reason == \"INDICATOR_PARTIAL\") | .ruleId] == [\"FORECAST_CASH_NEGATIVE\", \"RESERVE_BELOW_REFERENCE\"])
    and all(.unevaluated.findings[], .positive[], .attention[]; if any(.affectedRecords[]; .sourceId == \"$U_USD\") and .ruleId != \"FORECAST_CASH_NEGATIVE\" then .noteId == \"USER_DECLARED_EXCHANGE_RATE\" else true end)
    and ((.unevaluated.findings[] | select(.ruleId == \"STALE_EXCHANGE_RATE\") | .affectedRecords) == [{sourceType: \"ASSET\", sourceId: \"$U_USD\", name: \"Tài khoản USD\", route: \"/api/v1/assets/$U_USD\"}])
    and $ATT == [\"FINANCIAL_HEALTH:INCOME_CONCENTRATION\"] and [.suggestions[].suggestionId] == [\"FINANCIAL_HEALTH:INCOME_CONCENTRATION\"]" '{unev: [.unevaluated.findings[] | [.ruleId, .reason, .noteId]], att: [.attention[].findingId]}'
recon A16 "$T_U" 2026-11
adv A17 "$T_E"; st A17-status A17 200
ck A17-empty "$B/A17.json" ".unevaluated.count == 12 and .positive == [] and .attention == [] and .suggestions == []
    and [.sources[].findingCount] == [6, 6, 0] and all(.unevaluated.findings[]; .affectedRecords == [] and .noteId == (if .ruleId == \"FORECAST_CASH_NEGATIVE\" then \"FORECAST_FROM_ENTERED_SCHEDULES\" else \"MANUAL_ENTRY_NOT_RECONCILED\" end))" '{count: .unevaluated.count, sources, reasons: [.unevaluated.findings[].reason]}'
recon A17 "$T_E" 2026-11

# --- ownership, errors ---------------------------------------------------------------------------------------
others_of() { grep -E "^(($1)_[A-Z0-9_]*|UID_($1))=" "$OUT/env.sh" | cut -d= -f2 | jq -R . | jq -sc .; }
ck A18-b-sees-only-b "$B/A01.json" "(tostring) as \$t | $(others_of 'S|U|Z') | all(. as \$id | \$t | contains(\$id) | not)" '.'
ck A18-s-sees-only-s "$B/A11.json" "(tostring) as \$t | $(others_of 'B') | all(. as \$id | \$t | contains(\$id) | not)" '.'
for q in 'month=2026-13' 'month=11%2F2026' 'month=2026-9' 'month=' 'month=2026-11&month=2026-12'; do
  id="A19-$(echo "$q" | tr -c 'A-Za-z0-9-' '_')"
  adv "$id" "$T_B" "?$q"; st "$id-status" "$id" 400
  curl -s -o "$B/$id-fh.json" "$BASE/financial-health?$q" -H "Authorization: Bearer $T_B"
  XA=(--slurpfile h "$B/$id-fh.json")
  ck "$id-same-as-health" "$B/$id.json" "del(.instance, .correlationId) == (\$h[0] | del(.instance, .correlationId)) and .type == \"urn:rikkaus:problem:validation-failed\"
      and .errors == [{field: \"month\", message: \"must be a calendar month in the form YYYY-MM\"}] and .instance == \"/api/v1/advisor-report\" and (tostring | test(\"2026-13|11/2026|2026-9\\\"\") | not)" '.'
done
XA=()
curl -s -o "$B/A20a.json" -w '%{http_code}' "$BASE/advisor-report" > "$B/A20a.status"; st A20-no-token A20a 401
ck A20-no-token-body "$B/A20a.json" '.type == "urn:rikkaus:problem:unauthorized"'
curl -s -o "$B/A20b.json" -w '%{http_code}' "$BASE/advisor-report" -H 'Authorization: Bearer not-a-token' > "$B/A20b.status"; st A20-bad-token A20b 401
curl -s -o "$B/A21.json" -w '%{http_code}' -X POST "$BASE/advisor-report" -H "Authorization: Bearer $T_B" > "$B/A21.status"; st A21-post A21 405
curl -s -o "$B/A22.json" -w '%{http_code}' "$BASE/advisor-report" -H "Authorization: Bearer $T_B" -H 'Accept: application/xml' > "$B/A22.status"; st A22-xml A22 406

# --- language over every body -----------------------------------------------------------------------------
for f in A01 A10 A11 A15 A16 A17; do
  ck "A23-$f-language" "$B/$f.json" 'all(.suggestions[].action; test("^You may ") and endswith("then review the result.") and (test("(?i)\\b(guarantee|guaranteed|will|ensure|must|should|urgent|immediately)\\b") | not) and (contains("!") | not))' '[.suggestions[].action]'
done
echo "run: $PASS PASS, $FAIL FAIL"
