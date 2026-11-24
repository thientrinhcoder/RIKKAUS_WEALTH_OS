#!/usr/bin/env bash
# Scenarios E01-E11: the error shape a form or list needs, measured on the real API. Run after
# load-baseline.sh (an.nguyen@example.com). Signs in a second account, bich.tran@example.com, for the
# ownership cases. Prints one Markdown table row per request.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
# shellcheck source=../../services/api/fixtures/lib.sh
source "$ROOT/services/api/fixtures/lib.sh"
fixture_init

fixture_sign_in bich.tran@example.com "Bích Trần"
mv "$FIXTURE_TMP/auth.header" "$FIXTURE_TMP/b.header"
cp "$FIXTURE_TMP/b.header" "$FIXTURE_TMP/auth.header"
B_ASSET="$(api POST /assets "{\"name\":\"Sổ tiết kiệm Agribank\",\"category\":\"TERM_DEPOSIT\",\"currentValue\":300000000,\"currency\":\"VND\",\"valuationDate\":\"$(day -3)\",\"liquidity\":\"MEDIUM\"}" | jq -r .id)"
B_LIABILITY="$(api POST /liabilities '{"name":"Vay tiêu dùng FE Credit","category":"PERSONAL_LOAN","outstandingBalance":50000000,"currency":"VND"}' | jq -r .id)"
B_ENTRY="$(api POST /cash-flow-entries "{\"entryType\":\"EXPENSE\",\"category\":\"LIVING_COSTS\",\"amount\":12500000,\"currency\":\"VND\",\"timing\":\"ONE_TIME\",\"effectiveDate\":\"$(day -1)\"}" | jq -r .id)"
B_GOAL="$(api POST /goals '{"name":"Mua xe máy điện VinFast","type":"OTHER_GOAL","targetAmount":45000000,"targetDate":"2027-06-30"}' | jq -r .id)"

fixture_sign_in an.nguyen@example.com "An Nguyễn"
APARTMENT="$(api GET '/assets?q=thao%20dien' | jq -r '.[0].id')"
MISSING=00000000-0000-4000-8000-000000000000

# call ID METHOD PATH [BODY] [extra curl args...] — prints a table row; keeps the body in $FIXTURE_TMP/r.json
call() {
  local id="$1" method="$2" path="$3" body="${4:-}"
  shift 4 || shift $#
  local args=(-sS -D "$FIXTURE_TMP/h.txt" -o "$FIXTURE_TMP/r.json" -w '%{http_code}' -X "$method"
    -H "@$FIXTURE_TMP/${AUTH_FILE:-auth.header}" -H 'X-Correlation-Id: audit-gh107-errors')
  if [[ -n "$body" ]]; then args+=(-H 'Content-Type: application/json' --data-binary "$body"); fi
  local code ctype summary
  code="$(curl "${args[@]}" "$@" "$API$path")"
  ctype="$(grep -i '^content-type:' "$FIXTURE_TMP/h.txt" | tr -d '\r' | cut -d' ' -f2- || true)"
  summary="$(jq -r '[(.type // "-" | sub("urn:rikkaus:problem:"; "")),
      (if (has("title") and has("status") and has("detail") and has("instance") and has("correlationId")) then "all5" else "MISSING" end),
      (if has("errors") then (.errors | map(.field) | join(",")) else "-" end),
      (if has("impact") then "impact.confirmationRequired=\(.impact.confirmationRequired)" else "" end)] | join(" | ")' \
      "$FIXTURE_TMP/r.json" 2> /dev/null || echo "non-JSON | - | - |")"
  printf '| %s | `%s %s` | %s | %s | %s |\n' "$id" "$method" "${path:0:70}" "$code" "${ctype:--}" "$summary"
}

