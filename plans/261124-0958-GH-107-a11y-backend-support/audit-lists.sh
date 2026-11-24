#!/usr/bin/env bash
# Scenarios L01-L07: every list route measured on the long-lists account. Run after
# services/api/fixtures/load-long-lists.sh. Prints one Markdown table row per route and check.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
# shellcheck source=../../services/api/fixtures/lib.sh
source "$ROOT/services/api/fixtures/lib.sh"
fixture_init
fixture_sign_in "${1:-long.list@example.com}" "Lê Thị Danh Sách Dài"

# get PATH -> writes the body to $FIXTURE_TMP/out, prints "status bytes seconds"
get() {
  curl -sS -o "$FIXTURE_TMP/out" -w '%{http_code} %{size_download} %{time_total}' \
    -H "@$FIXTURE_TMP/auth.header" -H 'Accept: application/json' -H 'X-Correlation-Id: audit-gh107-0001' "$API$1"
}
sha() { shasum -a 256 "$FIXTURE_TMP/out" | cut -c1-12; }
join() { if [[ "$1" == *\?* ]]; then echo "$1&$2"; else echo "$1?$2"; fi; }

HISTORY="$(api GET '/assets?q=lich%20su%2024%20thang' | jq -r '.[0].id')"
MONTH_FROM="$(TZ=Asia/Ho_Chi_Minh date +%Y-%m-01)"
YEAR_TO="$(day 365)"
ROUTES=(/assets "/assets/$HISTORY/valuations" /liabilities /cash-flow-entries
  "/cash-flow-occurrences?from=$MONTH_FROM&to=$YEAR_TO" /goals /goal-projections /feedback /wealth-snapshot
  /cash-flow-forecast "/cash-flow-reports/monthly?month=${MONTH_FROM:0:7}")

echo "| Route | Status | Elements | Bytes | Median s (5) | Max s (5) | Stable x2 | page/limit/offset/size ignored |"
echo "|---|---|---|---|---|---|---|---|"
for route in "${ROUTES[@]}"; do
  times=()
  for _ in 1 2 3 4 5; do
    read -r status bytes seconds <<< "$(get "$route")"
    times+=("$seconds")
  done
  first="$(sha)"
  elements="$(jq 'if type == "array" then length else "object" end' "$FIXTURE_TMP/out")"
  get "$route" > /dev/null
  stable=$([[ "$(sha)" == "$first" ]] && echo yes || echo NO)
  ignored=yes
  for extra in page=2 limit=5 offset=10 size=5 "page=2&limit=5&offset=10&size=5&per_page=5&cursor=abc"; do
    get "$(join "$route" "$extra")" > /dev/null
    [[ "$(sha)" == "$first" ]] || ignored="NO ($extra)"
  done
  sorted="$(printf '%s\n' "${times[@]}" | sort -n)"
  median="$(sed -n 3p <<< "$sorted")"
  max="$(sed -n 5p <<< "$sorted")"
  printf '| `%s` | %s | %s | %s | %s | %s | %s | %s |\n' "${route/$HISTORY/{id\}}" "$status" "$elements" "$bytes" \
    "$median" "$max" "$stable" "$ignored"
done

echo
echo "| Sort | Elements | Stable x2 | First three names |"
echo "|---|---|---|---|"
for spec in "/assets -valuationDate valuationDate name -name" "/liabilities nextPaymentDate -nextPaymentDate name -name" \
  "/cash-flow-entries -effectiveDate effectiveDate" "/goals targetDate -targetDate name -name"; do
  read -r -a parts <<< "$spec"
  for key in "${parts[@]:1}"; do
    get "${parts[0]}?sort=$key" > /dev/null
    first="$(sha)"
    elements="$(jq length "$FIXTURE_TMP/out")"
    names="$(jq -r '[.[0:3][] | (.name // .notes // "—")[0:28]] | join(" / ")' "$FIXTURE_TMP/out")"
    get "${parts[0]}?sort=$key" > /dev/null
    printf '| `%s?sort=%s` | %s | %s | %s |\n' "${parts[0]}" "$key" "$elements" \
      "$([[ "$(sha)" == "$first" ]] && echo yes || echo NO)" "$names"
  done
done

echo
echo "Ties (identical name and date, ordered by id):"
api GET '/assets?sort=name' | jq -c '[.[] | select(.name == "Vàng miếng SJC 1 lượng" or .name == "Tiền mặt") | {name, valuationDate, id}]'
echo "Default orders:"
api GET /assets | jq -c '[.[].valuationDate] | {sortedDesc: (. == (sort | reverse))}'
api GET /liabilities | jq -c '[.[].nextPaymentDate] | {nullsLast: ((map(select(. != null)) | length) as $n | (.[$n:] | all(. == null))), datesAscending: (map(select(. != null)) | . == sort)}'
api GET /cash-flow-entries | jq -c '[.[].effectiveDate] | {sortedDesc: (. == (sort | reverse))}'
api GET /goals | jq -c '[.[].targetDate] | {sortedAsc: (. == sort)}'
api GET /feedback | jq -c '[.[].createdAt] | {newestFirst: (. == (sort | reverse))}'
echo "Names around D and Đ under sort=name:"
api GET '/assets?sort=name' | jq -c '[.[].name | select(startswith("D") or startswith("Đ"))]'
echo "Longest name returned (characters, NFC as sent):"
api GET /assets | jq -c '[.[].name | length] | max'
