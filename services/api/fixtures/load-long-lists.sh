#!/usr/bin/env bash
# Loads long lists into one development account, through the public API, for checking scrolling,
# wrapping, truncation and order on phone and desktop layouts.
#
#   ./load-long-lists.sh [email]            default email: long.list@example.com
#   API_BASE_URL=http://localhost:8107 ./load-long-lists.sh
#
# What it loads:
#   60 assets across all eleven categories, VND and USD, with long Vietnamese names (one exactly 120
#      characters, the maximum), names that differ only by diacritics, and two pairs of identical names
#      on the same valuation date, so the id tie-break is visible
#   24 valuation records on "Danh mục chứng chỉ quỹ mở … lịch sử 24 tháng"
#   40 liabilities across all six categories: overdue, due within 30 days, later and no payment date
#   120 cash-flow entries: one-time and recurring, income and expense, VND and USD, with and without notes
#   15 goals across all seven types
#   20 feedback entries (feedback has no delete route, so only the missing ones are added)
#
# Idempotent: the account's assets, liabilities, cash-flow entries and goals are removed first.
set -euo pipefail
# shellcheck source=lib.sh
source "$(cd "$(dirname "$0")" && pwd)/lib.sh"

EMAIL="${1:-long.list@example.com}"

fixture_init
fixture_sign_in "$EMAIL" "${FIXTURE_DISPLAY_NAME:-Lê Thị Danh Sách Dài}"
delete_all_records

post_id() { api POST "$1" "$2" | jq -r .id; }

api PUT /settings/exchange-rate '{"usdToVndRate": 25100.000000, "effectiveDate": "2026-09-12"}' > /dev/null

# ---- Assets -------------------------------------------------------------------------------------------
ASSET_CATEGORIES=(CASH BANK_ACCOUNT TERM_DEPOSIT LISTED_SECURITIES BOND INVESTMENT_FUND REAL_ESTATE
  PRIVATE_BUSINESS_EQUITY VEHICLE_OR_VALUABLE_ASSET COLLECTIBLE OTHER_ASSET)
ASSET_NAMES=(
  "Tiền mặt dự phòng để ở két sắt gia đình"
  "Tài khoản thanh toán Ngân hàng TMCP Ngoại thương Việt Nam chi nhánh Thủ Đức"
  "Sổ tiết kiệm kỳ hạn 13 tháng lãi cuối kỳ tại Ngân hàng Nông nghiệp và Phát triển Nông thôn"
  "Danh mục cổ phiếu ngành ngân hàng và bất động sản niêm yết trên sàn HOSE"
  "Trái phiếu Chính phủ Việt Nam kỳ hạn 10 năm phát hành đợt tháng Ba"
  "Chứng chỉ quỹ mở cổ phiếu tăng trưởng Việt Nam do công ty quản lý quỹ nội địa phát hành"
  "Nhà phố ba tầng mặt tiền đường Nguyễn Thị Minh Khai, Phường Đa Kao, Quận 1"
  "Vốn góp vào Công ty TNHH Thương mại và Dịch vụ Mộc Việt Phương Nam"
  "Xe ô tô Toyota Corolla Cross bản hybrid đăng ký tại Thành phố Hồ Chí Minh"
  "Bộ sưu tập tranh sơn mài và gốm men lam Chu Đậu của gia đình"
  "Khoản đặt cọc thuê mặt bằng kinh doanh cà phê tại Phường Bến Thành"
)
INSTITUTIONS=("Vietcombank" "Agribank" "Chứng khoán SSI" "Kho bạc Nhà nước" "Quỹ VinaCapital" "")
# Exactly 120 characters: the maximum the API accepts for a name.
NAME_120="Căn hộ chung cư cao cấp tầng mười tám, tòa Sapphire hai A, khu đô thị Vinhomes Grand Park, Phường Long Thạnh Mỹ, Thủ Đức"
HISTORY_ASSET_NAME="Danh mục chứng chỉ quỹ mở cân bằng – lịch sử 24 tháng"

asset_body() { # NAME CATEGORY VALUE CURRENCY DATE LIQUIDITY INSTITUTION
  jq -n --arg name "$1" --arg category "$2" --argjson value "$3" --arg currency "$4" --arg date "$5" \
    --arg liquidity "$6" --arg institution "$7" '
    {name: $name, category: $category, currentValue: $value, currency: $currency, valuationDate: $date,
     liquidity: $liquidity}
    + (if $institution == "" then {} else {institution: $institution} end)
    + (if $currency == "USD" then {usdToVndRate: 25100.000000, rateEffectiveDate: "2026-09-12"} else {} end)'
}

LIQUIDITY=(HIGH MEDIUM LOW)
for i in $(seq 0 50); do
  category="${ASSET_CATEGORIES[i % 11]}"
  name="${ASSET_NAMES[i % 11]} – hồ sơ số $((i + 1))"
  currency=VND
  value=$((37500000 + i * 186250000 + (i % 7) * 1375000))
  if ((i % 9 == 4)); then currency=USD; value=$((1250 + i * 731)); fi
  post_id /assets "$(asset_body "$name" "$category" "$value" "$currency" "$(day $((-(i * 7) % 400)))" \
    "${LIQUIDITY[i % 3]}" "${INSTITUTIONS[i % 6]}")" > /dev/null
