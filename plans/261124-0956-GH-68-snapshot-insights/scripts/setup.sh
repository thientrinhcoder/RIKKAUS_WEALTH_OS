#!/usr/bin/env bash
# Seeds the eight users of phase-01-test-scenarios.md through the API on $BASE (local profile, dev sign-in)
# and writes their ids and the relative dates to $OUT/env.sh. Tokens are minted again by cases.sh, since
# an access token lives 15 minutes; nothing secret is written here.
set -euo pipefail
BASE=${BASE:-http://127.0.0.1:8068/api/v1}
OUT=${OUT:?set OUT to a scratch directory}
mkdir -p "$OUT"

signin() { curl -sf -X POST "$BASE/auth/dev-sign-in" -H 'Content-Type: application/json' \
  -d "{\"email\":\"$1\",\"displayName\":\"$2\"}"; }
api() { curl -sf -H "Authorization: Bearer $1" -H 'Content-Type: application/json' -X "$2" "$BASE$3" ${4:+-d "$4"}; }
asset() { api "$1" POST /assets "{\"name\":\"$2\",\"category\":\"$3\",\"currentValue\":\"$4\",\"currency\":\"$5\",\"valuationDate\":\"$6\",\"liquidity\":\"$7\"}" | jq -r .id; }
liab() { api "$1" POST /liabilities "$2" | jq -r .id; }
rate() { api "$1" PUT /settings/exchange-rate "{\"usdToVndRate\":\"$2\",\"effectiveDate\":\"$3\"}" > /dev/null; }

# Today and the relative dates, in Asia/Ho_Chi_Minh.
eval "$(python3 - <<'PY'
from datetime import datetime, timedelta
from zoneinfo import ZoneInfo
d = datetime.now(ZoneInfo("Asia/Ho_Chi_Minh")).date()
for name, n in [("D", 0), ("DM48", -48), ("DM31", -31), ("DM30", -30), ("DM29", -29), ("DM20", -20),
                ("DM10", -10), ("DM7", -7), ("DM6", -6), ("DM5", -5), ("DM3", -3), ("DM2", -2), ("DM1", -1),
                ("DP12", 12), ("DP20", 20), ("DP30", 30), ("DP31", 31), ("DP40", 40), ("DP45", 45)]:
    print(f"{name}={(d + timedelta(days=n)).isoformat()}")
PY
)"

# Bash 3.2 (macOS) has no associative arrays: one TOK_<u>, UID_<u> and EMAIL_<u> variable per user.
user() {
  local body; body=$(signin "$2" "$3")
  printf -v "TOK_$1" '%s' "$(jq -r .accessToken <<<"$body")"
  printf -v "UID_$1" '%s' "$(jq -r .user.id <<<"$body")"
  printf -v "EMAIL_$1" '%s' "$2"
}
user A an.nguyen@example.com "An Nguyễn"
user O binh.tran@example.com "Bình Trần"
user E0 cuong.le@example.com "Cường Lê"
user E1 dung.vo@example.com "Dung Võ"
user C giang.ho@example.com "Giang Hồ"
user H ha.pham@example.com "Hà Phạm"
user Z khoa.do@example.com "Khoa Đỗ"
user N nam.pham@example.com "Nam Phạm"

# A: design guideline fixture 16.3.
T=$TOK_A
rate "$T" 25100 2026-09-12
A1=$(asset "$T" "Tài khoản ngân hàng" BANK_ACCOUNT 650000000 VND "$DM3" HIGH)
A2=$(asset "$T" "Tiền gửi 12 tháng" TERM_DEPOSIT 1200000000 VND "$DM10" MEDIUM)
A3=$(asset "$T" "Danh mục cổ phiếu HOSE" LISTED_SECURITIES 2150000000 VND "$DM1" HIGH)
A4=$(asset "$T" "Căn hộ Thảo Điền" REAL_ESTATE 7500000000 VND "$DM48" LOW)
A5=$(asset "$T" "Tiền gửi USD" BANK_ACCOUNT 20000 USD "$DM5" HIGH)
AL1=$(liab "$T" '{"name":"Khoản vay mua căn hộ","category":"BANK_LOAN","outstandingBalance":"1500000000","currency":"VND","maturityDate":"2041-09-20","recurringPaymentAmount":"18750000","recurringPaymentFrequency":"MONTHLY","nextPaymentDate":"'$DP12'","collateralAssetId":"'$A4'"}')
AL2=$(liab "$T" '{"name":"Dư nợ thẻ tín dụng","category":"CREDIT_CARD_BALANCE","outstandingBalance":"32000000","currency":"VND","nextPaymentDate":"'$DP40'"}')