echo "| ID | Request | Status | Content-Type | type \\| type,title,status,detail,instance,correlationId \\| errors[].field \\| extra |"
echo "|---|---|---|---|---|"
D="$(day -3)"
call E01 POST /assets '{}'
call E02 POST /assets "{\"name\":\"Tài khoản USD\",\"category\":\"BANK_ACCOUNT\",\"currentValue\":20000,\"currency\":\"USD\",\"valuationDate\":\"$D\",\"liquidity\":\"HIGH\",\"quantity\":0,\"symbol\":\"VCB\",\"usdToVndRate\":25100}"
call E03a POST /liabilities '{"name":"Vay mua xe","category":"BANK_LOAN","outstandingBalance":350000000,"currency":"VND","startDate":"2026-05-01","maturityDate":"2026-01-01","nextPaymentDate":"2027-01-01","recurringPaymentAmount":9500000}'
call E03b POST /cash-flow-entries "{\"entryType\":\"EXPENSE\",\"category\":\"SALARY\",\"amount\":0,\"currency\":\"VND\",\"timing\":\"ONE_TIME\",\"recurrence\":\"MONTHLY\",\"endDate\":\"$D\",\"effectiveDate\":\"$D\",\"linkedAssetId\":\"$APARTMENT\",\"linkedLiabilityId\":\"$MISSING\"}"
call E03c POST /goals '{"name":"Quỹ hưu trí","type":"RETIREMENT","targetAmount":1000000000,"currentAllocatedAmount":1200000000,"targetDate":"2025-01-01","expectedAnnualReturnPercent":31}'
call E03d POST "/assets/$APARTMENT/valuations" '{"value":-1,"currency":"USD","effectiveDate":"2030-01-01","usdToVndRate":25100}'
call E03e POST /feedback '{"screen":"Màn hình chính","message":"   "}'
call E03f PUT /settings/exchange-rate '{"usdToVndRate":0,"effectiveDate":"2030-01-01"}'
call E03g PUT /profile "{\"displayName\":\"$(printf 'A%.0s' $(seq 1 121))\"}"
LONG121="$(printf 'Đ%.0s' $(seq 1 121))"
call E04a POST /assets "{\"name\":\"$LONG121\",\"category\":\"CASH\",\"currentValue\":1000000.005,\"currency\":\"VND\",\"valuationDate\":\"$D\",\"liquidity\":\"HIGH\",\"notes\":\"$(printf 'x%.0s' $(seq 1 2001))\"}"
call E04b POST /assets "{\"name\":\"Tiền mặt\",\"category\":\"CASH\",\"currentValue\":123456789012345678,\"currency\":\"VND\",\"valuationDate\":\"$D\",\"liquidity\":\"HIGH\"}"
call E05a POST /assets '{"name":'
call E05b POST /assets "{\"name\":\"Tiền mặt\",\"category\":\"CASH\",\"currentValue\":1000000,\"currency\":\"VND\",\"valuationDate\":\"2026-13-45\",\"liquidity\":\"HIGH\"}"
call E05c POST /assets "{\"name\":\"Tiền mặt\",\"category\":\"cash\",\"currentValue\":1000000,\"currency\":\"VND\",\"valuationDate\":\"$D\",\"liquidity\":\"HIGH\"}"
call E05d POST /assets "{\"name\":\"Tiền mặt\",\"category\":\"CASH\",\"currentValue\":\"một triệu\",\"currency\":\"VND\",\"valuationDate\":\"$D\",\"liquidity\":\"HIGH\"}"
call E05e PATCH /onboarding '{"currentStep":2.5}'
call E06a GET '/assets?sort=bogus&category=cash&liquidity=high'
call E06b GET "/assets?q=$(printf 'a%.0s' $(seq 1 121))"
call E06c GET '/cash-flow-entries?effectiveFrom=2026-13-01&effectiveTo=2026-01-01'
call E06d GET '/cash-flow-entries?effectiveFrom=2026-06-01&effectiveTo=2026-01-01'
call E06e GET '/cash-flow-occurrences?from=2026-12-01&to=2026-01-01'
call E06f GET '/cash-flow-reports/monthly?month=2026-13'
call E06g GET '/cash-flow-forecast?from=hom-nay'
call E06h DELETE "/assets/$APARTMENT?confirm=maybe"
call E06i GET /cash-flow-occurrences
call E08 DELETE "/assets/$APARTMENT"
touch "$FIXTURE_TMP/anonymous.header"
AUTH_FILE=anonymous.header call E09a GET /assets
call E09b PATCH /assets '{}'
call E09c GET /assets '' -H 'Accept: text/csv'
call E09d POST /assets 'name=Tiền mặt' -H 'Content-Type: text/plain'
call E09e GET /khong-ton-tai
call E09f GET /assets/khong-phai-uuid

