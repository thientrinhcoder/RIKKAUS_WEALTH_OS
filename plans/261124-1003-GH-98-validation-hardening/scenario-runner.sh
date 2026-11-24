#!/bin/bash
# Runs the negative-input matrix of phase-01-test-scenarios.md against a running API.
#
#   OUT=<dir> LOG=<api log file> BASE=http://localhost:8098/api/v1 ./scenario-runner.sh
#
# Needs the `local` profile (POST /auth/dev-sign-in) and an empty database. Writes results.tsv (one
# line per case) and evidence.md (status and body of every case) into OUT. Tokens stay in shell
# variables and are never written out. Written for macOS bash 3.2.
set -u

BASE=${BASE:-http://localhost:8098/api/v1}
OUT=${OUT:?set OUT to an output directory}
LOG=${LOG:-}
mkdir -p "$OUT"
RESULTS="$OUT/results.tsv"
EVIDENCE="$OUT/evidence.md"
: > "$RESULTS"
: > "$EVIDENCE"

TODAY=$(TZ=Asia/Ho_Chi_Minh date +%F)
TOMORROW=$(TZ=Asia/Ho_Chi_Minh date -v+1d +%F)
THIS_MONTH=$(TZ=Asia/Ho_Chi_Minh date +%Y-%m)
RANDOM_ID=0f0e0d0c-0b0a-4909-8807-060504030201

STATUS=""
BODY=""

# call ID METHOD PATH TOKEN BODY [extra curl arguments]. BODY "-" sends no body.
call() {
    local id=$1 method=$2 path=$3 token=$4 body=$5
    shift 5
    local args=(-s -o "$OUT/$id.body" -w '%{http_code}' -X "$method" "$BASE$path")
    if [ -n "$token" ]; then args+=(-H "Authorization: Bearer $token"); fi
    if [ "$body" != "-" ]; then args+=(-H 'Content-Type: application/json' --data-binary "$body"); fi
    if [ $# -gt 0 ]; then args+=("$@"); fi
    STATUS=$(curl "${args[@]}")
    BODY=$(cat "$OUT/$id.body")
}

# check ID DESCRIPTION EXPECTED_STATUS [JQ_FILTER that must be true]
check() {
    local id=$1 desc=$2 expected=$3 filter=${4:-}
    local verdict=PASS
    if [ "$STATUS" != "$expected" ]; then verdict=FAIL; fi
    if [ -n "$filter" ] && ! printf '%s' "$BODY" | jq -e "$filter" > /dev/null 2>&1; then verdict=FAIL; fi
    case "$STATUS" in
        2*) ;;
        *)
            if printf '%s' "$BODY" | grep -qiE 'com\.rikkaus|exception|sql|jdbc|hibernate|tools\.jackson'; then
                verdict=FAIL
            fi
            ;;
    esac
    printf '%s\t%s\t%s\t%s\t%s\n' "$id" "$verdict" "$STATUS" "$expected" "$desc" >> "$RESULTS"
    {
        printf '#### %s — %s\n\n%s → **%s** (mong đợi %s)\n\n```json\n' "$id" "$verdict" "$desc" "$STATUS" "$expected"
        printf '%s' "$BODY" | jq . 2> /dev/null || printf '%s\n' "$BODY"
        printf '```\n\n'
    } >> "$EVIDENCE"
}

# Records a check computed in the script rather than from one response.
verdict() {
    local id=$1 desc=$2 ok=$3 detail=$4
    local v=FAIL
    if [ "$ok" = 1 ]; then v=PASS; fi
    printf '%s\t%s\t-\t-\t%s\n' "$id" "$v" "$desc" >> "$RESULTS"
    printf '#### %s — %s\n\n%s\n\n%s\n\n' "$id" "$v" "$desc" "$detail" >> "$EVIDENCE"
}

json() { jq -n --arg v "$1" '$v'; }
MR='.type=="urn:rikkaus:problem:malformed-request" and (has("errors")|not)'
vf() {
    printf '.type=="urn:rikkaus:problem:validation-failed" and (.errors|any(.field==%s and .message==%s))' \
        "$(json "$1")" "$(json "$2")"
}
# instance is the request path by RFC 9457's definition, so a path segment there is not an echo.
noecho() { printf '(del(.instance)|tostring|contains(%s)|not)' "$(json "$1")"; }
same_after_drop() { # FILE_A FILE_B -> 1 when equal after dropping instance and correlationId
    local a b
    a=$(jq -S -c 'del(.instance, .correlationId)' "$1")
    b=$(jq -S -c 'del(.instance, .correlationId)' "$2")
    if [ "$a" = "$b" ]; then echo 1; else echo 0; fi
}
repeat() { local s="" i; for ((i = 0; i < $2; i++)); do s="$s$1"; done; printf '%s' "$s"; }

sign_in() {
    curl -s -X POST "$BASE/auth/dev-sign-in" -H 'Content-Type: application/json' \
        --data-binary "{\"email\":\"$1\",\"displayName\":\"$2\"}" | jq -r .accessToken
}
TA=$(sign_in an.nguyen@example.com 'An Nguyễn')
TB=$(sign_in binh.tran@example.com 'Bình Trần')

