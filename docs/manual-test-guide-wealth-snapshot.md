# Hướng dẫn test thủ công — wealth snapshot

Route `GET /api/v1/wealth-snapshot` (issue #65) ship **không có automated test**, theo chỉ đạo của Product
Owner. Tài liệu này là phần thay thế: chạy lại nó là cách duy nhất một regression trong phép tổng hợp,
quy đổi tỷ giá, partial state, xếp hạng hay cửa sổ nghĩa vụ bị phát hiện. Kết quả của lần chạy đi kèm
implementation ghi ở
[`plans/reports/manual-verification-261123-1537-GH-65-wealth-snapshot-dashboard.md`](../plans/reports/manual-verification-261123-1537-GH-65-wealth-snapshot-dashboard.md).
Danh sách scenario gốc, viết trước khi code, ở
[`plans/261122-1842-GH-65-wealth-snapshot-dashboard/phase-01-test-scenarios.md`](../plans/261122-1842-GH-65-wealth-snapshot-dashboard/phase-01-test-scenarios.md);
số case ở đây là số case ở đó, và **số kỳ vọng của từng case nằm ở bảng đó**.

Công thức, quy tắc partial và quy đổi đang được kiểm chứng ở mục "Wealth snapshot" của
[`api-contract-conventions.md`](./api-contract-conventions.md).

## Chuẩn bị

Mọi lệnh chạy từ thư mục gốc repo, có Docker daemon, `curl`, `jq` 1.7 trở lên, `python3` 3.9 trở lên
(`zoneinfo`). Ba file dưới đây lưu vào một thư mục nháp ngoài repo, ví dụ `S=$(mktemp -d)`; script tự ghi
id, ngày và token vào `$S/env.sh` (quyền `600`), không bao giờ vào repo.

Mọi ngày của cửa sổ nghĩa vụ (`D−5` … `D+40`) được tính lúc chạy từ hôm nay theo `Asia/Ho_Chi_Minh`, nên
guide chạy lại được ngày nào cũng được. Không chạy sát nửa đêm giờ Việt Nam: `setup.sh` và API phải thấy
cùng một "hôm nay".

### 1. PostgreSQL dùng một lần

```bash
docker run -d --name rikkaus-pg-issue-65 -p 127.0.0.1:5465:5432 -e POSTGRES_DB=rikkaus -e POSTGRES_USER=rikkaus -e POSTGRES_PASSWORD=rikkaus-local-only postgres:18.6-alpine3.24
```

### 2. API

JDK 25 bắt buộc. Đóng gói rồi chạy jar để thứ được kiểm đúng là commit đang checkout.

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./services/api/mvnw -q -f services/api/pom.xml clean package -DskipTests
```

```bash
DB_URL=jdbc:postgresql://localhost:5465/rikkaus /opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home/bin/java -jar services/api/target/rikkaus-wealth-api-*.jar --spring.profiles.active=local --server.port=8065
```

Log phải có `now at version v14`: #65 không thêm migration.

### 3. Dữ liệu: `setup.sh`

Chèn bảy user A, B, C, N, Z, T, G vào `wealth.users` (không có endpoint đăng ký), mint token cho từng user,
một token hết hạn và một token ký bằng khóa ngẫu nhiên. Secret ký đọc lúc chạy từ profile `local` đã
commit, không chép vào đây. Rồi khai tỷ giá của A, tạo toàn bộ tài sản và công nợ của fixture **qua API**,
không chèn thẳng.

```bash
#!/usr/bin/env bash
# Seeds users A, B, C, N, Z, T, G, their tokens, and every asset, liability and rate of the phase 1
# fixture through the API. Dates relative to today in Asia/Ho_Chi_Minh are computed here, so the guide
# reruns on any day. Writes ids, dates and tokens to $S/env.sh (scratch only; never committed).
set -euo pipefail
S="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(git rev-parse --show-toplevel)"
BASE=http://127.0.0.1:8065/api/v1
PG=rikkaus-pg-issue-65

UA=0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93; UB=7b41e590-3c82-4f16-b0d7-59e1c4f3a842
UC=3d9a7c25-48e1-4b6f-a2c3-6e8f0b1d5a74; UN=5e8b1f30-9a64-4d27-b3c5-0c7f2e9a6d18
UZ=2a6c9e47-1d83-4b50-8f29-e4b7a1c0d365; UT=8d3f5a12-6e97-4c08-a1b4-7f2d9c6e3b50
UG=c4e70b91-2f58-4a3d-9e16-5b8a0d7f4c29

docker exec -i $PG psql -q -U rikkaus -d rikkaus -c "INSERT INTO wealth.users (id, google_subject, email, display_name, created_at, updated_at) VALUES
 ('$UA','google-sub-a','an.nguyen@example.com','An Nguyễn', now(), now()),
 ('$UB','google-sub-b','bich.tran@example.com','Bích Trần', now(), now()),
 ('$UC','google-sub-c','cuong.le@example.com','Cường Lê', now(), now()),
 ('$UN','google-sub-n','nam.pham@example.com','Nam Phạm', now(), now()),
 ('$UZ','google-sub-z','zung.vo@example.com','Zung Võ', now(), now()),
 ('$UT','google-sub-t','thao.dang@example.com','Thảo Đặng', now(), now()),
 ('$UG','google-sub-g','giang.ho@example.com','Giang Hồ', now(), now());"

# The signing secret is read from the committed local profile at run time, never copied here.
JWT_SECRET=$(awk '/^ *jwt:/{f=1} f && /^ *secret:/{print $2; exit}' "$ROOT/services/api/src/main/resources/application-local.yml")
export JWT_SECRET UA UB UC UN UZ UT UG
eval "$(python3 - <<'PY'
import base64, hashlib, hmac, json, os, time
secret = os.environ["JWT_SECRET"].encode()
def b64(raw): return base64.urlsafe_b64encode(raw).rstrip(b"=").decode()
def mint(sub, ttl=3600, key=secret):
    now = int(time.time())
    h = b64(json.dumps({"alg": "HS256"}, separators=(",", ":")).encode())
    p = b64(json.dumps({"iss": "rikkaus-wealth-api", "sub": sub, "sid": "11111111-2222-3333-4444-555555555555",
                        "iat": now, "exp": now + ttl}, separators=(",", ":")).encode())
    return f"{h}.{p}." + b64(hmac.new(key, f"{h}.{p}".encode(), hashlib.sha256).digest())
for name in ["A", "B", "C", "N", "Z", "T", "G"]:
    print(f"{name}=" + mint(os.environ["U" + name]))
print("EXPIRED=" + mint(os.environ["UA"], ttl=-60))
print("FORGED=" + mint(os.environ["UA"], key=os.urandom(32)))
PY
)"
unset JWT_SECRET