done
# Names that sort differently with and without diacritics, the 120-character name, and two pairs of
# identical names on the same date (their order is decided by id alone).
post_id /assets "$(asset_body "Đất nền Long An" REAL_ESTATE 1850000000 VND "$(day -15)" LOW "")" > /dev/null
post_id /assets "$(asset_body "Dat nen Long An khong dau" REAL_ESTATE 1720000000 VND "$(day -15)" LOW "")" > /dev/null
post_id /assets "$(asset_body "Ô tô Kia Morning" VEHICLE_OR_VALUABLE_ASSET 315000000 VND "$(day -9)" MEDIUM "")" > /dev/null
post_id /assets "$(asset_body "$NAME_120" REAL_ESTATE 6320000000 VND "$(day -4)" LOW "Vinhomes")" > /dev/null
for _ in 1 2; do
  post_id /assets "$(asset_body "Vàng miếng SJC 1 lượng" VEHICLE_OR_VALUABLE_ASSET 84500000 VND "$(day -5)" HIGH "SJC")" > /dev/null
  post_id /assets "$(asset_body "Tiền mặt" CASH 12000000 VND "$(day -5)" HIGH "")" > /dev/null
done
# One asset with 24 valuation records: created 720 days ago, then one record every 30 days.
HISTORY_ASSET="$(post_id /assets "$(asset_body "$HISTORY_ASSET_NAME" INVESTMENT_FUND 250000000 VND "$(day -720)" HIGH "Quỹ Dragon Capital")")"
for k in $(seq 1 23); do
  api POST "/assets/$HISTORY_ASSET/valuations" "$(jq -n --argjson value $((250000000 + k * 4375000 - (k % 4) * 2150000)) \
    --arg date "$(day $((-720 + k * 30)))" '{value: $value, currency: "VND", effectiveDate: $date,
      note: "Cập nhật theo giá chứng chỉ quỹ cuối tháng"}')" > /dev/null
done

# ---- Liabilities --------------------------------------------------------------------------------------
LIABILITY_CATEGORIES=(BANK_LOAN PERSONAL_LOAN MARGIN_LOAN CREDIT_CARD_BALANCE GUARANTEE_OBLIGATION OTHER_LIABILITY)
LIABILITY_NAMES=(
  "Vay mua nhà Ngân hàng TMCP Đầu tư và Phát triển Việt Nam, lãi suất thả nổi sau năm thứ ba"
  "Khoản mượn anh Hai để sửa nhà ở quê, trả dần khi có thưởng Tết"
  "Vay ký quỹ Chứng khoán VNDirect cho danh mục cổ phiếu ngành thép"
  "Dư nợ thẻ tín dụng Techcombank Visa Signature"
  "Bảo lãnh khoản vay kinh doanh của em gái tại Sacombank"
  "Tiền hụi tháng còn phải đóng cho dây hụi của chợ Bến Thành"
)
FREQUENCIES=(MONTHLY QUARTERLY SEMIANNUAL YEARLY)
for i in $(seq 0 39); do
  next=""
  case $((i % 5)) in
    0) next="$(day $((-3 - i)))" ;;  # overdue
    1) next="$(day $((2 + i % 25)))" ;; # due within 30 days
    2) next="$(day $((45 + i * 3)))" ;; # later
    *) next="" ;;                    # no payment date
  esac
  currency=VND
  balance=$((8500000 + i * 61250000))
  if ((i % 8 == 3)); then currency=USD; balance=$((900 + i * 215)); fi
  post_id /liabilities "$(jq -n --arg name "${LIABILITY_NAMES[i % 6]} – hợp đồng $((i + 1))" \
    --arg category "${LIABILITY_CATEGORIES[i % 6]}" --argjson balance "$balance" --arg currency "$currency" \
    --arg next "$next" --arg frequency "${FREQUENCIES[i % 4]}" --argjson recurring $((i % 2)) '
    {name: $name, category: $category, outstandingBalance: $balance, currency: $currency}
    + (if $next == "" then {} else {nextPaymentDate: $next} end)
    + (if $recurring == 1 then {recurringPaymentAmount: (($balance / 24) | floor), recurringPaymentFrequency: $frequency} else {} end)')" > /dev/null
done

# ---- Cash-flow entries --------------------------------------------------------------------------------
INCOME_CATEGORIES=(SALARY BUSINESS_INCOME RENTAL_INCOME DIVIDEND DEPOSIT_INTEREST BOND_OR_FUND_INCOME OTHER_INCOME)
EXPENSE_CATEGORIES=(LIVING_COSTS HOUSING EDUCATION HEALTHCARE INSURANCE TAX_AND_FEES DEBT_PRINCIPAL_PAYMENT
  INTEREST_PAYMENT OTHER_EXPENSE)