# ---------------------------------------------------------------- H — data and smoke
call H01 POST /assets "$TA" '{"name":"Căn hộ Thảo Điền","category":"REAL_ESTATE","currentValue":4200000000,"currency":"VND","valuationDate":"2026-11-15","liquidity":"LOW","purchaseValue":3100000000,"purchaseDate":"2021-06-15"}'
check H01 'POST /assets Căn hộ Thảo Điền 4.200.000.000 VND' 201 '.id != null'
ASV=$(printf '%s' "$BODY" | jq -r .id)
call H02 POST /assets "$TA" '{"name":"Tài khoản USD Vietcombank","category":"BANK_ACCOUNT","currentValue":21250.00,"currency":"USD","valuationDate":"2026-09-14","usdToVndRate":25100,"rateEffectiveDate":"2026-09-12","liquidity":"HIGH"}'
check H02 'POST /assets Tài khoản USD Vietcombank 21.250 USD' 201 '.id != null'
ASU=$(printf '%s' "$BODY" | jq -r .id)
call H03 POST /liabilities "$TA" "{\"name\":\"Vay mua nhà Vietcombank\",\"category\":\"BANK_LOAN\",\"outstandingBalance\":2400000000,\"currency\":\"VND\",\"interestRate\":8.5,\"startDate\":\"2021-06-15\",\"maturityDate\":\"2041-06-15\",\"recurringPaymentAmount\":28500000,\"recurringPaymentFrequency\":\"MONTHLY\",\"nextPaymentDate\":\"2026-12-05\",\"collateralAssetId\":\"$ASV\"}"
check H03 'POST /liabilities Vay mua nhà Vietcombank' 201 '.id != null'
LB1=$(printf '%s' "$BODY" | jq -r .id)
call H04 POST /cash-flow-entries "$TA" '{"entryType":"INCOME","category":"SALARY","amount":45000000,"currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-01-05","notes":"Lương tháng"}'
check H04 'POST /cash-flow-entries lương 45.000.000 VND/tháng' 201 '.id != null'
CF1=$(printf '%s' "$BODY" | jq -r .id)
call H05 POST /goals "$TA" '{"name":"Quỹ dự phòng 6 tháng","type":"EMERGENCY_FUND","targetAmount":270000000,"currentAllocatedAmount":85000000,"targetDate":"2027-12-31","plannedMonthlyContribution":12000000,"expectedAnnualReturnPercent":4.5}'
check H05 'POST /goals Quỹ dự phòng 6 tháng' 201 '.id != null'
G1=$(printf '%s' "$BODY" | jq -r .id)
call H06 PUT /settings/exchange-rate "$TA" '{"usdToVndRate":25100,"effectiveDate":"2026-11-20"}'
check H06 'PUT /settings/exchange-rate 25.100' 200 '.usdToVndRate == 25100'
call H07 POST "/assets/$ASU/valuations" "$TA" "{\"value\":21400.00,\"currency\":\"USD\",\"effectiveDate\":\"$TODAY\",\"usdToVndRate\":25100,\"rateEffectiveDate\":\"2026-11-20\",\"note\":\"Cập nhật số dư sau khi đối chiếu sao kê.\"}"
check H07 'POST valuation 21.400 USD hôm nay' 201 '.current == true'
call H08 PUT "/assets/$ASV" "$TA" '{"name":"Căn hộ Thảo Điền","liquidity":"LOW","purchaseValue":3100000000,"purchaseDate":"2021-06-15","notes":"Sổ hồng đứng tên hai vợ chồng"}'
check H08 'PUT /assets đổi ghi chú' 200 '.notes == "Sổ hồng đứng tên hai vợ chồng"'
call H09 PUT "/liabilities/$LB1" "$TA" "{\"name\":\"Vay mua nhà Vietcombank\",\"outstandingBalance\":2380000000,\"currency\":\"VND\",\"interestRate\":8.5,\"startDate\":\"2021-06-15\",\"maturityDate\":\"2041-06-15\",\"recurringPaymentAmount\":28500000,\"recurringPaymentFrequency\":\"MONTHLY\",\"nextPaymentDate\":\"2026-12-05\",\"collateralAssetId\":\"$ASV\"}"
check H09 'PUT /liabilities dư nợ 2.380.000.000' 200 '.outstandingBalance == 2380000000'
call H10 PUT "/cash-flow-entries/$CF1" "$TA" '{"entryType":"INCOME","category":"SALARY","amount":46000000,"currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-01-05","notes":"Lương tháng"}'
check H10 'PUT /cash-flow-entries 46.000.000' 200 '.amount == 46000000'
call H11 PUT "/goals/$G1" "$TA" '{"name":"Quỹ dự phòng 6 tháng","type":"EMERGENCY_FUND","targetAmount":270000000,"currentAllocatedAmount":85000000,"targetDate":"2027-12-31","plannedMonthlyContribution":13000000,"expectedAnnualReturnPercent":4.5}'
check H11 'PUT /goals góp 13.000.000' 200 '.plannedMonthlyContribution == 13000000'
call H12 PUT /profile "$TA" '{"displayName":"  Trinh Nguyễn  "}'
check H12 'PUT /profile "  Trinh Nguyễn  "' 200 '.displayName == "Trinh Nguyễn"'
call H13 PATCH /onboarding "$TA" '{"currentStep":3,"dashboardReviewed":true}'
check H13 'PATCH /onboarding bước 3' 200 '.currentStep == 3'
call H14 POST /feedback "$TA" '{"screen":"catalog/assets","category":"BUG","message":"Không thấy nút lưu trên điện thoại"}'
check H14 'POST /feedback' 201 '.id != null'
call H15 GET /wealth-snapshot "$TA" -
check H15 'GET /wealth-snapshot' 200 '.complete == true'
call H16 GET '/cash-flow-occurrences?from=2026-12-01&to=2026-12-31' "$TA" -
check H16 'GET /cash-flow-occurrences tháng 12/2026' 200 'length == 1'
call H17 GET '/cash-flow-reports/monthly?month=2026-11' "$TA" -
check H17 'GET /cash-flow-reports/monthly 2026-11' 200 '.totals.income == 46000000'
call H18 GET /cash-flow-forecast "$TA" -
check H18 'GET /cash-flow-forecast' 200 '(.months|length) == 3'
call H19 GET /goal-projections "$TA" -
check H19 'GET /goal-projections' 200 'length == 1'
call H20 GET "/assets/$ASV/deletion-impact" "$TA" -
check H20 'GET /assets/ASV/deletion-impact' 200 '.confirmationRequired == true'
call H21a POST /cash-flow-entries "$TA" '{"entryType":"EXPENSE","category":"EDUCATION","amount":18500000,"currency":"VND","timing":"ONE_TIME","effectiveDate":"2026-11-10","notes":"Học phí học kỳ 1"}'
CFX=$(printf '%s' "$BODY" | jq -r .id)
call H21 DELETE "/cash-flow-entries/$CFX" "$TA" -
check H21 'DELETE khoản học phí một lần' 204

