#!/usr/bin/env bash
# Seeds the nine users of phase-01-test-scenarios.md through the API on $BASE (local profile, dev sign-in)
# and writes their ids to $OUT/env.sh. The dates are the ones of the run on 27/11/2026; the expected
# values of phase 1 hold for month 2026-11 read on that day. Tokens are minted again by cases.sh, since an
# access token lives 15 minutes; nothing secret is written here.
set -euo pipefail
BASE=${BASE:-http://127.0.0.1:8083/api/v1}
OUT=${OUT:?set OUT to a scratch directory}
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
asset() { api POST /assets "{\"name\":\"$1\",\"category\":\"$2\",\"currentValue\":\"$3\",\"currency\":\"$4\",\"valuationDate\":\"$5\",\"liquidity\":\"$6\"}" | jq -r .id; }
liab() { api POST /liabilities "$1" | jq -r .id; }
# once TYPE CATEGORY AMOUNT DATE NOTES [extra]
once() { api POST /cash-flow-entries "$(jq -n --arg t "$1" --arg c "$2" --arg a "$3" --arg d "$4" --arg n "$5" --argjson x "${6:-"{}"}" \
  '{entryType:$t, category:$c, amount:$a, currency:"VND", timing:"ONE_TIME", effectiveDate:$d, notes:$n} + $x')" | jq -r .id; }
# every TYPE CATEGORY AMOUNT RECURRENCE START NOTES [extra]
every() { api POST /cash-flow-entries "$(jq -n --arg t "$1" --arg c "$2" --arg a "$3" --arg r "$4" --arg d "$5" --arg n "$6" --argjson x "${7:-"{}"}" \
  '{entryType:$t, category:$c, amount:$a, currency:"VND", timing:"RECURRING", recurrence:$r, effectiveDate:$d, notes:$n} + $x')" | jq -r .id; }
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
card() { liab '{"name":"Thẻ tín dụng Techcombank","category":"CREDIT_CARD_BALANCE","outstandingBalance":"32000000","currency":"VND","maturityDate":"2026-11-30"}'; }

# B and P share the design #81 baseline; P declares no rate.
baseline() { # baseline KEY
  local k=$1 cash usd term stocks rental fund loan
  cash=$(asset "Tiền mặt dự phòng" CASH 148000000 VND 2026-11-25 HIGH)
  usd=$(asset "Tài khoản USD HSBC" BANK_ACCOUNT 20000 USD 2026-11-24 HIGH)
  term=$(asset "Tiền gửi kỳ hạn 12 tháng" TERM_DEPOSIT 1200000000 VND 2026-11-21 MEDIUM)
  stocks=$(asset "Danh mục cổ phiếu HOSE" LISTED_SECURITIES 2150000000 VND 2026-11-26 HIGH)
  rental=$(asset "Căn hộ cho thuê Quận 2" REAL_ESTATE 4300000000 VND 2026-10-07 LOW)
  fund=$(asset "Quỹ trái phiếu An Tâm" INVESTMENT_FUND 300000000 VND 2026-11-20 MEDIUM)
  loan=$(mortgage); save "${k}_CARD" "$(card)"
  save "${k}_CASH" "$cash"; save "${k}_USD" "$usd"; save "${k}_TERM" "$term"; save "${k}_LOAN" "$loan"
  save "${k}_SALARY" "$(every INCOME SALARY 132000000 MONTHLY 2026-01-05 'Lương tháng')"
  save "${k}_BUSINESS" "$(once INCOME BUSINESS_INCOME 26200000 2026-11-12 'Thu nhập kinh doanh')"
  save "${k}_RENT" "$(every INCOME RENTAL_INCOME 14800000 MONTHLY 2026-01-10 'Thu nhập cho thuê' "$(asset_link "$rental")")"
  save "${k}_DIVIDEND" "$(api POST /cash-flow-entries "$(jq -n --arg id "$stocks" '{entryType:"INCOME", category:"DIVIDEND", amount:"150", currency:"USD", timing:"RECURRING", recurrence:"QUARTERLY", effectiveDate:"2026-11-15", linkedAssetId:$id, notes:"Cổ tức 150 USD"}')" | jq -r .id)"
  save "${k}_INTEREST_IN" "$(every INCOME DEPOSIT_INTEREST 1500000 MONTHLY 2026-01-25 'Lãi tiền gửi' "$(asset_link "$term")")"
  save "${k}_BOND" "$(every INCOME BOND_OR_FUND_INCOME 1500000 QUARTERLY 2026-11-20 'Thu nhập quỹ trái phiếu' "$(asset_link "$fund")")"
  save "${k}_OTHER_IN" "$(once INCOME OTHER_INCOME 235000 2026-11-18 'Thu nhập khác')"
  save "${k}_LIVING" "$(every EXPENSE LIVING_COSTS 23400000 MONTHLY 2026-01-03 'Chi phí sinh hoạt')"
  save "${k}_HOUSING" "$(every EXPENSE HOUSING 18750000 MONTHLY 2026-01-20 'Nhà ở' "$(liab_link "$loan")")"
  save "${k}_EDU" "$(once EXPENSE EDUCATION 12800000 2026-11-08 'Học phí')"
  save "${k}_HEALTH" "$(once EXPENSE HEALTHCARE 4650000 2026-11-14 'Khám sức khỏe')"
  save "${k}_INSURANCE" "$(every EXPENSE INSURANCE 5400000 YEARLY 2025-11-16 'Bảo hiểm sức khỏe gia đình')"
  save "${k}_TAX" "$(every EXPENSE TAX_AND_FEES 8250000 QUARTERLY 2026-11-22 'Thuế và phí')"
  save "${k}_PRINCIPAL" "$(every EXPENSE DEBT_PRINCIPAL_PAYMENT 10000000 MONTHLY 2026-01-20 'Trả gốc vay căn hộ' "$(liab_link "$loan")")"
  save "${k}_INTEREST" "$(every EXPENSE INTEREST_PAYMENT 6250000 MONTHLY 2026-01-20 'Trả lãi vay căn hộ' "$(liab_link "$loan")")"
  save "${k}_OTHER_EX" "$(once EXPENSE OTHER_EXPENSE 2500000 2026-11-24 'Chi phí khác')"
}

user B fh.base@example.com "An Nguyễn"; rate; baseline B
user P fh.partial@example.com "Phúc Lê"; baseline P

user W fh.watch@example.com "Vy Trần"; rate
save W_CASH "$(asset "Tiền mặt" CASH 40000000 VND 2026-11-26 HIGH)"
save W_TERM "$(asset "Tiền gửi kỳ hạn 6 tháng" TERM_DEPOSIT 160000000 VND 2026-11-20 MEDIUM)"
W_FLAT=$(asset "Căn hộ dịch vụ Bình Thạnh" REAL_ESTATE 2800000000 VND 2026-10-15 LOW)
W_LOAN=$(liab '{"name":"Vay mua nhà BIDV","category":"BANK_LOAN","outstandingBalance":"900000000","currency":"VND","maturityDate":"2036-06-20","recurringPaymentAmount":"16250000","recurringPaymentFrequency":"MONTHLY","nextPaymentDate":"2026-12-20"}')
save W_LOAN "$W_LOAN"
save W_SALARY "$(every INCOME SALARY 45000000 MONTHLY 2026-01-01 'Lương tháng')"
save W_RENT "$(every INCOME RENTAL_INCOME 12000000 MONTHLY 2026-01-01 'Thu nhập cho thuê' "$(asset_link "$W_FLAT")")"
save W_LIVING "$(every EXPENSE LIVING_COSTS 26150000 MONTHLY 2026-01-03 'Chi phí sinh hoạt')"
save W_PRINCIPAL "$(every EXPENSE DEBT_PRINCIPAL_PAYMENT 10000000 MONTHLY 2026-01-20 'Trả gốc vay BIDV' "$(liab_link "$W_LOAN")")"
save W_INTEREST "$(every EXPENSE INTEREST_PAYMENT 6250000 MONTHLY 2026-01-20 'Trả lãi vay BIDV' "$(liab_link "$W_LOAN")")"
save W_EDU "$(once EXPENSE EDUCATION 14000000 2026-11-08 'Học phí học kỳ')"

user S fh.stress@example.com "Sơn Phạm"; rate
save S_CASH "$(asset "Tiền mặt" CASH 12000000 VND 2026-11-26 HIGH)"
save S_BANK "$(asset "Tài khoản thanh toán" BANK_ACCOUNT 30000000 VND 2026-11-26 HIGH)"
S_RENTAL=$(asset "Căn hộ cho thuê Quận 2" REAL_ESTATE 4300000000 VND 2026-10-07 LOW)
S_LOAN=$(mortgage); save S_LOAN "$S_LOAN"; save S_CARD "$(card)"
save S_SALARY "$(every INCOME SALARY 132000000 MONTHLY 2026-01-05 'Lương tháng, kết thúc khi đổi việc' '{"endDate":"2026-10-31"}')"
save S_RENT "$(every INCOME RENTAL_INCOME 14800000 MONTHLY 2026-01-10 'Thu nhập cho thuê' "$(asset_link "$S_RENTAL")")"
save S_LIVING "$(every EXPENSE LIVING_COSTS 23400000 MONTHLY 2026-01-03 'Chi phí sinh hoạt')"
save S_HOUSING "$(every EXPENSE HOUSING 18750000 MONTHLY 2026-01-20 'Nhà ở' "$(liab_link "$S_LOAN")")"
save S_INSURANCE "$(every EXPENSE INSURANCE 5400000 YEARLY 2025-11-16 'Bảo hiểm sức khỏe gia đình')"
save S_TAX "$(every EXPENSE TAX_AND_FEES 8250000 QUARTERLY 2026-11-22 'Thuế và phí')"
save S_PRINCIPAL "$(every EXPENSE DEBT_PRINCIPAL_PAYMENT 10000000 MONTHLY 2026-01-20 'Trả gốc vay căn hộ' "$(liab_link "$S_LOAN")")"
save S_INTEREST "$(every EXPENSE INTEREST_PAYMENT 6250000 MONTHLY 2026-01-20 'Trả lãi vay căn hộ' "$(liab_link "$S_LOAN")")"

user Z fh.zero@example.com "Zung Hoàng"
save Z_CASH "$(asset "Tiền mặt" CASH 50000000 VND 2026-11-26 HIGH)"
save Z_LIVING "$(once EXPENSE LIVING_COSTS 23400000 2026-11-10 'Chi phí sinh hoạt tháng 11')"
save Z_EDU "$(once EXPENSE EDUCATION 12800000 2026-11-15 'Học phí')"
save Z_OTHER "$(once EXPENSE OTHER_EXPENSE 2500000 2026-11-20 'Chi phí khác')"

user I fh.income@example.com "Ích Đặng"
save I_SALARY "$(once INCOME SALARY 30000000 2026-11-05 'Lương thử việc')"

user E fh.empty@example.com "Em Võ"

# U: a recurring essential expense in USD and no declared rate.
user U fh.usd-expense@example.com "Uyên Ngô"
save U_CASH "$(asset "Tiền mặt" CASH 100000000 VND 2026-11-26 HIGH)"
save U_LIVING "$(every EXPENSE LIVING_COSTS 23400000 MONTHLY 2026-01-03 'Chi phí sinh hoạt')"
save U_INSURANCE "$(api POST /cash-flow-entries '{"entryType":"EXPENSE","category":"INSURANCE","amount":"200","currency":"USD","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-01-16","notes":"Phí bảo hiểm sức khỏe quốc tế 200 USD"}' | jq -r .id)"

# L: a liability paid in USD inside the obligation window and no declared rate.
user L fh.usd-loan@example.com "Lâm Bùi"
save L_CASH "$(asset "Tiền mặt" CASH 80000000 VND 2026-11-26 HIGH)"
save L_LOAN "$(liab '{"name":"Vay mua xe bằng USD","category":"BANK_LOAN","outstandingBalance":"12000","currency":"USD","startDate":"2025-12-10","maturityDate":"2028-12-10","recurringPaymentAmount":"500","recurringPaymentFrequency":"MONTHLY","nextPaymentDate":"2026-12-10"}')"

echo "seeded $(grep -c . "$OUT/env.sh") ids into $OUT/env.sh"