# Today and the relative dates of phase 1, in Asia/Ho_Chi_Minh.
eval "$(python3 - <<'PY'
from datetime import datetime, timedelta
from zoneinfo import ZoneInfo
d = datetime.now(ZoneInfo("Asia/Ho_Chi_Minh")).date()
for name, n in [("D", 0), ("DM5", -5), ("DM1", -1), ("DP7", 7), ("DP13", 13), ("DP20", 20), ("DP28", 28),
                ("DP30", 30), ("DP31", 31), ("DP40", 40)]:
    print(f"{name}={(d + timedelta(days=n)).isoformat()}")
PY
)"

api() { curl -s -H "Authorization: Bearer $1" -H 'Content-Type: application/json' -X "$2" "$BASE$3" ${4:+-d "$4"}; }
asset() { api "$1" POST /assets "{\"name\":\"$2\",\"category\":\"$3\",\"currentValue\":\"$4\",\"currency\":\"$5\",\"valuationDate\":\"$6\",\"liquidity\":\"$7\"}" | jq -r .id; }
liab() { api "$1" POST /liabilities "$2" | jq -r .id; }

# A: the accepted design #63 fixture.
api "$A" PUT /settings/exchange-rate '{"usdToVndRate":"25100.000000","effectiveDate":"2026-09-12"}' > /dev/null
A01=$(asset "$A" "Tiền mặt dự phòng" CASH 148000000 VND 2026-09-12 HIGH)
A02=$(asset "$A" "Tài khoản USD" BANK_ACCOUNT 20000 USD 2026-09-12 HIGH)
A03=$(asset "$A" "Tiền gửi 12 tháng" TERM_DEPOSIT 1200000000 VND 2026-09-08 MEDIUM)
A04=$(asset "$A" "Danh mục cổ phiếu HOSE" LISTED_SECURITIES 2150000000 VND 2026-09-19 HIGH)
A05=$(asset "$A" "Trái phiếu Chính phủ Việt Nam" BOND 420000000 VND 2026-08-31 LOW)
A06=$(asset "$A" "Quỹ mở cổ phiếu Việt" INVESTMENT_FUND 380000000 VND 2026-09-10 HIGH)
A07=$(asset "$A" "Căn hộ Thảo Điền" REAL_ESTATE 6100000000 VND 2026-08-02 LOW)
A08=$(asset "$A" "Vốn góp Công ty Mộc Việt" PRIVATE_BUSINESS_EQUITY 350000000 VND 2026-06-30 LOW)
A09=$(asset "$A" "Xe Volvo XC60" VEHICLE_OR_VALUABLE_ASSET 420000000 VND 2026-09-01 MEDIUM)
A10=$(asset "$A" "Đồng hồ cơ sưu tầm" COLLECTIBLE 110000000 VND 2026-07-15 LOW)
A11=$(asset "$A" "Khoản đặt cọc mặt bằng" OTHER_ASSET 222000000 VND 2026-09-05 MEDIUM)
L1=$(liab "$A" '{"name":"Khoản vay mua nhà","category":"BANK_LOAN","outstandingBalance":"1500000000","currency":"VND","maturityDate":"2041-09-20","recurringPaymentAmount":"18750000","recurringPaymentFrequency":"MONTHLY","nextPaymentDate":"'$DP28'","collateralAssetId":"'$A07'"}')
L2=$(liab "$A" '{"name":"Thẻ tín dụng An Tín","category":"CREDIT_CARD_BALANCE","outstandingBalance":"32000000","currency":"VND","nextPaymentDate":"'$DP40'"}')

# B: ownership.
B1=$(asset "$B" "Sổ tiết kiệm Agribank" TERM_DEPOSIT 300000000 VND 2026-11-01 MEDIUM)

# C: USD without a declared rate (declared later by case S14).
C1=$(asset "$C" "Tiền mặt dự phòng" CASH 148000000 VND 2026-11-15 HIGH)
C2=$(asset "$C" "Tài khoản USD" BANK_ACCOUNT 20000 USD 2026-11-15 HIGH)
CL1=$(liab "$C" '{"name":"Khoản vay USD từ người thân","category":"PERSONAL_LOAN","outstandingBalance":"5000","currency":"USD"}')