# Records of B, for the ownership cases.
call SB1 POST /assets "$TB" '{"name":"Nhà phố Thảo Điền","category":"REAL_ESTATE","currentValue":9500000000,"currency":"VND","valuationDate":"2026-10-01","liquidity":"LOW"}'
ASB=$(printf '%s' "$BODY" | jq -r .id)
call SB2 POST /liabilities "$TB" '{"name":"Vay kinh doanh ACB","category":"BANK_LOAN","outstandingBalance":1200000000,"currency":"VND"}'
LBB=$(printf '%s' "$BODY" | jq -r .id)
call SB3 POST /cash-flow-entries "$TB" '{"entryType":"INCOME","category":"RENTAL_INCOME","amount":35000000,"currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-02-01"}'
CFB=$(printf '%s' "$BODY" | jq -r .id)
call SB4 POST /goals "$TB" '{"name":"Mua nhà Quận 7","type":"HOME_PURCHASE","targetAmount":6500000000,"targetDate":"2030-12-31"}'
GB=$(printf '%s' "$BODY" | jq -r .id)
call SB5 GET "/assets/$ASU/valuations" "$TA" -
ASU_FIRST=$(printf '%s' "$BODY" | jq -r '.[-1].id')

# ---------------------------------------------------------------- X — protocol and body shape
LIA='{"name":"Thẻ tín dụng Techcombank Visa","category":"CREDIT_CARD_BALANCE","outstandingBalance":18700000,"currency":"VND"}'
call X01 POST /liabilities "$TA" ''
check X01 'body rỗng' 400 "$MR"
call X02 POST /liabilities "$TA" 'null'
check X02 'body null' 400 "$MR"
call X03 POST /liabilities "$TA" '[]'
check X03 'body []' 400 "$MR"
call X04 POST /liabilities "$TA" '{"name":'
check X04 'JSON cụt' 400 "$MR"
call X05 POST /liabilities "$TA" - -H 'Content-Type: text/plain' --data-binary "$LIA"
check X05 'Content-Type text/plain' 415 '.type=="urn:rikkaus:problem:unsupported-media-type"'
call X06 POST /liabilities "$TA" '{"name":"Thẻ tín dụng","category":"CREDIT_CARD_BALANCE","outstandingBalance":"abc","currency":"VND"}'
check X06 'outstandingBalance "abc"' 400 "$MR and $(noecho abc)"
call X07 POST /liabilities "$TA" '{"name":"Thẻ tín dụng","category":"CREDIT_CARD_BALANCE","outstandingBalance":true,"currency":"VND"}'
check X07 'outstandingBalance true' 400 "$MR"
call X08 POST /liabilities "$TA" '{"name":"Thẻ tín dụng","category":"CREDIT_CARD_BALANCE","outstandingBalance":18700000,"currency":"EUR"}'
check X08 'currency "EUR"' 400 "$MR and $(noecho EUR)"
call X09 POST /liabilities "$TA" '{"name":"Thẻ tín dụng","category":"CREDIT_CARD_BALANCE","outstandingBalance":18700000,"currency":1}'
check X09 'currency 1 (số)' 400 "$MR"
call X10 POST /liabilities "$TA" '{"name":"Thẻ tín dụng","category":0,"outstandingBalance":18700000,"currency":"VND"}'
check X10 'category 0 (số)' 400 "$MR"
call X11 POST /liabilities "$TA" "{\"name\":\"Thẻ tín dụng Techcombank Visa\",\"category\":\"CREDIT_CARD_BALANCE\",\"outstandingBalance\":18700000,\"currency\":\"VND\",\"userId\":\"$RANDOM_ID\",\"id\":\"$LBB\"}"
check X11 'trường thừa userId, id' 201 ".id != \"$LBB\""
call X12 POST /liabilities "$TA" '{"name":"Thẻ tín dụng","category":"CREDIT_CARD_BALANCE","outstandingBalance":1e2147483648,"currency":"VND"}'
check X12 'outstandingBalance 1e2147483648' 400 "$MR"
call X13 POST /liabilities "$TA" "{\"name\":\"Thẻ tín dụng\",\"category\":\"CREDIT_CARD_BALANCE\",\"outstandingBalance\":1$(repeat 0 1200),\"currency\":\"VND\"}"
check X13 'số 1.201 chữ số' 400 "$MR"
call X14 GET /liabilities/abc "$TA" -
check X14 'GET /liabilities/abc' 400 "$MR and $(noecho abc)"
call X15 PUT /settings/exchange-rate "$TA" '{"usdToVndRate":-1,"effectiveDate":null}' -H 'Accept-Language: de'
check X15 'settings sai, Accept-Language de' 400 "$(vf usdToVndRate 'must be greater than zero') and $(vf effectiveDate 'must not be null')"
call X16 POST /liabilities "$TA" '{"name":12345,"category":"PERSONAL_LOAN","outstandingBalance":50000000,"currency":"VND"}'
check X16 'name là số 12345' 201 '.name == "12345"'
call X17 POST /liabilities "$TA" '{"name":"Vay người thân","category":"PERSONAL_LOAN","outstandingBalance":"2400000000","currency":"VND"}'
check X17 'outstandingBalance là chuỗi' 201 '.outstandingBalance == 2400000000'
call X18 DELETE "/assets/$ASV?confirm=abc" "$TA" -
check X18 'DELETE confirm=abc' 400 "$MR and $(noecho abc)"

