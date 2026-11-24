#!/usr/bin/env bash
# Loads the design guideline's baseline fixture (docs/design-guidelines.md, section 16.3) into one
# development account, through the public API.
#
#   ./load-baseline.sh [email]              default email: an.nguyen@example.com
#   API_BASE_URL=http://localhost:8107 ./load-baseline.sh
#
# Idempotent: the account's assets, liabilities, cash-flow entries and goals are removed first, so a
# second run leaves the same records (with new ids). Dates that must stay "recent", "stale" or
# "upcoming" are computed from today in Asia/Ho_Chi_Minh, so the fixture holds on any day.
#
# What the dashboard shows afterwards:
#   total assets 12,002,000,000 VND, total liabilities 1,532,000,000 VND, net worth 10,470,000,000 VND
#   rate 1 USD = 25,100 VND declared for 12/09/2026; the 20,000 USD deposit converts to 502,000,000 VND
#   one stale valuation (the apartment, valued 75 days ago), one payment due within 30 days (the
#   mortgage, in 12 days), one concentration (real estate is 62.5% of assets), one AT_RISK goal
#   monthly income 180,000,000 VND, expenses 92,000,000 VND, net 88,000,000 VND
set -euo pipefail
# shellcheck source=lib.sh
source "$(cd "$(dirname "$0")" && pwd)/lib.sh"

EMAIL="${1:-an.nguyen@example.com}"

fixture_init
fixture_sign_in "$EMAIL" "${FIXTURE_DISPLAY_NAME:-An Nguyễn}"
delete_all_records

post_id() { api POST "$1" "$2" | jq -r .id; }

api PUT /settings/exchange-rate '{"usdToVndRate": 25100.000000, "effectiveDate": "2026-09-12"}' > /dev/null

# asset NAME CATEGORY VALUE CURRENCY VALUATION_DATE LIQUIDITY INSTITUTION [extra JSON merged in]
asset() {
  post_id /assets "$(jq -n --arg name "$1" --arg category "$2" --argjson value "$3" --arg currency "$4" \
    --arg date "$5" --arg liquidity "$6" --arg institution "$7" --argjson extra "${8:-"{}"}" \
    '{name: $name, category: $category, currentValue: $value, currency: $currency, valuationDate: $date,
      liquidity: $liquidity, institution: $institution} + $extra')"
}

asset "Tài khoản thanh toán Vietcombank" BANK_ACCOUNT 650000000 VND "$(day -2)" HIGH "Vietcombank" > /dev/null
asset "Tiền gửi kỳ hạn 12 tháng" TERM_DEPOSIT 1200000000 VND "$(day -6)" MEDIUM "Techcombank" \
  '{"interestRate": 5.2, "notes": "Đáo hạn tự động, lãi nhập gốc."}' > /dev/null
asset "Danh mục cổ phiếu HOSE" LISTED_SECURITIES 2150000000 VND "$(day -1)" HIGH "Chứng khoán SSI" > /dev/null
APARTMENT="$(asset "Căn hộ Thảo Điền" REAL_ESTATE 7500000000 VND "$(day -75)" LOW "Thành phố Thủ Đức" \
  '{"purchaseValue": 5200000000, "purchaseDate": "2021-09-20", "notes": "Định giá theo môi giới, cần cập nhật."}')"
asset "Tiền gửi USD" BANK_ACCOUNT 20000 USD "$(day -3)" HIGH "HSBC Việt Nam" \
  '{"usdToVndRate": 25100.000000, "rateEffectiveDate": "2026-09-12"}' > /dev/null

MORTGAGE="$(post_id /liabilities "$(jq -n --arg next "$(day 12)" --arg collateral "$APARTMENT" '{
  name: "Vay mua nhà Vietcombank", category: "BANK_LOAN", outstandingBalance: 1500000000, currency: "VND",
  interestRate: 8.5, startDate: "2021-09-20", maturityDate: "2041-09-20",
  recurringPaymentAmount: 18750000, recurringPaymentFrequency: "MONTHLY", nextPaymentDate: $next,
  collateralAssetId: $collateral}')")"
post_id /liabilities "$(jq -n --arg next "$(day 40)" '{
  name: "Thẻ tín dụng Techcombank Visa", category: "CREDIT_CARD_BALANCE", outstandingBalance: 32000000,
  currency: "VND", nextPaymentDate: $next}')" > /dev/null

# entry TYPE CATEGORY AMOUNT START NOTES [extra JSON] — a monthly recurring entry, open-ended.
entry() {
  post_id /cash-flow-entries "$(jq -n --arg type "$1" --arg category "$2" --argjson amount "$3" \
    --arg start "$4" --arg notes "$5" --argjson extra "${6:-"{}"}" \
    '{entryType: $type, category: $category, amount: $amount, currency: "VND", timing: "RECURRING",
      recurrence: "MONTHLY", effectiveDate: $start, notes: $notes} + $extra')" > /dev/null
}

entry INCOME SALARY 180000000 2026-01-05 "Lương và thưởng hiệu suất hằng tháng"
entry EXPENSE LIVING_COSTS 52250000 2026-01-10 "Chi tiêu sinh hoạt gia đình"
entry EXPENSE EDUCATION 21000000 2026-01-15 "Học phí trường quốc tế của con"
entry EXPENSE DEBT_PRINCIPAL_PAYMENT 18750000 2026-01-20 "Trả góp vay mua nhà Vietcombank" \
  "$(jq -n --arg id "$MORTGAGE" '{linkedLiabilityId: $id}')"

# goal_with_coverage NAME TYPE TARGET ALLOCATED TARGET_DATE RETURN PERCENT — creates the goal, reads the
# contribution the projection requires today, and states a contribution of PERCENT of it (rounded down
# to 100,000 VND). Reading it from the API keeps the status right on whatever day the script runs.
goal_with_coverage() {
  local body id required
  body="$(jq -n --arg name "$1" --arg type "$2" --argjson target "$3" --argjson allocated "$4" \
    --arg date "$5" --argjson ret "$6" \
    '{name: $name, type: $type, targetAmount: $target, currentAllocatedAmount: $allocated,
      targetDate: $date, expectedAnnualReturnPercent: $ret, plannedMonthlyContribution: 0}')"
  id="$(post_id /goals "$body")"
  required="$(api GET "/goal-projections/$id" | jq -r .requiredMonthlyContribution)"
  api PUT "/goals/$id" "$(jq --argjson required "$required" --argjson percent "$7" \
    '.plannedMonthlyContribution = (($required * $percent / 100 / 100000) | floor) * 100000' <<< "$body")" > /dev/null
}

goal_with_coverage "Quỹ du học cho con" EDUCATION 3500000000 800000000 2031-08-31 6 90
goal_with_coverage "Quỹ dự phòng 6 tháng chi tiêu" EMERGENCY_FUND 552000000 400000000 2027-08-31 0 130

api PATCH /onboarding '{"currentStep": 7, "dashboardReviewed": true, "status": "COMPLETED"}' > /dev/null

api GET /wealth-snapshot | jq -r --arg email "$EMAIL" '
  "Loaded the baseline for \($email): total assets \(.kpis.totalAssets.amountVnd), total liabilities \(.kpis.totalLiabilities.amountVnd), net worth \(.kpis.netWorth)."'