# Z: total assets of zero.
Z1=$(asset "$Z" "Xe máy Honda SH đã bán" VEHICLE_OR_VALUABLE_ASSET 0 VND 2026-11-10 LOW)
ZL1=$(liab "$Z" '{"name":"Vay tiêu dùng FE Credit","category":"PERSONAL_LOAN","outstandingBalance":"50000000","currency":"VND"}')

# T: six VND assets, a tie at the fifth place.
T1=$(asset "$T" "Cổ phiếu FPT" LISTED_SECURITIES 900000000 VND 2026-10-01 HIGH)
T2=$(asset "$T" "Tiền gửi BIDV 6 tháng" TERM_DEPOSIT 800000000 VND 2026-10-05 MEDIUM)
T3=$(asset "$T" "Quỹ mở VCBF" INVESTMENT_FUND 700000000 VND 2026-10-10 HIGH)
T4=$(asset "$T" "Trái phiếu Vingroup" BOND 600000000 VND 2026-09-30 LOW)
T5=$(asset "$T" "Ô tô Kia Morning" VEHICLE_OR_VALUABLE_ASSET 500000000 VND 2026-09-15 MEDIUM)
T6=$(asset "$T" "Vàng SJC" VEHICLE_OR_VALUABLE_ASSET 500000000 VND 2026-10-20 HIGH)

# G: the due-date window.
G0=$(asset "$G" "Căn hộ Thảo Điền" REAL_ESTATE 4200000000 VND 2026-10-01 LOW)
G1=$(liab "$G" '{"name":"Vay mua nhà Vietcombank","category":"BANK_LOAN","outstandingBalance":"2400000000","currency":"VND","maturityDate":"2046-11-15","recurringPaymentAmount":"25000000","recurringPaymentFrequency":"MONTHLY","nextPaymentDate":"'$DP13'","collateralAssetId":"'$G0'"}')
G2=$(liab "$G" '{"name":"Thẻ tín dụng Techcombank","category":"CREDIT_CARD_BALANCE","outstandingBalance":"18500000","currency":"VND","nextPaymentDate":"'$DP30'"}')
G3=$(liab "$G" '{"name":"Vay tiêu dùng HD Saison","category":"PERSONAL_LOAN","outstandingBalance":"36000000","currency":"VND","recurringPaymentAmount":"3000000","recurringPaymentFrequency":"MONTHLY","nextPaymentDate":"'$DP31'"}')
G4=$(liab "$G" '{"name":"Vay cầm cố sổ tiết kiệm","category":"BANK_LOAN","outstandingBalance":"300000000","currency":"VND","maturityDate":"'$DP20'"}')
G5=$(liab "$G" '{"name":"Nợ mượn anh Hai","category":"PERSONAL_LOAN","outstandingBalance":"50000000","currency":"VND"}')
G6=$(liab "$G" '{"name":"Vay margin SSI","category":"MARGIN_LOAN","outstandingBalance":"120000000","currency":"VND","nextPaymentDate":"'$DM5'"}')
G7=$(liab "$G" '{"name":"Vay mua xe VPBank","category":"BANK_LOAN","outstandingBalance":"8500000","currency":"VND","maturityDate":"'$DP7'","recurringPaymentAmount":"8500000","recurringPaymentFrequency":"MONTHLY","nextPaymentDate":"'$DP7'"}')