# ---------------------------------------------------------------- N — numbers
ASSET_HEAD='"category":"BANK_ACCOUNT","currency":"VND","valuationDate":"2026-11-01","liquidity":"HIGH"'
call N01 POST /assets "$TA" "{\"name\":\"Tài khoản ACB\",$ASSET_HEAD,\"currentValue\":-1}"
check N01 'currentValue -1' 400 "$(vf currentValue 'must not be negative')"
call N02 POST /assets "$TA" "{\"name\":\"Tài khoản ACB đã tất toán\",$ASSET_HEAD,\"currentValue\":0}"
check N02 'currentValue 0' 201 '.currentValue == 0'
DIGITS_17_2='must have at most 17 integer digits and 2 decimals'
call N03 POST /assets "$TA" "{\"name\":\"Tài khoản ACB\",$ASSET_HEAD,\"currentValue\":123456789012345678}"
check N03 'currentValue 18 chữ số nguyên' 400 "$(vf currentValue "$DIGITS_17_2")"
call N04 POST /assets "$TA" "{\"name\":\"Tài khoản ACB\",$ASSET_HEAD,\"currentValue\":1250000.005}"
check N04 'currentValue 3 chữ số thập phân' 400 "$(vf currentValue "$DIGITS_17_2")"
call N05 POST /assets "$TA" "{\"name\":\"Tài khoản ACB\",$ASSET_HEAD,\"currentValue\":1e400}"
check N05 'currentValue 1e400' 400 "$(vf currentValue "$DIGITS_17_2")"
call N06 POST /assets "$TA" "{\"name\":\"Cổ phiếu FPT\",\"category\":\"LISTED_SECURITIES\",\"currency\":\"VND\",\"valuationDate\":\"2026-11-01\",\"liquidity\":\"HIGH\",\"currentValue\":120000000,\"quantity\":0}"
check N06 'quantity 0' 400 "$(vf quantity 'must be greater than zero')"
call N07 POST /assets "$TA" "{\"name\":\"Sổ tiết kiệm BIDV 12 tháng\",\"category\":\"TERM_DEPOSIT\",\"currency\":\"VND\",\"valuationDate\":\"2026-11-01\",\"liquidity\":\"MEDIUM\",\"currentValue\":300000000,\"interestRate\":100.0001}"
check N07 'interestRate 100.0001' 400 "$(vf interestRate 'must be at most 100')"
call N08 POST "/assets/$ASV/valuations" "$TA" "{\"value\":-1,\"currency\":\"VND\",\"effectiveDate\":\"$TODAY\"}"
check N08 'valuation value -1' 400 "$(vf value 'must not be negative')"
call N09 POST /liabilities "$TA" '{"name":"Thẻ tín dụng","category":"CREDIT_CARD_BALANCE","outstandingBalance":-0.01,"currency":"VND"}'
check N09 'outstandingBalance -0.01' 400 "$(vf outstandingBalance 'must not be negative')"
call N10 POST /liabilities "$TA" '{"name":"Vay tiêu dùng","category":"PERSONAL_LOAN","outstandingBalance":50000000,"currency":"VND","recurringPaymentAmount":0,"recurringPaymentFrequency":"MONTHLY"}'
check N10 'recurringPaymentAmount 0' 400 "$(vf recurringPaymentAmount 'must be greater than zero')"
call N11 POST /liabilities "$TA" '{"name":"Vay tiêu dùng","category":"PERSONAL_LOAN","outstandingBalance":50000000,"currency":"VND","interestRate":1000}'
check N11 'liability interestRate 1000' 400 "$(vf interestRate 'must have at most 3 integer digits and 4 decimals')"
CF_HEAD='"entryType":"EXPENSE","category":"LIVING_COSTS","currency":"VND","timing":"ONE_TIME","effectiveDate":"2026-11-10"'
call N12 POST /cash-flow-entries "$TA" "{$CF_HEAD,\"amount\":0}"
check N12 'amount 0' 400 "$(vf amount 'must be greater than zero')"
call N13 POST /cash-flow-entries "$TA" "{$CF_HEAD,\"amount\":-45000000}"
check N13 'amount -45.000.000' 400 "$(vf amount 'must be greater than zero')"
call N14 POST /goals "$TA" '{"name":"Học phí đại học cho con","type":"EDUCATION","targetAmount":0,"targetDate":"2032-08-31"}'
check N14 'targetAmount 0' 400 "$(vf targetAmount 'must be greater than zero')"
call N15 POST /goals "$TA" '{"name":"Học phí đại học cho con","type":"EDUCATION","targetAmount":800000000,"targetDate":"2032-08-31","expectedAnnualReturnPercent":30.01}'
check N15 'expectedAnnualReturnPercent 30.01' 400 "$(vf expectedAnnualReturnPercent 'must be between 0 and 30')"
call N16 POST /goals "$TA" '{"name":"Quỹ dự phòng","type":"EMERGENCY_FUND","targetAmount":270000000,"currentAllocatedAmount":300000000,"targetDate":"2027-12-31"}'
check N16 'đã có > mục tiêu' 400 "$(vf currentAllocatedAmount 'must not exceed the target amount')"
call N17 PUT /settings/exchange-rate "$TA" '{"usdToVndRate":0,"effectiveDate":"2026-11-20"}'
check N17 'usdToVndRate 0' 400 "$(vf usdToVndRate 'must be greater than zero')"
call N18 PUT /settings/exchange-rate "$TA" '{"usdToVndRate":25100.1234567,"effectiveDate":"2026-11-20"}'
check N18 'usdToVndRate 7 chữ số thập phân' 400 "$(vf usdToVndRate 'must have at most 13 integer digits and 6 decimals')"
call N19 PATCH /onboarding "$TA" '{"currentStep":8}'
check N19 'currentStep 8' 400 "$(vf currentStep 'must be between 1 and 7')"
call N20 PATCH /onboarding "$TA" '{"currentStep":2.5}'
check N20 'currentStep 2.5' 400 "$MR"