# O: every rule within its threshold.
T=$TOK_O
rate "$T" 25300 "$DM2"
O1=$(asset "$T" "Cổ phiếu FPT" LISTED_SECURITIES 900000000 VND "$DM2" HIGH)
O2=$(asset "$T" "Tiền gửi BIDV 12 tháng" TERM_DEPOSIT 800000000 VND "$DM7" MEDIUM)
O3=$(asset "$T" "Nhà phố Gò Vấp" REAL_ESTATE 1200000000 VND "$DM20" LOW)
O4=$(asset "$T" "Tài khoản USD Vietcombank" BANK_ACCOUNT 10000 USD "$DM1" HIGH)
OL1=$(liab "$T" '{"name":"Vay mua ô tô VPBank","category":"BANK_LOAN","outstandingBalance":"400000000","currency":"VND","maturityDate":"2029-11-30","recurringPaymentAmount":"12500000","recurringPaymentFrequency":"MONTHLY","nextPaymentDate":"'$DP45'"}')

# E0: every observation exactly at its threshold.
T=$TOK_E0
rate "$T" 25150 "$DM5"
E01=$(asset "$T" "Tiền mặt" CASH 400000000 VND "$DM30" HIGH)
E02=$(asset "$T" "Trái phiếu Chính phủ" BOND 500000000 VND "$DM30" LOW)
E03=$(asset "$T" "Tranh sơn mài" COLLECTIBLE 100000000 VND "$DM30" LOW)
E0L1=$(liab "$T" '{"name":"Vay tiêu dùng","category":"PERSONAL_LOAN","outstandingBalance":"400000000","currency":"VND","nextPaymentDate":"'$DP31'"}')

# E1: every observation one unit past its threshold.
T=$TOK_E1
rate "$T" 25150 "$DM6"
E11=$(asset "$T" "Tiền mặt" CASH 399000000 VND "$DM29" HIGH)
E12=$(asset "$T" "Trái phiếu Chính phủ" BOND 501000000 VND "$DM31" LOW)
E13=$(asset "$T" "Tranh sơn mài" COLLECTIBLE 100000000 VND "$DM10" LOW)
E1L1=$(liab "$T" '{"name":"Vay margin SSI","category":"MARGIN_LOAN","outstandingBalance":"300000000","currency":"VND","nextPaymentDate":"'$DM5'"}')
E1L2=$(liab "$T" '{"name":"Vay tiêu dùng HD Saison","category":"PERSONAL_LOAN","outstandingBalance":"101000000","currency":"VND","recurringPaymentAmount":"3000000","recurringPaymentFrequency":"MONTHLY","nextPaymentDate":"'$DP30'"}')

# C: USD records and no declared rate (declared later by case I14).
T=$TOK_C
C1=$(asset "$T" "Tiền mặt dự phòng" CASH 148000000 VND "$DM3" HIGH)
C2=$(asset "$T" "Tài khoản USD" BANK_ACCOUNT 20000 USD "$DM3" HIGH)
CL1=$(liab "$T" '{"name":"Khoản vay USD từ người thân","category":"PERSONAL_LOAN","outstandingBalance":"5000","currency":"USD","maturityDate":"'$DP20'"}')

# H: VND only, no liability, no rate.
T=$TOK_H
H1=$(asset "$T" "Tiền gửi ACB" TERM_DEPOSIT 800000000 VND "$DM2" MEDIUM)
H2=$(asset "$T" "Cổ phiếu VNM" LISTED_SECURITIES 700000000 VND "$DM2" HIGH)

# Z: total assets of zero, a liability with no date.
T=$TOK_Z
Z1=$(asset "$T" "Xe máy Honda SH đã bán" VEHICLE_OR_VALUABLE_ASSET 0 VND "$DM1" LOW)
ZL1=$(liab "$T" '{"name":"Vay tiêu dùng FE Credit","category":"PERSONAL_LOAN","outstandingBalance":"50000000","currency":"VND"}')

{
  for v in D DM48 DM31 DM30 DM29 DM20 DM10 DM7 DM6 DM5 DM3 DM2 DM1 DP12 DP20 DP30 DP31 DP40 DP45 \
           A1 A2 A3 A4 A5 AL1 AL2 O1 O2 O3 O4 OL1 E01 E02 E03 E0L1 E11 E12 E13 E1L1 E1L2 C1 C2 CL1 H1 H2 Z1 ZL1; do
    printf 'export %s=%q\n' "$v" "${!v}"
  done
  for u in A O E0 E1 C H Z N; do
    uid="UID_$u"; email="EMAIL_$u"
    printf 'export U_%s=%q EMAIL_%s=%q\n' "$u" "${!uid}" "$u" "${!email}"
  done
} > "$OUT/env.sh"
echo "seeded on D=$D: A=$UID_A O=$UID_O N=$UID_N"