echo
echo "E07 — another user's id against an id that exists nowhere (instance id replaced by {id}):"
echo
echo "| Request | Other user's | Missing | Identical |"
echo "|---|---|---|---|"
compare() {
  local method="$1" template="$2" other="$3" body="${4:-}" a b
  local args=(-sS -X "$method" -H "@$FIXTURE_TMP/auth.header" -H 'X-Correlation-Id: audit-gh107-own')
  if [[ -n "$body" ]]; then args+=(-H 'Content-Type: application/json' --data-binary "$body"); fi
  curl "${args[@]}" -D "$FIXTURE_TMP/ha" -o "$FIXTURE_TMP/a" "$API${template//\{id\}/$other}"
  curl "${args[@]}" -D "$FIXTURE_TMP/hb" -o "$FIXTURE_TMP/b" "$API${template//\{id\}/$MISSING}"
  a="$(sed "s/$other/{id}/g" "$FIXTURE_TMP/a")"
  b="$(sed "s/$MISSING/{id}/g" "$FIXTURE_TMP/b")"
  printf '| `%s %s` | %s | %s | %s |\n' "$method" "$template" "$(head -1 "$FIXTURE_TMP/ha" | cut -d' ' -f2)" \
    "$(head -1 "$FIXTURE_TMP/hb" | cut -d' ' -f2)" "$([[ "$a" == "$b" ]] && echo "yes, $(wc -c < "$FIXTURE_TMP/a" | tr -d ' ') bytes" || echo NO)"
}
ASSET_PUT='{"name":"Sổ tiết kiệm","liquidity":"MEDIUM"}'
compare GET '/assets/{id}' "$B_ASSET"
compare PUT '/assets/{id}' "$B_ASSET" "$ASSET_PUT"
compare DELETE '/assets/{id}?confirm=true' "$B_ASSET"
compare GET '/assets/{id}/deletion-impact' "$B_ASSET"
compare GET '/assets/{id}/valuations' "$B_ASSET"
compare POST '/assets/{id}/valuations' "$B_ASSET" "{\"value\":310000000,\"currency\":\"VND\",\"effectiveDate\":\"$D\"}"
compare GET '/liabilities/{id}' "$B_LIABILITY"
compare PUT '/liabilities/{id}' "$B_LIABILITY" '{"name":"Vay","outstandingBalance":1,"currency":"VND"}'
compare DELETE '/liabilities/{id}?confirm=true' "$B_LIABILITY"
compare GET '/liabilities/{id}/deletion-impact' "$B_LIABILITY"
compare GET '/cash-flow-entries/{id}' "$B_ENTRY"
compare DELETE '/cash-flow-entries/{id}' "$B_ENTRY"
compare GET '/goals/{id}' "$B_GOAL"
compare DELETE '/goals/{id}' "$B_GOAL"
compare GET '/goal-projections/{id}' "$B_GOAL"

echo
echo "E10 — correlation and CORS from the Expo web origin:"
curl -sS -D - -o /dev/null -H "@$FIXTURE_TMP/auth.header" -H 'X-Correlation-Id: audit-gh107-cors' -H 'Origin: http://localhost:8081' \
  "$API/assets/$MISSING" | tr -d '\r' | grep -iE '^(HTTP|x-correlation-id|access-control-allow-origin|access-control-expose-headers|content-type)'
curl -sS -H "@$FIXTURE_TMP/auth.header" -H 'X-Correlation-Id: audit-gh107-cors' "$API/assets/$MISSING" | jq -c '{correlationId}'
echo "Preflight:"
curl -sS -D - -o /dev/null -X OPTIONS -H 'Origin: http://localhost:8081' -H 'Access-Control-Request-Method: DELETE' \
  -H 'Access-Control-Request-Headers: authorization,content-type,x-correlation-id' "$API/assets/$MISSING" | tr -d '\r' \
  | grep -iE '^(HTTP|access-control-allow-(origin|methods|headers))'

echo
echo "Cleanup: the second account's records."
cp "$FIXTURE_TMP/b.header" "$FIXTURE_TMP/auth.header"
delete_all_records
echo "done"