# ---------------------------------------------------------------- D — dates
call D01 POST /assets "$TA" '{"name":"Tài khoản ACB","category":"BANK_ACCOUNT","currency":"VND","valuationDate":"2026-02-30","liquidity":"HIGH","currentValue":85000000}'
check D01 'valuationDate 2026-02-30' 400 "$MR"
call D02 POST /assets "$TA" "{\"name\":\"Tài khoản ACB\",\"category\":\"BANK_ACCOUNT\",\"currency\":\"VND\",\"valuationDate\":\"$TOMORROW\",\"liquidity\":\"HIGH\",\"currentValue\":85000000}"
check D02 'valuationDate ngày mai' 400 "$(vf valuationDate 'must not be in the future')"
call D03 POST /assets "$TA" '{"name":"Xe Honda SH 160i","category":"VEHICLE_OR_VALUABLE_ASSET","currency":"VND","valuationDate":"2026-11-15","purchaseDate":"2026-11-20","liquidity":"MEDIUM","currentValue":95000000}'
check D03 'mua sau ngày định giá' 400 "$(vf purchaseDate 'must not be later than the valuation date')"
call D04 POST "/assets/$ASV/valuations" "$TA" "{\"value\":4250000000,\"currency\":\"VND\",\"effectiveDate\":\"$TOMORROW\"}"
check D04 'valuation ngày mai' 400 "$(vf effectiveDate 'must not be in the future')"
call D05 POST "/assets/$ASV/valuations" "$TA" '{"value":2900000000,"currency":"VND","effectiveDate":"2020-01-01"}'
check D05 'valuation trước ngày mua' 400 "$(vf effectiveDate 'must not be earlier than the purchase date')"
LB_HEAD='"name":"Vay mua nhà BIDV","category":"BANK_LOAN","outstandingBalance":1500000000,"currency":"VND"'
call D06 POST /liabilities "$TA" "{$LB_HEAD,\"startDate\":\"2021-06-15\",\"maturityDate\":\"2020-01-01\"}"
check D06 'đáo hạn trước ngày bắt đầu' 400 "$(vf maturityDate 'must not be before the start date')"
call D07 POST /liabilities "$TA" "{$LB_HEAD,\"startDate\":\"2021-06-15\",\"maturityDate\":\"2041-06-15\",\"recurringPaymentAmount\":18000000,\"recurringPaymentFrequency\":\"MONTHLY\",\"nextPaymentDate\":\"2042-01-01\"}"
check D07 'kỳ trả sau đáo hạn' 400 "$(vf nextPaymentDate 'must not be after the maturity date')"
call D08 POST /liabilities "$TA" "{$LB_HEAD,\"startDate\":\"-5000-01-01\"}"
check D08 'startDate -5000-01-01' 400 "$MR and $(noecho 5000)"
call D09 POST /liabilities "$TA" "{$LB_HEAD,\"startDate\":\"+10000-01-01\"}"
check D09 'startDate +10000-01-01' 400 "$MR"
CF_SAL='"entryType":"INCOME","category":"SALARY","amount":45000000,"currency":"VND","timing":"RECURRING","recurrence":"MONTHLY"'
call D10 POST /cash-flow-entries "$TA" "{$CF_SAL,\"effectiveDate\":\"0000-01-31\"}"
check D10 'effectiveDate 0000-01-31' 400 "$MR"
call D11 POST /cash-flow-entries "$TA" "{$CF_SAL,\"effectiveDate\":\"2026-09-12T10:00:00\"}"
check D11 'effectiveDate date-time' 400 "$MR"
call D12 POST /cash-flow-entries "$TA" "{$CF_SAL,\"effectiveDate\":20000}"
check D12 'effectiveDate 20000 (số)' 400 "$MR"
call D13 POST /cash-flow-entries "$TA" "{$CF_SAL,\"effectiveDate\":[2026,9,12]}"
check D13 'effectiveDate [2026,9,12]' 400 "$MR"
call D14 POST /cash-flow-entries "$TA" "{$CF_SAL,\"effectiveDate\":\"2026-01-05\",\"endDate\":\"2026-01-01\"}"
check D14 'endDate trước effectiveDate' 400 "$(vf endDate 'must not be before the effective date')"
call D15 POST /cash-flow-entries "$TA" '{"entryType":"INCOME","category":"OTHER_INCOME","amount":60000000,"currency":"VND","timing":"ONE_TIME","effectiveDate":"2027-01-15","notes":"Thưởng Tết dự kiến"}'
check D15 'effectiveDate tương lai 15/01/2027' 201 '.effectiveDate == "2027-01-15"'
call D16 POST /goals "$TA" "{\"name\":\"Du lịch Nhật Bản\",\"type\":\"OTHER_GOAL\",\"targetAmount\":120000000,\"targetDate\":\"$TODAY\"}"
check D16 'targetDate hôm nay' 400 "$(vf targetDate 'must be after today')"
call D17 POST /goals "$TA" '{"name":"Du lịch Nhật Bản","type":"OTHER_GOAL","targetAmount":120000000,"targetDate":"+999999999-12-31"}'
check D17 'targetDate +999999999-12-31' 400 "$MR"
call D18 PUT /settings/exchange-rate "$TA" "{\"usdToVndRate\":25100,\"effectiveDate\":\"$TOMORROW\"}"
check D18 'effectiveDate ngày mai' 400 "$(vf effectiveDate 'must not be in the future')"
call D19 PUT /settings/exchange-rate "$TA" '{"usdToVndRate":25100,"effectiveDate":"2026-09-12T20:00:00Z"}'
check D19 'effectiveDate date-time UTC' 400 "$MR"
call D20 POST /assets "$TA" "{\"name\":\"Tài khoản USD HSBC\",\"category\":\"BANK_ACCOUNT\",\"currency\":\"USD\",\"valuationDate\":\"2026-11-01\",\"liquidity\":\"HIGH\",\"currentValue\":5000,\"usdToVndRate\":25100,\"rateEffectiveDate\":\"$TOMORROW\"}"
check D20 'rateEffectiveDate ngày mai' 400 "$(vf rateEffectiveDate 'must not be in the future')"
call D21 GET '/cash-flow-occurrences?from=2026-02-29&to=2026-03-31' "$TA" -
check D21 'occurrences from=2026-02-29' 400 "$(vf from 'must be a calendar date in the form YYYY-MM-DD')"
call D22 GET '/cash-flow-occurrences?from=2026-12-01&to=2026-11-01' "$TA" -
check D22 'occurrences to < from' 400 "$(vf to 'must not be before from')"
call D23 GET '/cash-flow-reports/monthly?month=2026-13' "$TA" -
check D23 'report month=2026-13' 400 "$(vf month 'must be a calendar month in the form YYYY-MM')"
call D24 GET '/cash-flow-forecast?from=2026-9' "$TA" -
check D24 'forecast from=2026-9' 400 "$(vf from 'must be a calendar month in the form YYYY-MM')"
call D25 GET "/cash-flow-forecast?from=$THIS_MONTH" "$TA" -
check D25 'forecast from=tháng này' 400 "$(vf from 'must not be before the month after today')"
call D26 GET '/cash-flow-entries?effectiveFrom=2026-02-30' "$TA" -
check D26 'effectiveFrom=2026-02-30' 400 "$(vf effectiveFrom 'must be a calendar date in the form YYYY-MM-DD')"
call D27 POST /liabilities "$TA" '{"name":"Vay người thân","category":"PERSONAL_LOAN","outstandingBalance":30000000,"currency":"VND","startDate":""}'
check D27 'startDate ""' 201 '.startDate == null'
call D28 POST /cash-flow-entries "$TA" "{$CF_SAL,\"effectiveDate\":\"\"}"
check D28 'effectiveDate ""' 400 "$(vf effectiveDate 'must not be null')"

