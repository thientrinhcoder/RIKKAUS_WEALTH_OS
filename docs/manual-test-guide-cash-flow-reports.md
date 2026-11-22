# Hướng dẫn test thủ công — báo cáo dòng tiền tháng

Route `GET /api/v1/cash-flow-reports/monthly` (issue #77) ship **không có automated test**, theo chỉ đạo của
Product Owner. Tài liệu này là phần thay thế: chạy lại nó là cách duy nhất một regression trong phép tổng
hợp, chia tỷ trọng hay quy đổi tỷ giá bị phát hiện. Kết quả của lần chạy đi kèm implementation ghi ở
[`plans/reports/manual-verification-261122-1538-GH-77-monthly-cash-flow-reports.md`](../plans/reports/manual-verification-261122-1538-GH-77-monthly-cash-flow-reports.md).
Danh sách scenario gốc, viết trước khi code, ở
[`plans/261122-1538-GH-77-monthly-cash-flow-reports/phase-01-test-scenarios.md`](../plans/261122-1538-GH-77-monthly-cash-flow-reports/phase-01-test-scenarios.md);
số case ở đây là số case ở đó, và **số kỳ vọng của từng case nằm ở bảng đó**.

Công thức, quy tắc tỷ trọng và quy đổi đang được kiểm chứng ở mục "Cash-flow monthly report" của
[`api-contract-conventions.md`](./api-contract-conventions.md).

## Chuẩn bị

Mọi lệnh chạy từ thư mục gốc repo, có Docker daemon, `curl`, `jq` 1.7 trở lên, `python3`. Ba file dưới đây
lưu vào một thư mục nháp ngoài repo, ví dụ `S=$(mktemp -d)`; script tự ghi id và token vào `$S/env.sh`
(quyền `600`), không bao giờ vào repo.

### 1. PostgreSQL dùng một lần

```bash
docker run -d --name rikkaus-pg-issue-77 -p 127.0.0.1:5477:5432 -e POSTGRES_DB=rikkaus -e POSTGRES_USER=rikkaus -e POSTGRES_PASSWORD=rikkaus-local-only postgres:18.6-alpine3.24
```

### 2. API

JDK 25 bắt buộc.

```bash
DB_URL=jdbc:postgresql://localhost:5477/rikkaus JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./services/api/mvnw -f services/api/pom.xml spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local --server.port=8077"
```

Log phải có `now at version v12`: #77 không thêm migration.

### 3. Dữ liệu: `setup.sh`

Chèn ba user A, B, C vào `wealth.users` (không có endpoint đăng ký), mint token A, B, C, một token hết hạn
và một token ký bằng khóa ngẫu nhiên. Secret ký đọc lúc chạy từ profile `local` đã commit, không chép vào
đây. Rồi khai tỷ giá của A, tạo hai tài sản và toàn bộ entry của fixture **qua API**, không chèn thẳng.

```bash
#!/usr/bin/env bash
# Seeds users A, B, C, their tokens, A's assets and every entry of the phase 1 fixture.
# Writes ids and tokens to $S/env.sh (scratchpad only; never committed).
set -euo pipefail
S="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(git rev-parse --show-toplevel)"
BASE=http://127.0.0.1:8077/api/v1
PG=rikkaus-pg-issue-77

docker exec -i $PG psql -q -U rikkaus -d rikkaus -c "INSERT INTO wealth.users (id, google_subject, email, display_name, created_at, updated_at) VALUES
 ('0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93','google-sub-a','an.nguyen@example.com','An Nguyễn', now(), now()),
 ('7b41e590-3c82-4f16-b0d7-59e1c4f3a842','google-sub-b','bich.tran@example.com','Bích Trần', now(), now()),
 ('3d9a7c25-48e1-4b6f-a2c3-6e8f0b1d5a74','google-sub-c','cuong.le@example.com','Cường Lê', now(), now());"

# The signing secret is read from the committed local profile at run time, never copied here.
JWT_SECRET=$(awk '/^ *jwt:/{f=1} f && /^ *secret:/{print $2; exit}' "$ROOT/services/api/src/main/resources/application-local.yml")
export JWT_SECRET
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
print("A=" + mint("0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93"))
print("B=" + mint("7b41e590-3c82-4f16-b0d7-59e1c4f3a842"))
print("C=" + mint("3d9a7c25-48e1-4b6f-a2c3-6e8f0b1d5a74"))
print("EXPIRED=" + mint("0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93", ttl=-60))
print("FORGED=" + mint("0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93", key=os.urandom(32)))
PY
)"
unset JWT_SECRET

api() { curl -s -H "Authorization: Bearer $1" -H 'Content-Type: application/json' -X "$2" "$BASE$3" ${4:+-d "$4"}; }
mk() { api "$1" POST /cash-flow-entries "$2" | jq -r .id; }

api "$A" PUT /settings/exchange-rate '{"usdToVndRate":"25100.000000","effectiveDate":"2026-09-12"}' > /dev/null
AS1=$(api "$A" POST /assets '{"name":"Căn hộ Thảo Điền","category":"REAL_ESTATE","currentValue":"6100000000.00","currency":"VND","valuationDate":"2026-08-02","liquidity":"LOW"}' | jq -r .id)
AS2=$(api "$A" POST /assets '{"name":"Cổ phiếu VNM","category":"LISTED_SECURITIES","currentValue":"325000000.00","currency":"VND","valuationDate":"2026-09-01","liquidity":"HIGH"}' | jq -r .id)

R1=$(mk "$A" '{"entryType":"INCOME","category":"SALARY","amount":"95000000","currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-01-05","notes":"Lương tháng"}')
R2=$(mk "$A" '{"entryType":"INCOME","category":"BUSINESS_INCOME","amount":"26200000","currency":"VND","timing":"ONE_TIME","effectiveDate":"2026-09-28","notes":"Doanh thu tư vấn quý 3"}')
R3=$(mk "$A" '{"entryType":"INCOME","category":"RENTAL_INCOME","amount":"18000000","currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-01-10","linkedAssetId":"'$AS1'","notes":"Cho thuê căn hộ Thảo Điền"}')
R4=$(mk "$A" '{"entryType":"INCOME","category":"DIVIDEND","amount":"4500000","currency":"VND","timing":"RECURRING","recurrence":"QUARTERLY","effectiveDate":"2026-03-30","linkedAssetId":"'$AS2'","notes":"Cổ tức VNM"}')
R5=$(mk "$A" '{"entryType":"INCOME","category":"DEPOSIT_INTEREST","amount":"1200","currency":"USD","timing":"RECURRING","recurrence":"YEARLY","effectiveDate":"2025-09-15","notes":"Lãi tiền gửi USD"}')
R6=$(mk "$A" '{"entryType":"INCOME","category":"OTHER_INCOME","amount":"6180000","currency":"VND","timing":"ONE_TIME","effectiveDate":"2026-09-12","notes":"Hoàn thuế TNCN 2025"}')
X1=$(mk "$A" '{"entryType":"EXPENSE","category":"LIVING_COSTS","amount":"23400000","currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-01-31","notes":"Chi phí sinh hoạt gia đình"}')
X2=$(mk "$A" '{"entryType":"EXPENSE","category":"HOUSING","amount":"18750000","currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-01-05","notes":"Điện nước, phí quản lý, internet"}')
X3=$(mk "$A" '{"entryType":"EXPENSE","category":"EDUCATION","amount":"12800000","currency":"VND","timing":"RECURRING","recurrence":"QUARTERLY","effectiveDate":"2026-03-08","notes":"Học phí"}')
X4=$(mk "$A" '{"entryType":"EXPENSE","category":"HEALTHCARE","amount":"4650000","currency":"VND","timing":"ONE_TIME","effectiveDate":"2026-09-18","notes":"Khám sức khỏe định kỳ"}')
X5=$(mk "$A" '{"entryType":"EXPENSE","category":"INSURANCE","amount":"5400000","currency":"VND","timing":"RECURRING","recurrence":"YEARLY","effectiveDate":"2025-09-15","notes":"Phí bảo hiểm sức khỏe gia đình"}')
X6=$(mk "$A" '{"entryType":"EXPENSE","category":"TAX_AND_FEES","amount":"8250000","currency":"VND","timing":"ONE_TIME","effectiveDate":"2026-09-25","notes":"Thuế TNCN tạm nộp"}')
X7=$(mk "$A" '{"entryType":"EXPENSE","category":"DEBT_PRINCIPAL_PAYMENT","amount":"10000000","currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-01-28","notes":"Trả gốc vay mua nhà"}')
X8=$(mk "$A" '{"entryType":"EXPENSE","category":"INTEREST_PAYMENT","amount":"6250000","currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-01-28","notes":"Lãi vay mua nhà"}')
X9=$(mk "$A" '{"entryType":"EXPENSE","category":"OTHER_EXPENSE","amount":"2500000","currency":"VND","timing":"ONE_TIME","effectiveDate":"2026-09-22","notes":"Chi khác"}')
N1=$(mk "$A" '{"entryType":"EXPENSE","category":"OTHER_EXPENSE","amount":"3000000","currency":"VND","timing":"ONE_TIME","effectiveDate":"2026-08-31","notes":"Sửa xe"}')
N2=$(mk "$A" '{"entryType":"INCOME","category":"OTHER_INCOME","amount":"2000000","currency":"VND","timing":"ONE_TIME","effectiveDate":"2026-10-01","notes":"Thưởng dự án"}')
N3=$(mk "$A" '{"entryType":"INCOME","category":"BOND_OR_FUND_INCOME","amount":"1500000","currency":"VND","timing":"RECURRING","recurrence":"QUARTERLY","effectiveDate":"2026-01-20","notes":"Lãi chứng chỉ quỹ"}')
N4=$(mk "$A" '{"entryType":"EXPENSE","category":"HOUSING","amount":"2000000","currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-01-15","endDate":"2026-08-15","notes":"Thuê kho"}')
N5=$(mk "$A" '{"entryType":"EXPENSE","category":"TAX_AND_FEES","amount":"1200000","currency":"VND","timing":"RECURRING","recurrence":"YEARLY","effectiveDate":"2026-03-31","notes":"Phí thường niên thẻ tín dụng"}')
N6=$(mk "$A" '{"entryType":"EXPENSE","category":"LIVING_COSTS","amount":"5000000","currency":"VND","timing":"RECURRING","recurrence":"MONTHLY","effectiveDate":"2026-10-05","notes":"Gửi xe tháng"}')

B1=$(mk "$B" '{"entryType":"INCOME","category":"SALARY","amount":"10000000","currency":"VND","timing":"ONE_TIME","effectiveDate":"2026-09-05","notes":"Lương"}')
B2=$(mk "$B" '{"entryType":"INCOME","category":"BUSINESS_INCOME","amount":"10000000","currency":"VND","timing":"ONE_TIME","effectiveDate":"2026-09-15","notes":"Doanh thu bán hàng online"}')
B3=$(mk "$B" '{"entryType":"INCOME","category":"OTHER_INCOME","amount":"10000000","currency":"VND","timing":"ONE_TIME","effectiveDate":"2026-09-20","notes":"Tiền lì xì cưới"}')
B4=$(mk "$B" '{"entryType":"EXPENSE","category":"LIVING_COSTS","amount":"12000000","currency":"VND","timing":"ONE_TIME","effectiveDate":"2026-09-10","notes":"Chi tiêu gia đình"}')

C1=$(mk "$C" '{"entryType":"INCOME","category":"SALARY","amount":"40000000","currency":"VND","timing":"ONE_TIME","effectiveDate":"2026-09-05","notes":"Lương"}')
C2=$(mk "$C" '{"entryType":"INCOME","category":"DIVIDEND","amount":"150","currency":"USD","timing":"ONE_TIME","effectiveDate":"2026-09-30","notes":"Cổ tức ETF Mỹ"}')
C3=$(mk "$C" '{"entryType":"EXPENSE","category":"HOUSING","amount":"15000000","currency":"VND","timing":"ONE_TIME","effectiveDate":"2026-09-01","notes":"Tiền nhà"}')
C4=$(mk "$C" '{"entryType":"EXPENSE","category":"LIVING_COSTS","amount":"200","currency":"USD","timing":"ONE_TIME","effectiveDate":"2026-09-10","notes":"Phần mềm làm việc"}')

{
  for v in A B C EXPIRED FORGED AS1 AS2 R1 R2 R3 R4 R5 R6 X1 X2 X3 X4 X5 X6 X7 X8 X9 N1 N2 N3 N4 N5 N6 B1 B2 B3 B4 C1 C2 C3 C4; do
    printf 'export %s=%q\n' "$v" "${!v}"
  done
} > "$S/env.sh"
chmod 600 "$S/env.sh"
for v in AS1 AS2 R1 R5 X9 N6 B4 C4; do printf '%s=%s\n' "$v" "${!v}"; done
```

### 4. Reconciliation: `recon.jq`

Tính lại mọi tổng của một body từ chính các dòng `occurrences` trong body đó và in một object các phép so;
mọi giá trị phải là `true`. Tỷ trọng được cộng theo phần mười (`× 10`, `round`) vì `jq` cộng số thực:
`33.4 + 33.3 + 33.3` trong `jq` là `99.99999999999999`.

```jq
# Every total of a monthly report recomputed from the occurrences in the same body.
# Prints an object of named checks; every value must be true.
def total(f): (map(f) | add) // 0;
def passiveKind: IN("RENTAL_INCOME", "DIVIDEND", "DEPOSIT_INTEREST", "BOND_OR_FUND_INCOME");
. as $r
| $r.occurrences as $o
| ($o | map(select(.amount != null))) as $c
| ($c | map(select(.entryType == "INCOME"))) as $ci
| ($c | map(select(.entryType == "EXPENSE"))) as $ce
| def shares($rows; $t): if $t > 0 then ($rows | total(.sharePercent * 10 | round)) == 1000 else ($rows | all(.sharePercent == null)) end;
  def rowOk($type):
    . as $row
    | ($o | map(select(.entryType == $type and .category == $row.category))) as $lines
    | $row.amount == ($lines | map(select(.amount != null)) | total(.amount))
      and $row.occurrenceCount == ($lines | length)
      and $row.partial == ($lines | any(.amount == null));
{
  income_eq_sum_income_lines: ($r.totals.income == ($ci | total(.amount))),
  expense_eq_sum_expense_lines: ($r.totals.expense == ($ce | total(.amount))),
  net_eq_income_minus_expense: ($r.totals.net == ($r.totals.income - $r.totals.expense)),
  income_eq_sum_income_composition: ($r.totals.income == ($r.incomeComposition | total(.amount))),
  expense_eq_sum_expense_composition: ($r.totals.expense == ($r.expenseComposition | total(.amount))),
  each_income_row_eq_its_lines: ($r.incomeComposition | all(rowOk("INCOME"))),
  each_expense_row_eq_its_lines: ($r.expenseComposition | all(rowOk("EXPENSE"))),
  composition_covers_every_line: (($r.incomeComposition | total(.occurrenceCount)) + ($r.expenseComposition | total(.occurrenceCount)) == ($o | length)),
  income_shares_sum_100: shares($r.incomeComposition; $r.totals.income),
  expense_shares_sum_100: shares($r.expenseComposition; $r.totals.expense),
  passive_total_eq_sum_by_asset: ($r.passiveIncome.total == ($r.passiveIncome.byAsset | total(.amount))),
  passive_total_eq_linked_income_lines: ($r.passiveIncome.total == ($ci | map(select(.linkedAssetId != null)) | total(.amount))),
  each_asset_eq_its_lines: ($r.passiveIncome.byAsset | all(. as $a
      | ($o | map(select(.entryType == "INCOME" and .linkedAssetId == $a.assetId))) as $lines
      | $a.amount == ($lines | map(select(.amount != null)) | total(.amount))
        and $a.occurrenceCount == ($lines | length)
        and $a.partial == ($lines | any(.amount == null)))),
  unassigned_eq_its_lines: (($o | map(select(.entryType == "INCOME" and .linkedAssetId == null and (.category | passiveKind)))) as $lines
      | $r.passiveIncome.unassigned.amount == ($lines | map(select(.amount != null)) | total(.amount))
        and $r.passiveIncome.unassigned.occurrenceCount == ($lines | length)
        and $r.passiveIncome.unassigned.partial == ($lines | any(.amount == null))),
  unconverted_count_eq_null_lines: ($r.unconverted.occurrenceCount == ($o | map(select(.amount == null)) | length)),
  unconverted_income_eq_original_usd: ($r.unconverted.income == ($o | map(select(.amount == null and .entryType == "INCOME")) | total(.originalAmount))),
  unconverted_expense_eq_original_usd: ($r.unconverted.expense == ($o | map(select(.amount == null and .entryType == "EXPENSE")) | total(.originalAmount))),
  complete_iff_nothing_unconverted: ($r.complete == ($r.unconverted.occurrenceCount == 0)),
  each_line_amount_is_its_conversion: ($o | all(
      if .originalCurrency == "VND" then .amount == .originalAmount
      elif .amount == null then $r.exchangeRate == null
      else .amount == ((.originalAmount * $r.exchangeRate.usdToVndRate) | round) end)),
  lines_ordered_by_date_then_id: ($o | map([.occurrenceDate, .sourceEntryId]) | . == sort)
}
```

### 5. Các case: `cases.sh`

In một dòng `PASS`/`FAIL` cho mỗi phép kiểm, giữ body thật ở `$S/bodies/` cho evidence, và kết thúc bằng
`TOTAL pass=… fail=…`. Chạy theo thứ tự trong script: M14 xóa một tài sản và M17 khai tỷ giá cho C, nên cả
hai chạy sau các case cần fixture nguyên vẹn.

```bash
#!/usr/bin/env bash
# Runs the 40 cases of phase 1 against the API on 8077 and prints one PASS/FAIL line per check.
# Bodies are kept in $S/bodies for the evidence report. Run after setup.sh, in this order:
# M14 deletes an asset and M17 declares C's rate, so both run after the cases that need the fixture intact.
set -uo pipefail
S="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(git rev-parse --show-toplevel)"
source "$S/env.sh"
BASE=http://127.0.0.1:8077/api/v1
R=$BASE/cash-flow-reports/monthly
mkdir -p "$S/bodies"
PASS=0; FAIL=0

api() { curl -s -H "Authorization: Bearer $1" -H 'Content-Type: application/json' -X "$2" "$BASE$3" ${4:+-d "$4"}; }
# rep ID TOKEN MONTH -> saves body and status
rep() { curl -s -o "$S/bodies/$1.json" -w '%{http_code}' "$R?month=$3" -H "Authorization: Bearer $2" > "$S/bodies/$1.status"; }
# ck ID LABEL FILE JQ-BOOL
ck() {
  if jq -e "$4" "$3" > /dev/null 2>&1; then PASS=$((PASS+1)); echo "$1 PASS $2";
  else FAIL=$((FAIL+1)); echo "$1 FAIL $2 :: $(jq -c . "$3" 2>/dev/null | cut -c1-400)"; fi
}
st() { cat "$S/bodies/$1.status"; }
recon() {
  jq -f "$S/recon.jq" "$S/bodies/$2.json" > "$S/bodies/$1-recon-$2.json"
  ck "$1" "reconciliation of $2 (all true)" "$S/bodies/$1-recon-$2.json" '[.[]] | all'
}

echo "commit $(git -C "$ROOT" rev-parse --short HEAD) $(date '+%Y-%m-%d %H:%M %Z')"

# A. Tháng 09/2026 của A
rep M01 "$A" 2026-09
ck M01 "status 200" "$S/bodies/M01.status" '. == 200'
ck M01 "header, rate, complete, totals, 15 lines" "$S/bodies/M01.json" '
  .month == "2026-09" and .from == "2026-09-01" and .to == "2026-09-30" and .currency == "VND"
  and .exchangeRate == {usdToVndRate: 25100, effectiveDate: "2026-09-12"} and .complete == true
  and .totals == {income: 180000000, expense: 92000000, net: 88000000} and (.occurrences | length) == 15'
ck M02 "the 15 source ids are R1..R6, X1..X9, once each" "$S/bodies/M01.json" "
  (.occurrences | map(.sourceEntryId) | sort) == ([\"$R1\",\"$R2\",\"$R3\",\"$R4\",\"$R5\",\"$R6\",\"$X1\",\"$X2\",\"$X3\",\"$X4\",\"$X5\",\"$X6\",\"$X7\",\"$X8\",\"$X9\"] | sort)"
ck M02 "dates and sequences as the fixture" "$S/bodies/M01.json" "
  (.occurrences | map({(.sourceEntryId): [.occurrenceDate, .sequence]}) | add) == {
   \"$R1\":[\"2026-09-05\",9], \"$R2\":[\"2026-09-28\",1], \"$R3\":[\"2026-09-10\",9], \"$R4\":[\"2026-09-30\",3],
   \"$R5\":[\"2026-09-15\",2], \"$R6\":[\"2026-09-12\",1], \"$X1\":[\"2026-09-30\",9], \"$X2\":[\"2026-09-05\",9],
   \"$X3\":[\"2026-09-08\",3], \"$X4\":[\"2026-09-18\",1], \"$X5\":[\"2026-09-15\",2], \"$X6\":[\"2026-09-25\",1],
   \"$X7\":[\"2026-09-28\",9], \"$X8\":[\"2026-09-28\",9], \"$X9\":[\"2026-09-22\",1]}"
ck M03 "N1..N6 absent" "$S/bodies/M01.json" "
  [.occurrences[].sourceEntryId] as \$ids | [\"$N1\",\"$N2\",\"$N3\",\"$N4\",\"$N5\",\"$N6\"] | all(. as \$n | \$ids | index(\$n) | not)"
ck M04 "monthly, quarterly, yearly sequences" "$S/bodies/M01.json" "
  (.occurrences | map({(.sourceEntryId): [.recurrence, .sequence]}) | add) as \$m
  | \$m[\"$R1\"] == [\"MONTHLY\",9] and \$m[\"$R4\"] == [\"QUARTERLY\",3] and \$m[\"$R5\"] == [\"YEARLY\",2]
    and \$m[\"$X3\"] == [\"QUARTERLY\",3] and \$m[\"$X5\"] == [\"YEARLY\",2] and \$m[\"$R2\"] == [null,1]"
ck M05 "X1 shifted to 2026-09-30" "$S/bodies/M01.json" "
  .occurrences[] | select(.sourceEntryId == \"$X1\") | .occurrenceDate == \"2026-09-30\" and .shiftedToMonthEnd == true"
ck M06 "R5 1200.00 USD -> 30120000.00" "$S/bodies/M01.json" "
  .occurrences[] | select(.sourceEntryId == \"$R5\") | .originalAmount == 1200 and .originalCurrency == \"USD\" and .amount == 30120000"
rep M07a "$A" 2026-08; rep M07b "$A" 2026-10
curl -s "$BASE/cash-flow-occurrences?from=2026-08-01&to=2026-10-31" -H "Authorization: Bearer $A" > "$S/bodies/M07occ.json"
ck M07 "2026-08 has N1 (31/08) and N4 (15/08)" "$S/bodies/M07a.json" "
  [.occurrences[] | select(.sourceEntryId == \"$N1\" or .sourceEntryId == \"$N4\") | [.sourceEntryId, .occurrenceDate]] == [[\"$N4\",\"2026-08-15\"],[\"$N1\",\"2026-08-31\"]]"
ck M07 "2026-10 has N2, N3, N6" "$S/bodies/M07b.json" "
  [.occurrences[] | select(.sourceEntryId == \"$N2\" or .sourceEntryId == \"$N3\" or .sourceEntryId == \"$N6\") | .occurrenceDate] == [\"2026-10-01\",\"2026-10-05\",\"2026-10-20\"]"
jq -n --slurpfile a "$S/bodies/M07a.json" --slurpfile b "$S/bodies/M01.json" --slurpfile c "$S/bodies/M07b.json" --slurpfile w "$S/bodies/M07occ.json" \
  '([$a[0], $b[0], $c[0]] | map(.occurrences[] | [.sourceEntryId, .occurrenceDate]) | sort) == ($w[0] | map([.sourceEntryId, .occurrenceDate]) | sort)
   and ([$a[0], $b[0], $c[0]] | map(.occurrences | length) | add) == ($w[0] | length)' > "$S/bodies/M07join.json"
ck M07 "08 + 09 + 10 lines == occurrences 2026-08-01..2026-10-31, no overlap" "$S/bodies/M07join.json" '. == true'

# B. Composition
ck M08 "income composition order, amounts, shares" "$S/bodies/M01.json" '
  [.incomeComposition[] | [.category, .amount, .sharePercent, .occurrenceCount, .partial]] == [
   ["SALARY",95000000,52.8,1,false], ["DEPOSIT_INTEREST",30120000,16.7,1,false], ["BUSINESS_INCOME",26200000,14.6,1,false],
   ["RENTAL_INCOME",18000000,10.0,1,false], ["OTHER_INCOME",6180000,3.4,1,false], ["DIVIDEND",4500000,2.5,1,false]]'
ck M09 "expense composition, HEALTHCARE 5.0" "$S/bodies/M01.json" '
  [.expenseComposition[] | [.category, .amount, .sharePercent]] == [
   ["LIVING_COSTS",23400000,25.4], ["HOUSING",18750000,20.4], ["EDUCATION",12800000,13.9], ["DEBT_PRINCIPAL_PAYMENT",10000000,10.9],
   ["TAX_AND_FEES",8250000,9.0], ["INTEREST_PAYMENT",6250000,6.8], ["INSURANCE",5400000,5.9], ["HEALTHCARE",4650000,5.0],
   ["OTHER_EXPENSE",2500000,2.7]]'
ck M10 "shares sum exactly 100.0 (summed in tenths)" "$S/bodies/M01.json" '
  ([.incomeComposition[].sharePercent * 10 | round] | add) == 1000 and ([.expenseComposition[].sharePercent * 10 | round] | add) == 1000'
rep M11 "$B" 2026-09
ck M11 "B equal thirds 33.4 / 33.3 / 33.3" "$S/bodies/M11.json" '
  [.incomeComposition[] | [.category, .sharePercent]] == [["BUSINESS_INCOME",33.4],["OTHER_INCOME",33.3],["SALARY",33.3]]
  and ([.incomeComposition[].sharePercent * 10 | round] | add) == 1000'

# C. Passive income
ck M12 "passive total, share, by asset, unassigned" "$S/bodies/M01.json" "
  .passiveIncome == {total: 22500000, sharePercentOfIncome: 12.5,
    byAsset: [{assetId: \"$AS1\", amount: 18000000, sharePercentOfIncome: 10.0, occurrenceCount: 1, partial: false},
              {assetId: \"$AS2\", amount: 4500000, sharePercentOfIncome: 2.5, occurrenceCount: 1, partial: false}],
    unassigned: {amount: 30120000, occurrenceCount: 1, partial: false}}"
ck M13 "R3 under AS1, R4 under AS2, R5 unlinked, R1 R2 R6 unlinked" "$S/bodies/M01.json" "
  (.occurrences | map({(.sourceEntryId): .linkedAssetId}) | add) as \$l
  | \$l[\"$R3\"] == \"$AS1\" and \$l[\"$R4\"] == \"$AS2\" and \$l[\"$R5\"] == null
    and \$l[\"$R1\"] == null and \$l[\"$R2\"] == null and \$l[\"$R6\"] == null"

# E. Currency (before C declares a rate)
rep M16 "$C" 2026-09
ck M16 "C without rate: partial report" "$S/bodies/M16.json" "
  .exchangeRate == null and .complete == false
  and .totals == {income: 40000000, expense: 15000000, net: 25000000}
  and .unconverted == {occurrenceCount: 2, income: 150, expense: 200}
  and ([.occurrences[] | select(.originalCurrency == \"USD\") | [.sourceEntryId, .originalAmount, .amount]] | sort) == ([[\"$C2\",150,null],[\"$C4\",200,null]] | sort)
  and [.incomeComposition[] | [.category, .amount, .sharePercent, .partial]] == [[\"SALARY\",40000000,100.0,false],[\"DIVIDEND\",0,0.0,true]]
  and [.expenseComposition[] | [.category, .amount, .sharePercent, .partial]] == [[\"HOUSING\",15000000,100.0,false],[\"LIVING_COSTS\",0,0.0,true]]
  and .passiveIncome.unassigned == {amount: 0, occurrenceCount: 1, partial: true}"
ck M18 "B: no rate, only VND -> complete" "$S/bodies/M11.json" '.exchangeRate == null and .complete == true and .unconverted.occurrenceCount == 0'

# F. Empty and future
rep M19 "$A" 2025-06
ck M19 "empty month: full structure, zeros" "$S/bodies/M19.status" '. == 200'
ck M19 "empty month body" "$S/bodies/M19.json" '
  .month == "2025-06" and .from == "2025-06-01" and .to == "2025-06-30" and .complete == true and .exchangeRate != null
  and .totals == {income: 0, expense: 0, net: 0} and .unconverted == {occurrenceCount: 0, income: 0, expense: 0}
  and .incomeComposition == [] and .expenseComposition == [] and .occurrences == []
  and .passiveIncome == {total: 0, sharePercentOfIncome: null, byAsset: [], unassigned: {amount: 0, occurrenceCount: 0, partial: false}}'
rep M20 "$A" 2026-12
ck M20 "future month 2026-12" "$S/bodies/M20.json" "
  .totals == {income: 117500000, expense: 76200000, net: 41300000}
  and (.occurrences | map(.sourceEntryId) | sort) == ([\"$R1\",\"$R3\",\"$R4\",\"$X1\",\"$X2\",\"$X3\",\"$X7\",\"$X8\",\"$N6\"] | sort)
  and (.occurrences[] | select(.sourceEntryId == \"$X1\") | .occurrenceDate == \"2026-12-31\" and .shiftedToMonthEnd == false)
  and (.occurrences[] | select(.sourceEntryId == \"$R4\") | .occurrenceDate == \"2026-12-30\")"

# G. Traceability and determinism
: > "$S/bodies/M21-entries.jsonl"
for id in $(jq -r '.occurrences[].sourceEntryId' "$S/bodies/M01.json"); do
  curl -s -w '\n' "$BASE/cash-flow-entries/$id" -H "Authorization: Bearer $A" -o - | jq -c --arg s "$(curl -s -o /dev/null -w '%{http_code}' "$BASE/cash-flow-entries/$id" -H "Authorization: Bearer $A")" '. + {status: ($s | tonumber)}' >> "$S/bodies/M21-entries.jsonl"
done
jq -n --slurpfile r "$S/bodies/M01.json" --slurpfile e <(jq -s . "$S/bodies/M21-entries.jsonl") '
  ($e[0] | map({(.id): .}) | add) as $by
  | $r[0].occurrences | all(. as $l | $by[$l.sourceEntryId] as $x
      | $x.status == 200 and $x.entryType == $l.entryType and $x.category == $l.category and $x.amount == $l.originalAmount
        and $x.currency == $l.originalCurrency and $x.linkedAssetId == $l.linkedAssetId and $x.notes == $l.notes)' > "$S/bodies/M21a.json"
ck M21 "every line reads back from GET /cash-flow-entries/{id}" "$S/bodies/M21a.json" '. == true'
curl -s "$BASE/cash-flow-occurrences?from=2026-09-01&to=2026-09-30" -H "Authorization: Bearer $A" > "$S/bodies/M21occ.json"
jq -n --slurpfile r "$S/bodies/M01.json" --slurpfile o "$S/bodies/M21occ.json" '
  ($r[0].occurrences | map([.sourceEntryId, .occurrenceDate, .sequence, .shiftedToMonthEnd])) == ($o[0] | map([.sourceEntryId, .occurrenceDate, .sequence, .shiftedToMonthEnd]))' > "$S/bodies/M21b.json"
ck M21 "lines == /cash-flow-occurrences 2026-09-01..2026-09-30, same order" "$S/bodies/M21b.json" '. == true'
ck M22 "lines ordered by date then sourceEntryId" "$S/bodies/M01.json" '(.occurrences | map([.occurrenceDate, .sourceEntryId])) as $k | $k == ($k | sort)'
h1=$(curl -s "$R?month=2026-09" -H "Authorization: Bearer $A" | shasum -a 256 | cut -c1-64)
h2=$(curl -s "$R?month=2026-09" -H "Authorization: Bearer $A" | shasum -a 256 | cut -c1-64)
echo "{\"h1\":\"$h1\",\"h2\":\"$h2\"}" > "$S/bodies/M22.json"
ck M22 "two reads byte-identical" "$S/bodies/M22.json" '.h1 == .h2'

# H. Ownership
ck M23 "B sees only its 4 entries" "$S/bodies/M11.json" "
  (.occurrences | map(.sourceEntryId) | sort) == ([\"$B1\",\"$B2\",\"$B3\",\"$B4\"] | sort)
  and .totals == {income: 30000000, expense: 12000000, net: 18000000}"
ck M24 "A sees nothing of B or C" "$S/bodies/M01.json" "
  [.occurrences[].sourceEntryId] as \$ids | ([\"$B1\",\"$B2\",\"$B3\",\"$B4\",\"$C1\",\"$C2\",\"$C3\",\"$C4\"] | all(. as \$n | \$ids | index(\$n) | not)) and (\$ids | length) == 15"

# D. Reconciliation on every body that carries data
recon M15 M01; recon M15 M11; recon M15 M16; recon M15 M20; recon M15 M07a; recon M15 M07b; recon M15 M19

# C/E. Mutations: delete AS2, declare C's rate
curl -s -o /dev/null -w '%{http_code}' -X DELETE "$BASE/assets/$AS2" -H "Authorization: Bearer $A" > "$S/bodies/M14del.status"
rep M14 "$A" 2026-09
ck M14 "DELETE asset AS2 -> 204" "$S/bodies/M14del.status" '. == 204'
ck M14 "R4 moves to unassigned" "$S/bodies/M14.json" "
  .passiveIncome == {total: 18000000, sharePercentOfIncome: 10.0,
    byAsset: [{assetId: \"$AS1\", amount: 18000000, sharePercentOfIncome: 10.0, occurrenceCount: 1, partial: false}],
    unassigned: {amount: 34620000, occurrenceCount: 2, partial: false}}
  and (.occurrences[] | select(.sourceEntryId == \"$R4\") | .linkedAssetId == null)
  and .totals.income == 180000000"
recon M15 M14
api "$C" PUT /settings/exchange-rate '{"usdToVndRate":"25100.000000","effectiveDate":"2026-09-12"}' > "$S/bodies/M17put.json"
rep M17 "$C" 2026-09
ck M17 "C after declaring 25,100" "$S/bodies/M17.json" "
  .complete == true and .exchangeRate == {usdToVndRate: 25100, effectiveDate: \"2026-09-12\"}
  and .totals == {income: 43765000, expense: 20020000, net: 23745000}
  and .unconverted == {occurrenceCount: 0, income: 0, expense: 0}
  and [.incomeComposition[] | [.category, .amount, .sharePercent, .partial]] == [[\"SALARY\",40000000,91.4,false],[\"DIVIDEND\",3765000,8.6,false]]
  and [.expenseComposition[] | [.category, .amount, .sharePercent, .partial]] == [[\"HOUSING\",15000000,74.9,false],[\"LIVING_COSTS\",5020000,25.1,false]]
  and .passiveIncome.unassigned == {amount: 3765000, occurrenceCount: 1, partial: false}"
recon M15 M17

# I. Invalid input
bad() { curl -s -o "$S/bodies/$1.json" -w '%{http_code}' "$R$2" -H "Authorization: Bearer $A" > "$S/bodies/$1.status"; }
badck() {
  ck "$1" "400 validation-failed: $2" "$S/bodies/$1.json" \
    ".status == 400 and .type == \"urn:rikkaus:problem:validation-failed\" and .errors == [{field: \"month\", message: \"$2\"}]"
}
bad M25 ''; badck M25 "must be present"
bad M26 '?month='; badck M26 "must be present"
FMT="must be a calendar month in the form YYYY-MM"
bad M27 '?month=2026-9'; badck M27 "$FMT"
bad M28 '?month=2026-13'; badck M28 "$FMT"
bad M29 '?month=2026-00'; badck M29 "$FMT"
bad M30 '?month=09/2026'; badck M30 "$FMT"
ck M30 "body does not echo 09/2026" "$S/bodies/M30.json" 'tostring | contains("09/2026") | not'
bad M31 '?month=2026-09-01'; badck M31 "$FMT"
bad M32 '?month=2026-09&month=2026-10'; badck M32 "$FMT"
bad M33 '?month=abc'; badck M33 "$FMT"
bad M34 '?month=%2B2026-09'; badck M34 "$FMT"

# J. Auth, method, media type, contract
Q="$R?month=2026-09"
curl -s "$Q" | jq -c 'del(.correlationId)' > "$S/bodies/M35.json"
curl -s "$Q" -H "Authorization: Bearer $EXPIRED" | jq -c 'del(.correlationId)' > "$S/bodies/M36.json"
curl -s "$Q" -H "Authorization: Bearer $FORGED" | jq -c 'del(.correlationId)' > "$S/bodies/M37.json"
ck M35 "no token 401 unauthorized" "$S/bodies/M35.json" '.status == 401 and .type == "urn:rikkaus:problem:unauthorized"'
jq -n --slurpfile a "$S/bodies/M35.json" --slurpfile b "$S/bodies/M36.json" '$a == $b' > "$S/bodies/M36eq.json"
jq -n --slurpfile a "$S/bodies/M35.json" --slurpfile b "$S/bodies/M37.json" '$a == $b' > "$S/bodies/M37eq.json"
ck M36 "expired token == no token" "$S/bodies/M36eq.json" '. == true'
ck M37 "forged token == no token" "$S/bodies/M37eq.json" '. == true'
for m in POST PUT DELETE; do
  curl -s -i -X $m "$Q" -H "Authorization: Bearer $A" | tr -d '\r' | awk 'NR==1{s=$2} tolower($1)=="allow:"{a=$2} END{printf "{\"status\":%s,\"allow\":\"%s\"}\n", s, a}' > "$S/bodies/M38$m.json"
  ck M38 "$m 405 Allow GET" "$S/bodies/M38$m.json" '.status == 405 and .allow == "GET"'
done
curl -s -o /dev/null -w '{"status":%{http_code}}' "$Q" -H "Authorization: Bearer $A" -H 'Accept: application/xml' > "$S/bodies/M39.json"
ck M39 "Accept xml 406" "$S/bodies/M39.json" '.status == 406'
jq -c '.paths["/api/v1/cash-flow-reports/monthly"] | {methods: keys, params: [.get.parameters[] | [.name, .required, .in]]}' "$ROOT/services/api/openapi/openapi.json" > "$S/bodies/M40a.json"
ck M40 "contract: only get, month required query" "$S/bodies/M40a.json" '.methods == ["get"] and .params == [["month", true, "query"]]'
docker exec rikkaus-pg-issue-77 psql -U rikkaus -d rikkaus -tAc "SELECT json_build_object('tables', (SELECT string_agg(tablename, ',' ORDER BY tablename) FROM pg_tables WHERE schemaname='wealth'), 'flyway', (SELECT max(version::int) FROM flyway_schema_history WHERE success))" > "$S/bodies/M40b.json"
ck M40 "schema unchanged, Flyway max 12" "$S/bodies/M40b.json" '.flyway == 12 and .tables == "asset_valuations,assets,cash_flow_entries,feedback_entries,goals,liabilities,onboarding_progress,refresh_tokens,user_settings,users"'

echo "TOTAL pass=$PASS fail=$FAIL"
```

## Chạy

```bash
bash "$S/setup.sh" && bash "$S/cases.sh"
```

Kết quả đúng là `TOTAL pass=60 fail=0` (40 case, 60 phép kiểm). `setup.sh` chỉ chạy được một lần trên một
database: lần chạy lại cần container mới (`docker rm -f rikkaus-pg-issue-77`, mục 1, khởi động lại API để
Flyway migrate).

Để xem tay một body, ví dụ tháng 09/2026 của A:

```bash
source "$S/env.sh"; curl -s 'http://127.0.0.1:8077/api/v1/cash-flow-reports/monthly?month=2026-09' -H "Authorization: Bearer $A" | jq 'del(.occurrences)'
```

## Dọn dẹp

Dừng API (Ctrl+C), rồi:

```bash
docker rm -f rikkaus-pg-issue-77
```