ENTRY_NOTES=(
  "Lương tháng và phụ cấp trách nhiệm từ Công ty Cổ phần Công nghệ Phương Nam"
  "Tiền thuê căn hộ cho khách nước ngoài thuê dài hạn, đã trừ phí quản lý tòa nhà"
  "Học phí học kỳ một và tiền xe đưa đón của hai con tại trường song ngữ"
  "Phí bảo hiểm sức khỏe cao cấp cho cả gia đình bốn người, đóng theo năm"
  "Chi tiêu đi chợ, điện nước, internet và gửi tiền về quê cho ông bà"
  ""
)
RECURRENCES=(MONTHLY QUARTERLY YEARLY)
for i in $(seq 0 119); do
  if ((i % 3 == 0)); then
    type=INCOME; category="${INCOME_CATEGORIES[i % 7]}"
  else
    type=EXPENSE; category="${EXPENSE_CATEGORIES[i % 9]}"
  fi
  currency=VND
  amount=$((1250000 + i * 2765000 + (i % 11) * 137000))
  if ((i % 10 == 7)); then currency=USD; amount=$((120 + i * 13)); fi
  post_id /cash-flow-entries "$(jq -n --arg type "$type" --arg category "$category" --argjson amount "$amount" \
    --arg currency "$currency" --arg date "$(day $((-(i * 3) % 365 + (i % 6 == 5 ? 40 : 0))))" \
    --arg notes "${ENTRY_NOTES[i % 6]}" --argjson recurring $((i % 2)) --arg recurrence "${RECURRENCES[i % 3]}" '
    {entryType: $type, category: $category, amount: $amount, currency: $currency, effectiveDate: $date}
    + (if $recurring == 1 then {timing: "RECURRING", recurrence: $recurrence} else {timing: "ONE_TIME"} end)
    + (if $notes == "" then {} else {notes: $notes} end)')" > /dev/null
done

# ---- Goals --------------------------------------------------------------------------------------------
GOAL_TYPES=(EMERGENCY_FUND HOME_PURCHASE EDUCATION RETIREMENT NET_WORTH_GROWTH DEBT_REDUCTION OTHER_GOAL)
GOAL_NAMES=(
  "Quỹ dự phòng sáu tháng chi tiêu của cả gia đình"
  "Mua căn hộ ba phòng ngủ gần trường của các con ở Thành phố Thủ Đức"
  "Quỹ du học thạc sĩ ngành kỹ thuật tại Úc cho con gái lớn"
  "Nghỉ hưu sớm ở tuổi năm mươi lăm tại Đà Lạt"
  "Tăng tài sản ròng lên hai mươi tỷ đồng"
  "Trả hết khoản vay mua nhà trước hạn"
  "Chuyến du lịch Nhật Bản mùa hoa anh đào cho cả nhà"
)
for i in $(seq 0 14); do
  target=$((350000000 + i * 1137500000))
  post_id /goals "$(jq -n --arg name "${GOAL_NAMES[i % 7]} – kế hoạch $((i + 1))" --arg type "${GOAL_TYPES[i % 7]}" \
    --argjson target "$target" --argjson allocated $((target * (i % 5) / 10)) --arg date "$(day $((60 + i * 137)))" \
    --argjson contribution $(((i % 4) * 7500000)) --argjson ret $((i % 4 * 2)) '
    {name: $name, type: $type, targetAmount: $target, currentAllocatedAmount: $allocated, targetDate: $date,
     plannedMonthlyContribution: $contribution, expectedAnnualReturnPercent: $ret}')" > /dev/null
done

# ---- Feedback (no delete route: add only the missing ones) --------------------------------------------
SCREENS=(dashboard catalog/assets catalog/liabilities cash-flow goals onboarding settings/exchange-rate)
FEEDBACK_CATEGORIES=(BUG IDEA QUESTION OTHER)
existing="$(count /feedback)"
for ((i = existing; i < 20; i++)); do
  api POST /feedback "$(jq -n --arg screen "${SCREENS[i % 7]}" --arg category "${FEEDBACK_CATEGORIES[i % 4]}" \
    --arg message "Góp ý số $((i + 1)): trên điện thoại màn hình này phải cuộn khá lâu mới thấy nút lưu, mong được đưa nút lên cao hơn hoặc cố định ở cuối màn hình." \
    '{screen: $screen, category: $category, message: $message}')" > /dev/null
done

api PATCH /onboarding '{"currentStep": 7, "dashboardReviewed": true, "status": "COMPLETED"}' > /dev/null

printf 'Loaded long lists for %s: assets %s, valuations of "%s" %s, liabilities %s, cash-flow entries %s, goals %s, feedback %s.\n' \
  "$EMAIL" "$(count /assets)" "$HISTORY_ASSET_NAME" "$(count "/assets/$HISTORY_ASSET/valuations")" \
  "$(count /liabilities)" "$(count /cash-flow-entries)" "$(count /goals)" "$(count /feedback)"