# ---------------------------------------------------------------- C — currency
call C01 POST /assets "$TA" '{"name":"Tài khoản EUR","category":"BANK_ACCOUNT","currency":"EUR","valuationDate":"2026-11-01","liquidity":"HIGH","currentValue":5000}'
check C01 'asset currency EUR' 400 "$MR and $(noecho EUR)"
call C02 POST "/assets/$ASU/valuations" "$TA" "{\"value\":537140000,\"currency\":\"VND\",\"effectiveDate\":\"$TODAY\"}"
check C02 'valuation VND trên tài sản USD' 400 "$(vf currency "must match the asset's currency")"
call C03 POST /assets "$TA" '{"name":"Tài khoản Techcombank","category":"BANK_ACCOUNT","currency":"VND","valuationDate":"2026-11-01","liquidity":"HIGH","currentValue":85000000,"usdToVndRate":25100,"rateEffectiveDate":"2026-11-01"}'
check C03 'VND kèm tỷ giá' 400 "$(vf usdToVndRate 'must be absent for a VND valuation')"
call C04 POST /assets "$TA" '{"name":"Tài khoản USD HSBC","category":"BANK_ACCOUNT","currency":"USD","valuationDate":"2026-11-01","liquidity":"HIGH","currentValue":5000,"usdToVndRate":25100}'
check C04 'USD có tỷ giá thiếu ngày' 400 "$(vf rateEffectiveDate 'must be given together with usdToVndRate')"
call C05 POST /cash-flow-entries "$TA" '{"entryType":"EXPENSE","category":"LIVING_COSTS","amount":1200,"currency":"usd","timing":"ONE_TIME","effectiveDate":"2026-11-10"}'
check C05 'currency "usd"' 400 "$MR"
call C06 GET '/liabilities?currency=EUR' "$TA" -
check C06 'list currency=EUR' 400 "$(vf currency 'must be one of VND, USD') and $(noecho EUR)"

