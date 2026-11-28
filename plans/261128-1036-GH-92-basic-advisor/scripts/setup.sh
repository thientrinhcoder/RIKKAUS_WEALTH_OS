#!/usr/bin/env bash
# Seeds the five users of phase-01-test-scenarios.md through the API on $BASE (local profile, dev sign-in)
# and writes their ids to $OUT/env.sh. The dates are those of the run on 28/11/2026. One goal's target date
# is moved into the past with SQL in the disposable container $PG, since the API refuses a past target date.
# Tokens are minted again by cases.sh, since an access token lives 15 minutes; nothing secret is written.
set -euo pipefail
BASE=${BASE:-http://127.0.0.1:8092/api/v1}
OUT=${OUT:?set OUT to a scratch directory}
PG=${PG:-rikkaus-pg-92}
mkdir -p "$OUT"
: > "$OUT/env.sh"

signin() { curl -sf -X POST "$BASE/auth/dev-sign-in" -H 'Content-Type: application/json' \
  -d "{\"email\":\"$1\",\"displayName\":\"$2\"}"; }
api() {
  local code
  code=$(curl -s -o "$OUT/last.json" -w '%{http_code}' -H "Authorization: Bearer $T" -H 'Content-Type: application/json' \
    -X "$1" "$BASE$2" ${3:+-d "$3"})
  if [[ "$code" != 2* ]]; then echo "$1 $2 -> $code $(cat "$OUT/last.json")" >&2; exit 1; fi
  cat "$OUT/last.json"
}
rate() { api PUT /settings/exchange-rate '{"usdToVndRate":"25100","effectiveDate":"2026-09-12"}' > /dev/null; }
# asset NAME CATEGORY VALUE CURRENCY VALUATION_DATE LIQUIDITY
asset() { api POST /assets "$(jq -n --arg n "$1" --arg c "$2" --arg v "$3" --arg cu "$4" --arg d "$5" --arg l "$6" \
  '{name:$n, category:$c, currentValue:$v, currency:$cu, valuationDate:$d, liquidity:$l}')" | jq -r .id; }
liab() { api POST /liabilities "$1" | jq -r .id; }
# once TYPE CATEGORY AMOUNT DATE NOTES [extra]
once() { api POST /cash-flow-entries "$(jq -n --arg t "$1" --arg c "$2" --arg a "$3" --arg d "$4" --arg n "$5" --argjson x "${6:-"{}"}" \
  '{entryType:$t, category:$c, amount:$a, currency:"VND", timing:"ONE_TIME", effectiveDate:$d, notes:$n} + $x')" | jq -r .id; }
# every TYPE CATEGORY AMOUNT RECURRENCE START NOTES [extra]
every() { api POST /cash-flow-entries "$(jq -n --arg t "$1" --arg c "$2" --arg a "$3" --arg r "$4" --arg d "$5" --arg n "$6" --argjson x "${7:-"{}"}" \
  '{entryType:$t, category:$c, amount:$a, currency:"VND", timing:"RECURRING", recurrence:$r, effectiveDate:$d, notes:$n} + $x')" | jq -r .id; }
# goal NAME TYPE TARGET DATE ALLOCATED PLANNED|"" RETURN
goal() { api POST /goals "$(jq -n --arg n "$1" --arg t "$2" --arg a "$3" --arg d "$4" --arg al "$5" --arg p "$6" --arg r "$7" \
  '{name:$n, type:$t, targetAmount:$a, targetDate:$d, currentAllocatedAmount:$al, expectedAnnualReturnPercent:$r}
   + (if $p == "" then {} else {plannedMonthlyContribution:$p} end)')" | jq -r .id; }
asset_link() { jq -n --arg id "$1" '{linkedAssetId:$id}'; }
liab_link() { jq -n --arg id "$1" '{linkedLiabilityId:$id}'; }
save() { echo "$1=$2" >> "$OUT/env.sh"; }

user() { # user KEY EMAIL NAME -> sets T, writes UID_/EMAIL_
  local body; body=$(signin "$2" "$3")
  T=$(jq -r .accessToken <<<"$body")
  save "UID_$1" "$(jq -r .user.id <<<"$body")"
  save "EMAIL_$1" "$2"
}

mortgage() { liab '{"name":"Khoản vay mua căn hộ","category":"BANK_LOAN","outstandingBalance":"1500000000","currency":"VND","interestRate":"8.5","startDate":"2021-09-20","maturityDate":"2041-09-20","recurringPaymentAmount":"18750000","recurringPaymentFrequency":"MONTHLY","nextPaymentDate":"2026-12-20"}'; }
card() { liab '{"name":"Thẻ tín dụng","category":"CREDIT_CARD_BALANCE","outstandingBalance":"32000000","currency":"VND","maturityDate":"2026-11-30"}'; }

# B — the design #90 baseline moved to November 2026: 12.002.000.000 ₫ of assets, 1.532.000.000 ₫ of
# liabilities, 180.000.000 ₫ of income and 92.000.000 ₫ of expense, and six goals.
user B adv.base@example.com "An Nguyễn"; rate
save B_CASH "$(asset "Tiền mặt dự phòng" CASH 148000000 VND 2026-11-25 HIGH)"
save B_USD "$(asset "Tài khoản USD" BANK_ACCOUNT 20000 USD 2026-11-24 HIGH)"
B_TERM=$(asset "Tiền gửi kỳ hạn 12 tháng" TERM_DEPOSIT 1200000000 VND 2026-11-21 MEDIUM); save B_TERM "$B_TERM"
B_STOCKS=$(asset "Danh mục cổ phiếu niêm yết" LISTED_SECURITIES 2352000000 VND 2026-11-26 HIGH); save B_STOCKS "$B_STOCKS"
B_FLAT=$(asset "Căn hộ Thảo Điền" REAL_ESTATE 7500000000 VND 2026-10-11 LOW); save B_FLAT "$B_FLAT"
B_FUND=$(asset "Chứng chỉ quỹ trái phiếu" INVESTMENT_FUND 300000000 VND 2026-11-20 MEDIUM); save B_FUND "$B_FUND"
B_LOAN=$(mortgage); save B_LOAN "$B_LOAN"; save B_CARD "$(card)"
save B_SALARY "$(every INCOME SALARY 132000000 MONTHLY 2026-01-05 'Lương Công ty ABC')"
save B_BUSINESS "$(once INCOME BUSINESS_INCOME 26200000 2026-11-12 'Thu nhập kinh doanh')"
save B_RENT "$(every INCOME RENTAL_INCOME 14800000 MONTHLY 2026-01-10 'Thu nhập cho thuê Căn hộ Thảo Điền' "$(asset_link "$B_FLAT")")"
save B_DIVIDEND "$(api POST /cash-flow-entries "$(jq -n --arg id "$B_STOCKS" '{entryType:"INCOME", category:"DIVIDEND", amount:"150", currency:"USD", timing:"RECURRING", recurrence:"QUARTERLY", effectiveDate:"2026-11-15", linkedAssetId:$id, notes:"Cổ tức 150 USD"}')" | jq -r .id)"
save B_INTEREST_IN "$(every INCOME DEPOSIT_INTEREST 1500000 MONTHLY 2026-01-25 'Lãi tiền gửi' "$(asset_link "$B_TERM")")"
save B_BOND "$(every INCOME BOND_OR_FUND_INCOME 1500000 QUARTERLY 2026-11-20 'Thu nhập quỹ trái phiếu' "$(asset_link "$B_FUND")")"
save B_OTHER_IN "$(once INCOME OTHER_INCOME 235000 2026-11-18 'Thu nhập khác')"
save B_LIVING "$(every EXPENSE LIVING_COSTS 23400000 MONTHLY 2026-01-03 'Chi phí sinh hoạt')"
save B_HOUSING "$(every EXPENSE HOUSING 18750000 MONTHLY 2026-01-20 'Nhà ở' "$(liab_link "$B_LOAN")")"
save B_EDU "$(once EXPENSE EDUCATION 12800000 2026-11-08 'Học phí')"
save B_HEALTH "$(once EXPENSE HEALTHCARE 4650000 2026-11-14 'Khám sức khỏe')"
save B_INSURANCE "$(every EXPENSE INSURANCE 5400000 YEARLY 2025-11-16 'Bảo hiểm sức khỏe gia đình')"
save B_TAX "$(every EXPENSE TAX_AND_FEES 8250000 QUARTERLY 2026-11-22 'Thuế và phí')"
save B_PRINCIPAL "$(every EXPENSE DEBT_PRINCIPAL_PAYMENT 10000000 MONTHLY 2026-01-20 'Trả gốc vay căn hộ' "$(liab_link "$B_LOAN")")"
save B_INTEREST "$(every EXPENSE INTEREST_PAYMENT 6250000 MONTHLY 2026-01-20 'Trả lãi vay căn hộ' "$(liab_link "$B_LOAN")")"
save B_OTHER_EX "$(once EXPENSE OTHER_EXPENSE 2500000 2026-11-24 'Chi phí khác')"
save B_G1 "$(goal 'Quỹ dự phòng 6 tháng' EMERGENCY_FUND 370000000 2027-11-30 250000000 10200000 3.00)"
save B_G2 "$(goal 'Mua nhà phố Thủ Đức' HOME_PURCHASE 3000000000 2029-11-30 1800000000 21000000 5.00)"
save B_G3 "$(goal 'Học phí đại học cho con' EDUCATION 1200000000 2034-08-31 200000000 5000000 6.00)"
save B_G4 "$(goal 'Du lịch Nhật Bản cuối năm' OTHER_GOAL 60000000 2026-11-30 45000000 5000000 0.00)"
B_G5=$(goal 'Quỹ cưới' OTHER_GOAL 300000000 2027-06-30 240000000 10000000 4.00); save B_G5 "$B_G5"
save B_G6 "$(goal 'Quỹ hưu trí' RETIREMENT 5000000000 2046-12-31 600000000 '' 6.00)"
# The API refuses a past target date; an expired goal is one whose date has since passed.
docker exec "$PG" psql -U rikkaus -d rikkaus -qtAc "UPDATE wealth.goals SET target_date = DATE '2026-10-31' WHERE id = '$B_G5'" > /dev/null

# S — stress: the #83 variant that fires all six health rules, with the same two liabilities as B.
user S adv.stress@example.com "Sơn Phạm"; rate
save S_CASH "$(asset "Tiền mặt" CASH 12000000 VND 2026-11-26 HIGH)"
save S_BANK "$(asset "Tài khoản thanh toán" BANK_ACCOUNT 30000000 VND 2026-11-26 HIGH)"
S_RENTAL=$(asset "Căn hộ cho thuê Quận 2" REAL_ESTATE 4300000000 VND 2026-10-07 LOW); save S_RENTAL "$S_RENTAL"
S_LOAN=$(mortgage); save S_LOAN "$S_LOAN"; save S_CARD "$(card)"
save S_SALARY "$(every INCOME SALARY 132000000 MONTHLY 2026-01-05 'Lương Công ty ABC, kết thúc khi đổi việc' '{"endDate":"2026-10-31"}')"
save S_RENT "$(every INCOME RENTAL_INCOME 14800000 MONTHLY 2026-01-10 'Thu nhập cho thuê' "$(asset_link "$S_RENTAL")")"
save S_LIVING "$(every EXPENSE LIVING_COSTS 23400000 MONTHLY 2026-01-03 'Chi phí sinh hoạt')"
save S_HOUSING "$(every EXPENSE HOUSING 18750000 MONTHLY 2026-01-20 'Nhà ở' "$(liab_link "$S_LOAN")")"
save S_INSURANCE "$(every EXPENSE INSURANCE 5400000 YEARLY 2025-11-16 'Bảo hiểm sức khỏe gia đình')"
save S_TAX "$(every EXPENSE TAX_AND_FEES 8250000 QUARTERLY 2026-11-22 'Thuế và phí')"
save S_PRINCIPAL "$(every EXPENSE DEBT_PRINCIPAL_PAYMENT 10000000 MONTHLY 2026-01-20 'Trả gốc vay căn hộ' "$(liab_link "$S_LOAN")")"
save S_INTEREST "$(every EXPENSE INTEREST_PAYMENT 6250000 MONTHLY 2026-01-20 'Trả lãi vay căn hộ' "$(liab_link "$S_LOAN")")"

# Z — partial: expenses this month, no income, no liability, no rate.
user Z adv.partial@example.com "Zung Hoàng"
save Z_CASH "$(asset "Tiền mặt" CASH 50000000 VND 2026-11-26 HIGH)"
save Z_LIVING "$(once EXPENSE LIVING_COSTS 23400000 2026-11-10 'Chi phí sinh hoạt tháng 11')"
save Z_EDU "$(once EXPENSE EDUCATION 12800000 2026-11-15 'Học phí')"
save Z_OTHER "$(once EXPENSE OTHER_EXPENSE 2500000 2026-11-20 'Chi phí khác')"

# U — a USD account and no declared rate.
user U adv.usd@example.com "Uyên Ngô"
save U_CASH "$(asset "Tiền mặt" CASH 100000000 VND 2026-11-26 HIGH)"
save U_USD "$(asset "Tài khoản USD" BANK_ACCOUNT 5000 USD 2026-11-26 HIGH)"
save U_SALARY "$(every INCOME SALARY 40000000 MONTHLY 2026-01-05 'Lương Công ty ABC')"
save U_LIVING "$(every EXPENSE LIVING_COSTS 20000000 MONTHLY 2026-01-03 'Chi phí sinh hoạt')"

# E — no record at all.
user E adv.empty@example.com "Em Võ"

echo "seeded $(grep -c . "$OUT/env.sh") ids into $OUT/env.sh"