{
  for v in A B C N Z T G EXPIRED FORGED UA UB UC UN UZ UT UG D DM5 DM1 DP7 DP13 DP20 DP28 DP30 DP31 DP40 \
           A01 A02 A03 A04 A05 A06 A07 A08 A09 A10 A11 L1 L2 B1 C1 C2 CL1 Z1 ZL1 T1 T2 T3 T4 T5 T6 \
           G0 G1 G2 G3 G4 G5 G6 G7; do
    printf 'export %s=%q\n' "$v" "${!v}"
  done
} > "$S/env.sh"
chmod 600 "$S/env.sh"
for v in A01 A02 L1 L2 C2 CL1 T6 G1 G7 D; do printf '%s=%s\n' "$v" "${!v}"; done
```

### 4. Reconciliation: `recon.jq`

Tính lại mọi tổng của một body từ chính các dòng `assets` và `liabilities` trong body đó và in một object
các phép so; mọi giá trị phải là `true`. Tỷ số nợ/tài sản được so với phép chia trong `jq` với sai số dưới
`0.0000005`, vì server làm tròn sáu chữ số còn `jq` chia bằng số thực.

```jq
# Every total of a wealth snapshot recomputed from the asset and liability lines in the same body.
# Prints an object of named checks; every value must be true.
def total(f): (map(f) | add) // 0;
def agg($lines; f): {
  known: ($lines | map(f | select(. != null)) | add // 0),
  count: ($lines | length),
  excluded: ($lines | map(select(f == null)) | length)
};
def aggOk($a; $lines; f): agg($lines; f) as $x
  | $a.knownAmountVnd == $x.known and $a.recordCount == $x.count and $a.excludedRecordCount == $x.excluded
    and $a.amountVnd == (if $x.excluded == 0 then $x.known else null end);
def nullsLastDesc: (map(select(. != null))) as $v | $v == ($v | sort | reverse) and (map(. == null) | . == sort);
. as $r
| $r.assets as $as
| $r.liabilities as $ls
| ($r.exchangeRate.usdToVndRate // null) as $rate
| $r.upcomingLiabilityDues as $due
| def conv($v; $cur): if $v == null then null elif $cur == "VND" then $v elif $rate == null then null else ($v * $rate | round) end;
{
  asset_lines_converted: ($as | all(.valueVnd == conv(.value; .currency))),
  liability_lines_converted: ($ls | all(.outstandingBalanceVnd == conv(.outstandingBalance; .currency))),
  total_assets_eq_lines: aggOk($r.kpis.totalAssets; $as; .valueVnd),
  total_liabilities_eq_lines: aggOk($r.kpis.totalLiabilities; $ls; .outstandingBalanceVnd),
  liquid_assets_eq_high_lines: aggOk($r.kpis.liquidAssets; ($as | map(select(.liquidity == "HIGH"))); .valueVnd),
  net_worth_eq_assets_minus_liabilities: ($r.kpis.totalAssets.amountVnd as $a | $r.kpis.totalLiabilities.amountVnd as $l
      | $r.kpis.netWorth == (if $a == null or $l == null then null else $a - $l end)),
  ratio_eq_liabilities_over_assets: ($r.kpis.totalAssets.amountVnd as $a | $r.kpis.totalLiabilities.amountVnd as $l
      | if $a == null or $l == null or $a == 0 then $r.kpis.debtToAssetRatio == null
        else ($r.kpis.debtToAssetRatio != null and (($r.kpis.debtToAssetRatio - ($l / $a)) | fabs) < 0.0000005) end),
  each_category_eq_its_lines: ($r.assetsByCategory | all(. as $g | aggOk($g; ($as | map(select(.category == $g.category))); .valueVnd))),
  categories_cover_every_asset_once: (($as | map(.category) | unique) == ($r.assetsByCategory | map(.category) | sort)
      and ($r.assetsByCategory | total(.recordCount)) == ($as | length)),
  categories_ordered_known_desc_then_code: ($r.assetsByCategory | map([0 - .knownAmountVnd, .category]) | . == sort),
  categories_sum_to_known_total: (($r.assetsByCategory | total(.knownAmountVnd)) == $r.kpis.totalAssets.knownAmountVnd),
  each_liquidity_eq_its_lines: ($r.assetsByLiquidity | all(. as $g | aggOk($g; ($as | map(select(.liquidity == $g.liquidity))); .valueVnd))),
  liquidity_present_levels_in_fixed_order: (($r.assetsByLiquidity | map(.liquidity)) == (["HIGH", "MEDIUM", "LOW"] | map(select(. as $l | $as | any(.liquidity == $l))))),
  liquidity_sum_to_known_total: (($r.assetsByLiquidity | total(.knownAmountVnd)) == $r.kpis.totalAssets.knownAmountVnd),
  largest_is_first_five_converted_lines: (($r.largestAssets.items | map([.assetId, .name, .category, .liquidity, .valueVnd, .valuationDate]))
      == ($as | map(select(.valueVnd != null)) | .[:5] | map([.id, .name, .category, .liquidity, .valueVnd, .valuationDate]))),
  largest_excluded_eq_unconverted_lines: ($r.largestAssets.excludedRecordCount == ($as | map(select(.valueVnd == null)) | length)),
  assets_ordered_value_desc_nulls_last: ($as | map(.valueVnd) | nullsLastDesc),
  liabilities_ordered_balance_desc_nulls_last: ($ls | map(.outstandingBalanceVnd) | nullsLastDesc),
  due_window_end_is_as_of_plus_30: ($due.windowDays == 30
      and $due.windowEnd == ($r.asOfDate + "T00:00:00Z" | fromdateiso8601 + 30 * 86400 | strftime("%Y-%m-%d"))),
  each_due_matches_its_liability: ($due.items | all(. as $d | ($ls | map(select(.id == $d.liabilityId)) | first) as $l
      | $l != null and $d.name == $l.name and $d.category == $l.category and $d.currency == $l.currency
        and (if $d.dueKind == "PAYMENT" then $d.dueDate == $l.nextPaymentDate and $d.amount == $l.recurringPaymentAmount
             else $d.dueKind == "MATURITY" and $d.dueDate == $l.maturityDate and $d.amount == $l.outstandingBalance end)
        and $d.dueDate <= $due.windowEnd and $d.overdue == ($d.dueDate < $r.asOfDate)
        and $d.amountVnd == conv($d.amount; $d.currency))),
  every_date_in_window_has_a_due: (($due.items | length)
      == ([$ls[] | select(.nextPaymentDate != null and .nextPaymentDate <= $due.windowEnd)] | length)
       + ([$ls[] | select(.maturityDate != null and .maturityDate <= $due.windowEnd)] | length)),
  dues_ordered_date_then_kind: ($due.items | map([.dueDate, (if .dueKind == "PAYMENT" then 0 else 1 end)]) | . == sort),
  last_data_update_is_newest_source: ($r.kpis.lastDataUpdate == ($r.sourceDates | map(.date) | max)),
  source_dates_one_per_asset: (($r.sourceDates | map(select(.sourceType == "ASSET") | [.sourceId, .name, .date]) | sort)
      == ($as | map([.id, .name, .valuationDate]) | sort)),
  source_dates_one_per_liability: (($r.sourceDates | map(select(.sourceType == "LIABILITY") | [.sourceId, .name, .date]) | sort)
      == ($ls | map([.id, .name, .updatedOn]) | sort)),
  source_dates_rate_iff_a_usd_line_converted: (($r.sourceDates | map(select(.sourceType == "EXCHANGE_RATE"))) as $x
      | (($as | any(.currency == "USD" and .valueVnd != null)) or ($ls | any(.currency == "USD" and .outstandingBalanceVnd != null))) as $used
      | if $used then $x == [{sourceType: "EXCHANGE_RATE", sourceId: null, name: null, date: $r.exchangeRate.effectiveDate}] else $x == [] end),
  source_dates_newest_first: ($r.sourceDates | map(.date) | . == (sort | reverse)),
  complete_iff_every_line_converted: ($r.complete == (($as | all(.valueVnd != null)) and ($ls | all(.outstandingBalanceVnd != null))))
}
```

### 5. Các case: `cases.sh`

In một dòng `PASS`/`FAIL` cho mỗi phép kiểm, giữ body thật ở `$S/bodies/` cho evidence, và kết thúc bằng
`TOTAL pass=… fail=…`. Chạy **một lần** sau `setup.sh`, theo thứ tự trong script: S14 khai tỷ giá cho C,
S20 và S22 ghi định giá cho T, nên chạy lần hai trên cùng database sẽ fail ở các case trước đó. Muốn chạy
lại thì xóa container và bắt đầu từ bước 1.

```bash
#!/usr/bin/env bash
# Runs the 32 cases of phase 1 against the API on 8065 and prints one PASS/FAIL line per check.
# Bodies are kept in $S/bodies for the evidence report. Run once after setup.sh, in this order:
# S14 declares C's rate and S20/S22 record valuations, so each runs after the cases that need the fixture before it.
set -uo pipefail
S="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(git rev-parse --show-toplevel)"
source "$S/env.sh"
BASE=http://127.0.0.1:8065/api/v1
W=$BASE/wealth-snapshot
mkdir -p "$S/bodies"
PASS=0; FAIL=0

api() { curl -s -H "Authorization: Bearer $1" -H 'Content-Type: application/json' -X "$2" "$BASE$3" ${4:+-d "$4"}; }
# snap ID TOKEN [QUERY] -> saves body and status
snap() { curl -s -o "$S/bodies/$1.json" -w '%{http_code}' "$W${3:-}" -H "Authorization: Bearer $2" > "$S/bodies/$1.status"; }
# ck ID LABEL FILE JQ-BOOL
ck() {
  if jq -e "$4" "$3" > /dev/null 2>&1; then PASS=$((PASS+1)); echo "$1 PASS $2";
  else FAIL=$((FAIL+1)); echo "$1 FAIL $2 :: $(jq -c . "$3" 2>/dev/null | cut -c1-600)"; fi
}
same() {
  if cmp -s "$3" "$4"; then PASS=$((PASS+1)); echo "$1 PASS $2"; else FAIL=$((FAIL+1)); echo "$1 FAIL $2"; fi
}
recon() {
  jq -f "$S/recon.jq" "$S/bodies/$2.json" > "$S/bodies/$1-recon.json"
  ck "$1" "reconciliation of $2 (all true)" "$S/bodies/$1-recon.json" '[.[]] | all'
}

echo "commit $(git -C "$ROOT" rev-parse --short HEAD) $(date '+%Y-%m-%d %H:%M %Z') today=$D"

# A. Fixture design #63
snap S01 "$A"
ck S01 "status 200" "$S/bodies/S01.status" '. == 200'
ck S01 "header, rate, complete" "$S/bodies/S01.json" "
  .asOfDate == \"$D\" and .timeZone == \"Asia/Ho_Chi_Minh\" and .currency == \"VND\"
  and .exchangeRate == {usdToVndRate: 25100, effectiveDate: \"2026-09-12\"} and .complete == true"
ck S02 "kpis" "$S/bodies/S01.json" '
  .kpis.totalAssets == {amountVnd: 12002000000, knownAmountVnd: 12002000000, recordCount: 11, excludedRecordCount: 0}
  and .kpis.totalLiabilities == {amountVnd: 1532000000, knownAmountVnd: 1532000000, recordCount: 2, excludedRecordCount: 0}
  and .kpis.netWorth == 10470000000 and .kpis.debtToAssetRatio == 0.127645
  and .kpis.liquidAssets == {amountVnd: 3180000000, knownAmountVnd: 3180000000, recordCount: 4, excludedRecordCount: 0}'
ck S03 "A02 20000 USD -> 502000000" "$S/bodies/S01.json" "
  .assets[] | select(.id == \"$A02\") | .value == 20000 and .currency == \"USD\" and .valueVnd == 502000000"
ck S04 "assetsByCategory order and amounts" "$S/bodies/S01.json" '
  [.assetsByCategory[] | [.category, .amountVnd, .recordCount, .excludedRecordCount]] == [
   ["REAL_ESTATE",6100000000,1,0], ["LISTED_SECURITIES",2150000000,1,0], ["TERM_DEPOSIT",1200000000,1,0],
   ["BANK_ACCOUNT",502000000,1,0], ["BOND",420000000,1,0], ["VEHICLE_OR_VALUABLE_ASSET",420000000,1,0],
   ["INVESTMENT_FUND",380000000,1,0], ["PRIVATE_BUSINESS_EQUITY",350000000,1,0], ["OTHER_ASSET",222000000,1,0],
   ["CASH",148000000,1,0], ["COLLECTIBLE",110000000,1,0]]
  and ([.assetsByCategory[4:][] | .amountVnd] | add) == 2050000000'
ck S05 "assetsByLiquidity" "$S/bodies/S01.json" '
  [.assetsByLiquidity[] | [.liquidity, .amountVnd, .recordCount]] == [["HIGH",3180000000,4], ["MEDIUM",1842000000,3], ["LOW",6980000000,4]]'
ck S06 "largest five, tie 420M -> Trái phiếu before Xe Volvo" "$S/bodies/S01.json" "
  [.largestAssets.items[] | .assetId] == [\"$A07\",\"$A04\",\"$A03\",\"$A02\",\"$A05\"]
  and [.largestAssets.items[] | .name] == [\"Căn hộ Thảo Điền\",\"Danh mục cổ phiếu HOSE\",\"Tiền gửi 12 tháng\",\"Tài khoản USD\",\"Trái phiếu Chính phủ Việt Nam\"]
  and .largestAssets.excludedRecordCount == 0 and ([.assets[] | .name] | index(\"Xe Volvo XC60\")) == 5"
ck S07 "one upcoming due: L1 payment D+28" "$S/bodies/S01.json" "
  .upcomingLiabilityDues == {windowDays: 30, windowEnd: \"$DP30\", items: [{liabilityId: \"$L1\", name: \"Khoản vay mua nhà\",
   category: \"BANK_LOAN\", dueKind: \"PAYMENT\", dueDate: \"$DP28\", overdue: false, amount: 18750000, currency: \"VND\", amountVnd: 18750000}]}"
ck S08 "lastDataUpdate D, 14 source dates" "$S/bodies/S01.json" "
  .kpis.lastDataUpdate == \"$D\" and (.sourceDates | length) == 14
  and ([.sourceDates[] | .sourceType] | group_by(.) | map([.[0], length])) == [[\"ASSET\",11],[\"EXCHANGE_RATE\",1],[\"LIABILITY\",2]]
  and [.sourceDates[0:2][] | [.sourceType, .name, .date]] == [[\"LIABILITY\",\"Khoản vay mua nhà\",\"$D\"],[\"LIABILITY\",\"Thẻ tín dụng An Tín\",\"$D\"]]
  and [.sourceDates[2:6][] | [.sourceType, .name, .date]] == [[\"ASSET\",\"Danh mục cổ phiếu HOSE\",\"2026-09-19\"],
       [\"ASSET\",\"Tài khoản USD\",\"2026-09-12\"],[\"ASSET\",\"Tiền mặt dự phòng\",\"2026-09-12\"],[\"EXCHANGE_RATE\",null,\"2026-09-12\"]]
  and .sourceDates[-1] == {sourceType: \"ASSET\", sourceId: \"$A08\", name: \"Vốn góp Công ty Mộc Việt\", date: \"2026-06-30\"}"
recon S09 S01
snap S10 "$A"
same S10 "second read byte-identical" "$S/bodies/S01.json" "$S/bodies/S10.json"

# B. Partial state (C)
snap S11 "$C"
ck S11 "status 200" "$S/bodies/S11.status" '. == 200'
ck S11 "partial aggregates" "$S/bodies/S11.json" "
  .complete == false and .exchangeRate == null
  and .kpis.totalAssets == {amountVnd: null, knownAmountVnd: 148000000, recordCount: 2, excludedRecordCount: 1}
  and .kpis.totalLiabilities == {amountVnd: null, knownAmountVnd: 0, recordCount: 1, excludedRecordCount: 1}
  and .kpis.netWorth == null and .kpis.debtToAssetRatio == null
  and .kpis.liquidAssets == {amountVnd: null, knownAmountVnd: 148000000, recordCount: 2, excludedRecordCount: 1}
  and [.assetsByCategory[] | [.category, .amountVnd, .knownAmountVnd, .recordCount, .excludedRecordCount]] == [[\"CASH\",148000000,148000000,1,0],[\"BANK_ACCOUNT\",null,0,1,1]]
  and [.assetsByLiquidity[] | [.liquidity, .amountVnd, .knownAmountVnd, .recordCount, .excludedRecordCount]] == [[\"HIGH\",null,148000000,2,1]]
  and [.largestAssets.items[] | .assetId] == [\"$C1\"] and .largestAssets.excludedRecordCount == 1
  and (.assets[] | select(.id == \"$C2\") | .value == 20000 and .currency == \"USD\" and .valueVnd == null)
  and (.liabilities[0] | .id == \"$CL1\" and .outstandingBalance == 5000 and .outstandingBalanceVnd == null)"
ck S12 "no zero stands in for unavailable; no rate source" "$S/bodies/S11.json" '
  [.kpis.totalAssets.amountVnd, .kpis.totalLiabilities.amountVnd, .kpis.liquidAssets.amountVnd,
   (.assetsByCategory[] | select(.category == "BANK_ACCOUNT") | .amountVnd), .kpis.netWorth, .kpis.debtToAssetRatio] == [null,null,null,null,null,null]
  and ([.sourceDates[] | select(.sourceType == "EXCHANGE_RATE")] | length) == 0'
recon S13 S11
api "$C" PUT /settings/exchange-rate '{"usdToVndRate":"25100.000000","effectiveDate":"2026-11-20"}' > "$S/bodies/S14-put.json"
snap S14 "$C"
ck S14 "after declaring the rate" "$S/bodies/S14.json" "
  .complete == true and .exchangeRate == {usdToVndRate: 25100, effectiveDate: \"2026-11-20\"}
  and .kpis.totalAssets.amountVnd == 650000000 and .kpis.totalLiabilities.amountVnd == 125500000
  and .kpis.netWorth == 524500000 and .kpis.debtToAssetRatio == 0.193077 and .kpis.liquidAssets.amountVnd == 650000000
  and [.assetsByCategory[] | [.category, .amountVnd]] == [[\"BANK_ACCOUNT\",502000000],[\"CASH\",148000000]]
  and ([.sourceDates[] | select(.sourceType == \"EXCHANGE_RATE\") | .date] == [\"2026-11-20\"])"
recon S15 S14

# C. Edges
snap S16 "$N"
ck S16 "status 200, not 404" "$S/bodies/S16.status" '. == 200'
ck S16 "full empty structure" "$S/bodies/S16.json" "
  . == {asOfDate: \"$D\", timeZone: \"Asia/Ho_Chi_Minh\", currency: \"VND\", exchangeRate: null, complete: true,
   kpis: {totalAssets: {amountVnd: 0, knownAmountVnd: 0, recordCount: 0, excludedRecordCount: 0},
          totalLiabilities: {amountVnd: 0, knownAmountVnd: 0, recordCount: 0, excludedRecordCount: 0},
          netWorth: 0, debtToAssetRatio: null,
          liquidAssets: {amountVnd: 0, knownAmountVnd: 0, recordCount: 0, excludedRecordCount: 0}, lastDataUpdate: null},
   assetsByCategory: [], assetsByLiquidity: [], largestAssets: {items: [], excludedRecordCount: 0},
   upcomingLiabilityDues: {windowDays: 30, windowEnd: \"$DP30\", items: []}, sourceDates: [], assets: [], liabilities: []}"
recon S17 S16
snap S18 "$Z"
ck S18 "zero assets: ratio null, net worth negative" "$S/bodies/S18.json" "
  .kpis.totalAssets == {amountVnd: 0, knownAmountVnd: 0, recordCount: 1, excludedRecordCount: 0}
  and .kpis.totalLiabilities.amountVnd == 50000000 and .kpis.netWorth == -50000000 and .kpis.debtToAssetRatio == null
  and .assetsByCategory == [{category: \"VEHICLE_OR_VALUABLE_ASSET\", amountVnd: 0, knownAmountVnd: 0, recordCount: 1, excludedRecordCount: 0}]
  and [.largestAssets.items[] | [.assetId, .valueVnd]] == [[\"$Z1\", 0]]"

# D. Top five, tie, valuation (T)
snap S19 "$T"
ck S19 "top five, Ô tô before Vàng" "$S/bodies/S19.json" "
  .kpis.totalAssets.amountVnd == 4000000000 and .kpis.debtToAssetRatio == 0 and .kpis.liquidAssets.amountVnd == 2100000000
  and .kpis.lastDataUpdate == \"2026-10-20\"
  and [.largestAssets.items[] | .assetId] == [\"$T1\",\"$T2\",\"$T3\",\"$T4\",\"$T5\"]
  and [.assets[] | .name] == [\"Cổ phiếu FPT\",\"Tiền gửi BIDV 6 tháng\",\"Quỹ mở VCBF\",\"Trái phiếu Vingroup\",\"Ô tô Kia Morning\",\"Vàng SJC\"]"
# post ID TOKEN PATH JSON -> saves body and status
post() { curl -s -o "$S/bodies/$1.json" -w '%{http_code}' -X POST "$BASE$3" -H "Authorization: Bearer $2" -H 'Content-Type: application/json' -d "$4" > "$S/bodies/$1.status"; }
post S20-post "$T" "/assets/$T6/valuations" '{"value":"650000000","currency":"VND","effectiveDate":"'$DM1'","note":"Giá vàng SJC tăng"}'
ck S20 "valuation recorded 201" "$S/bodies/S20-post.status" '. == 201'
snap S20 "$T"
ck S20 "kpis and ranking follow the new valuation" "$S/bodies/S20.json" "
  .kpis.totalAssets.amountVnd == 4150000000 and .kpis.liquidAssets.amountVnd == 2250000000
  and .kpis.lastDataUpdate == \"$DM1\"
  and [.largestAssets.items[] | .assetId] == [\"$T1\",\"$T2\",\"$T3\",\"$T6\",\"$T4\"]
  and (.assets[] | select(.id == \"$T6\") | .value == 650000000 and .valueVnd == 650000000 and .valuationDate == \"$DM1\")
  and ([.assetsByCategory[] | select(.category == \"VEHICLE_OR_VALUABLE_ASSET\") | .amountVnd] == [1150000000])"
curl -s "$BASE/assets/$T6/valuations" -H "Authorization: Bearer $T" > "$S/bodies/S21.json"
ck S21 "history kept: 2 records" "$S/bodies/S21.json" "
  (if type == \"array\" then . else (.items // .content // .valuations) end) | length == 2"
post S22-post "$T" "/assets/$T6/valuations" '{"value":"480000000","currency":"VND","effectiveDate":"2026-08-01","note":"Bổ sung định giá tháng 8"}'
ck S22 "back-dated valuation recorded 201" "$S/bodies/S22-post.status" '. == 201'
snap S22 "$T"
same S22 "back-dated valuation leaves the snapshot byte-identical" "$S/bodies/S20.json" "$S/bodies/S22.json"
curl -s "$BASE/assets/$T6/valuations" -H "Authorization: Bearer $T" > "$S/bodies/S22-history.json"
ck S22 "history kept: 3 records" "$S/bodies/S22-history.json" "
  (if type == \"array\" then . else (.items // .content // .valuations) end) | length == 3"
recon S23 S22

# E. Due window (G)
snap S24 "$G"
ck S24 "six dues in order" "$S/bodies/S24.json" "
  [.upcomingLiabilityDues.items[] | [.liabilityId, .dueKind, .dueDate, .amount, .amountVnd, .overdue]] == [
   [\"$G6\",\"PAYMENT\",\"$DM5\",null,null,true],
   [\"$G7\",\"PAYMENT\",\"$DP7\",8500000,8500000,false],
   [\"$G7\",\"MATURITY\",\"$DP7\",8500000,8500000,false],
   [\"$G1\",\"PAYMENT\",\"$DP13\",25000000,25000000,false],
   [\"$G4\",\"MATURITY\",\"$DP20\",300000000,300000000,false],
   [\"$G2\",\"PAYMENT\",\"$DP30\",null,null,false]]
  and ([.upcomingLiabilityDues.items[] | .liabilityId] | index(\"$G3\")) == null
  and ([.upcomingLiabilityDues.items[] | .liabilityId] | index(\"$G5\")) == null"
ck S25 "G totals" "$S/bodies/S24.json" '
  .kpis.totalAssets.amountVnd == 4200000000 and .kpis.totalLiabilities.amountVnd == 2933000000
  and .kpis.netWorth == 1267000000 and .kpis.debtToAssetRatio == 0.698333'
recon S26 S24

# F. Ownership and HTTP
snap S27 "$B"
ck S27 "B sees only B" "$S/bodies/S27.json" "
  [.assets[] | .id] == [\"$B1\"] and .kpis.totalAssets.amountVnd == 300000000 and .liabilities == []"
grep -c -e "$A01" -e "$A07" -e "$L1" -e "$UA" "$S/bodies/S27.json" > "$S/bodies/S27-a-in-b.txt"
ck S27 "no id of A in B's body" "$S/bodies/S27-a-in-b.txt" '. == 0'
grep -c -e "$B1" -e "$UB" "$S/bodies/S10.json" > "$S/bodies/S27-b-in-a.txt"
ck S27 "no id of B in A's body" "$S/bodies/S27-b-in-a.txt" '. == 0'
snap S28 "$B" "?userId=$UA"
same S28 "?userId=<A> with B's token is B's body" "$S/bodies/S27.json" "$S/bodies/S28.json"
for t in none:'' expired:"$EXPIRED" forged:"$FORGED"; do
  name=${t%%:*}; tok=${t#*:}
  if [ -z "$tok" ]; then
    curl -s -D "$S/bodies/S29-$name.headers" -o "$S/bodies/S29-$name.json" -w '%{http_code}' "$W" > "$S/bodies/S29-$name.status"
  else
    curl -s -D "$S/bodies/S29-$name.headers" -o "$S/bodies/S29-$name.json" -w '%{http_code}' "$W" -H "Authorization: Bearer $tok" > "$S/bodies/S29-$name.status"
  fi
  ck S29 "$name token -> 401" "$S/bodies/S29-$name.status" '. == 401'
  ck S29 "$name token -> unauthorized problem" "$S/bodies/S29-$name.json" '.type == "urn:rikkaus:problem:unauthorized" and .status == 401'
  grep -qi '^content-type: application/problem+json' "$S/bodies/S29-$name.headers" && echo true > "$S/bodies/S29-$name.ct" || echo false > "$S/bodies/S29-$name.ct"
  ck S29 "$name token -> problem+json" "$S/bodies/S29-$name.ct" '. == true'
done
for m in POST DELETE; do
  curl -s -o "$S/bodies/S30-$m.json" -w '%{http_code}' -X $m "$W" -H "Authorization: Bearer $A" > "$S/bodies/S30-$m.status"
  ck S30 "$m -> 405" "$S/bodies/S30-$m.status" '. == 405'
  ck S30 "$m -> problem" "$S/bodies/S30-$m.json" '.status == 405'
done
curl -s -o "$S/bodies/S31.json" -w '%{http_code}' "$W" -H "Authorization: Bearer $A" -H 'Accept: text/csv' > "$S/bodies/S31.status"
ck S31 "Accept text/csv -> 406" "$S/bodies/S31.status" '. == 406'
curl -s http://127.0.0.1:8065/v3/api-docs > "$S/bodies/S32.json"
ck S32 "contract publishes GET only, bearerAuth" "$S/bodies/S32.json" '
  (.paths["/api/v1/wealth-snapshot"] | keys) == ["get"]
  and .paths["/api/v1/wealth-snapshot"].get.security == [{"bearerAuth": []}]'

echo "TOTAL pass=$PASS fail=$FAIL"
```

### 6. Dọn

```bash
docker rm -f rikkaus-pg-issue-65
```

Dừng API bằng `Ctrl+C` ở terminal đang chạy jar, rồi xóa thư mục `$S`.

## Đọc kết quả

- `TOTAL pass=N fail=0` là đạt. Một dòng `FAIL` in kèm 600 ký tự đầu của body để đối chiếu với bảng kỳ
  vọng ở phase 1.
- Một `FAIL` ở `reconciliation` nghĩa là một tổng không còn bằng tổng các dòng của nó: mở
  `$S/bodies/<case>-recon.json` để thấy phép so nào sai.
- S10, S22 và S28 so hai body bằng `cmp`: khác một byte là fail.