# ---------------------------------------------------------------- T — text
call T01 POST /assets "$TA" "{\"name\":\"\\u00a0\",$ASSET_HEAD,\"currentValue\":85000000}"
check T01 'name U+00A0' 400 "$(vf name 'must not be blank')"
call T02 POST /assets "$TA" "{\"name\":\"Tiền\\u0000mặt\",$ASSET_HEAD,\"currentValue\":85000000}"
check T02 'name chứa U+0000' 400 "$(vf name 'must not contain a null character') and $(noecho 'Tiền')"
call T03 POST /assets "$TA" "{\"name\":\"$(repeat Đ 121)\",$ASSET_HEAD,\"currentValue\":85000000}"
check T03 'name 121 ký tự' 400 "$(vf name 'must be at most 120 characters')"
call T04 POST /liabilities "$TA" '{"name":"Vay mua xe","category":"BANK_LOAN","outstandingBalance":350000000,"currency":"VND","notes":"\u00a0\u3000"}'
check T04 'notes chỉ khoảng trắng Unicode' 201 '.notes == null'
call T05 POST /cash-flow-entries "$TA" "{$CF_HEAD,\"amount\":3500000,\"notes\":\"Tiền\\u0000điện\"}"
check T05 'notes chứa U+0000' 400 "$(vf notes 'must not contain a null character')"
call T06 POST /goals "$TA" '{"name":"\u00a0","type":"OTHER_GOAL","targetAmount":120000000,"targetDate":"2028-06-30"}'
check T06 'goal name U+00A0' 400 "$(vf name 'must not be blank')"
call T07 POST /feedback "$TA" '{"screen":"dashboard","message":"\u00a0\u3000"}'
check T07 'feedback message trắng' 400 "$(vf message 'must not be blank')"
call T08 POST /feedback "$TA" '{"screen":"Dashboard","message":"Biểu đồ khó đọc"}'
check T08 'feedback screen hoa' 400 "$(vf screen 'must be a lowercase screen code such as dashboard or catalog/assets')"
call T09 PUT /profile "$TA" '{"displayName":"\u00a0"}'
check T09 'displayName U+00A0' 400 "$(vf displayName 'must not be blank')"
call T10 PUT /profile "$TA" '{"displayName":"An\u0000Nguyễn"}'
check T10 'displayName chứa U+0000' 400 "$(vf displayName 'must not contain a null character')"
call T11 PUT /profile "$TA" "{\"displayName\":\"$(repeat a 121)\"}"
check T11 'displayName 121 ký tự' 400 "$(vf displayName 'must be at most 120 characters')"
call T12 POST /assets "$TA" "{\"name\":\"\\u200b\",$ASSET_HEAD,\"currentValue\":85000000}"
check T12 'name U+200B' 400 "$(vf name 'must not be blank')"
call T13 POST "/assets/$ASV/valuations" "$TA" "{\"value\":4210000000,\"currency\":\"VND\",\"effectiveDate\":\"$TODAY\",\"note\":\"\\ufeff\"}"
check T13 'valuation note U+FEFF' 201 '.note == null'
call T14 POST /auth/dev-sign-in "" '{"email":"cuong.le@example.com","displayName":"Cường\u0000Lê"}'
check T14 'dev-sign-in displayName U+0000' 400 "$(vf displayName 'must not contain a null character')"
call T15 GET "/assets?q=$(repeat a 121)" "$TA" -
check T15 'q 121 ký tự' 400 "$(vf q 'must be at most 120 characters')"

# ---------------------------------------------------------------- R — relations and ownership
pair() { # ID DESC METHOD PATH_OTHER PATH_MISSING BODY
    call "$1-other" "$3" "$4" "$TA" "$6"
    local s1=$STATUS
    call "$1-missing" "$3" "$5" "$TA" "$6"
    local s2=$STATUS
    local same
    same=$(same_after_drop "$OUT/$1-other.body" "$OUT/$1-missing.body")
    local ok=0
    if [ "$s1" = 404 ] && [ "$s2" = 404 ] && [ "$same" = 1 ]; then ok=1; fi
    verdict "$1" "$2" "$ok" "của B: $s1, không tồn tại: $s2, giống từng byte sau khi bỏ instance/correlationId: $same"
}
pair R01 'GET asset của B so với id không tồn tại' GET "/assets/$ASB" "/assets/$RANDOM_ID" -
pair R02 'PUT liability của B so với id không tồn tại' PUT "/liabilities/$LBB" "/liabilities/$RANDOM_ID" "$LIA"
pair R03 'DELETE goal của B so với id không tồn tại' DELETE "/goals/$GB" "/goals/$RANDOM_ID" -
pair R04 'PUT cash-flow của B so với id không tồn tại' PUT "/cash-flow-entries/$CFB" "/cash-flow-entries/$RANDOM_ID" "{$CF_HEAD,\"amount\":3500000}"
pair R05 'GET valuations asset của B so với id không tồn tại' GET "/assets/$ASB/valuations" "/assets/$RANDOM_ID/valuations" -
call R06-other POST /liabilities "$TA" "{$LB_HEAD,\"collateralAssetId\":\"$ASB\"}"
call R06-missing POST /liabilities "$TA" "{$LB_HEAD,\"collateralAssetId\":\"$RANDOM_ID\"}"
R06_SAME=$(same_after_drop "$OUT/R06-other.body" "$OUT/R06-missing.body")
R06_OK=0
if [ "$R06_SAME" = 1 ] && jq -e "$(vf collateralAssetId 'must identify an asset the signed-in user owns')" "$OUT/R06-other.body" > /dev/null; then R06_OK=1; fi
verdict R06 'collateral là asset của B so với id không tồn tại' "$R06_OK" "giống nhau trừ correlationId: $R06_SAME; body: $(jq -c . "$OUT/R06-other.body")"
call R07 POST /cash-flow-entries "$TA" "{\"entryType\":\"INCOME\",\"category\":\"RENTAL_INCOME\",\"amount\":35000000,\"currency\":\"VND\",\"timing\":\"RECURRING\",\"recurrence\":\"MONTHLY\",\"effectiveDate\":\"2026-02-01\",\"linkedAssetId\":\"$ASB\"}"
check R07 'linkedAssetId của B' 400 "$(vf linkedAssetId "must reference one of the signed-in user's assets")"
call R08 POST /cash-flow-entries "$TA" "{\"entryType\":\"EXPENSE\",\"category\":\"INTEREST_PAYMENT\",\"amount\":16000000,\"currency\":\"VND\",\"timing\":\"RECURRING\",\"recurrence\":\"MONTHLY\",\"effectiveDate\":\"2026-02-05\",\"linkedAssetId\":\"$ASV\",\"linkedLiabilityId\":\"$LB1\"}"
check R08 'link cả asset và liability' 400 "$(vf linkedLiabilityId 'must not be combined with linkedAssetId')"
call R09 POST "/assets/$ASV/valuations" "$TA" "{\"value\":4190000000,\"currency\":\"VND\",\"effectiveDate\":\"$TODAY\",\"note\":\"Điều chỉnh theo thẩm định\",\"correctsValuationId\":\"$ASU_FIRST\"}"
check R09 'correctsValuationId của tài sản khác' 400 "$(vf correctsValuationId 'must identify a valuation of this asset')"

# ---------------------------------------------------------------- M — several errors at once
call M01 POST /cash-flow-entries "$TA" '{"entryType":"EXPENSE","category":"SALARY","amount":45000000,"currency":"VND","timing":"ONE_TIME","recurrence":"MONTHLY","endDate":"2026-12-31","effectiveDate":"2026-11-01"}'
check M01 'ba lỗi trong một body' 400 "(.errors|length) == 3 and $(vf category 'must belong to the entry type') and $(vf recurrence 'must be omitted for a one-time entry') and $(vf endDate 'must be omitted for a one-time entry')"

# ---------------------------------------------------------------- P — privacy over the whole run
FIVE_HUNDREDS=$(awk -F'\t' '$3 == "500"' "$RESULTS" | wc -l | tr -d ' ')
P01_OK=0
if [ "$FIVE_HUNDREDS" = 0 ]; then P01_OK=1; fi
verdict P01 'Số response 500 trong lần chạy' "$P01_OK" "$FIVE_HUNDREDS"
if [ -n "$LOG" ]; then
    ERRORS=$(grep -c ' ERROR ' "$LOG" | tr -d ' ')
    P02_OK=0
    if [ "$ERRORS" = 0 ]; then P02_OK=1; fi
    verdict P02 'Dòng ERROR trong log API' "$P02_OK" "$ERRORS"
    LEAKS=$(grep -cE 'Tiền|Căn hộ Thảo Điền|5001-01-01 BC' "$LOG" | tr -d ' ')
    P03_OK=0
    if [ "$LEAKS" = 0 ]; then P03_OK=1; fi
    verdict P03 'Log API chứa giá trị người dùng gửi' "$P03_OK" "$LEAKS"
fi

PASS=$(grep -c $'\tPASS\t' "$RESULTS")
FAIL=$(grep -c $'\tFAIL\t' "$RESULTS")
echo "PASS=$PASS FAIL=$FAIL"
grep $'\tFAIL\t' "$RESULTS" || true
