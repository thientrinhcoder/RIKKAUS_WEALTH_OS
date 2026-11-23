# Manual test guide — deletion impact and linked-record confirmation

The deletion-impact routes and the `linked-records` guard (issue #104) ship with no automated test, by
Product Owner direction. This guide is the replacement: rerunning it is how a regression is caught. The
scenario list is
[`plans/261122-1842-GH-104-safe-edit-delete/phase-01-test-scenarios.md`](../plans/261122-1842-GH-104-safe-edit-delete/phase-01-test-scenarios.md)
and the recorded run is
[`plans/reports/manual-verification-261122-1842-GH-104-safe-edit-delete.md`](../plans/reports/manual-verification-261122-1842-GH-104-safe-edit-delete.md).

The contract under test is *Deletion impact and linked-record confirmation* in
[`api-contract-conventions.md`](api-contract-conventions.md).

## Set up the run

A shell in the repository root, a Docker daemon, `jq`, and JDK 25 (`JAVA_HOME` below; the system default
is often 21 and fails on `release 25`).

### 1. A disposable database

Its own name and port, so a run cannot touch another database:

```bash
docker run -d --name rikkaus-pg-issue-104 -p 127.0.0.1:5404:5432 -e POSTGRES_DB=rikkaus -e POSTGRES_USER=rikkaus -e POSTGRES_PASSWORD=rikkaus-local-only postgres:18.6-alpine3.24
```

```bash
pg() { docker exec -i rikkaus-pg-issue-104 psql -U rikkaus -d rikkaus -tA -F'|' -c "$1"; }
```

### 2. The packaged application

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./services/api/mvnw -f services/api/pom.xml clean verify
```

Run the jar that produced (copy it out of the module's output directory first if you like):

```bash
DB_URL=jdbc:postgresql://localhost:5404/rikkaus /opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home/bin/java -jar <the jar> --spring.profiles.active=local --server.port=8104 > api.log 2>&1
```

Flyway must report `Successfully applied 11 migrations ... now at version v14`: this issue adds no
migration.

### 3. Two users and their tokens

Insert users A (`0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93`) and B (`7b41e590-3c82-4f16-b0d7-59e1c4f3a842`) and
mint the four tokens (`A`, `B`, `EXPIRED`, `FORGED`) exactly as steps 3 and 4 of
[`manual-test-guide-assets.md`](manual-test-guide-assets.md) do, with the container name above. Then:

```bash
api() { curl -s -w '\n%{http_code}\n' -X "$2" ${1:+-H "Authorization: Bearer $1"} -H 'Content-Type: application/json' "http://127.0.0.1:8104/api/v1$3" ${4:+--data-binary "$4"}; }
```

```bash
id() { jq -r .id; }
```

### 4. The data set

The table in phase 1 of the plan, created through the API. Each line prints the new id; keep it in the
variable named on the left.

```bash
AS1=$(api "$A" POST /assets '{"name":"Căn hộ Thảo Điền","category":"REAL_ESTATE","currentValue":"4050000000.00","currency":"VND","valuationDate":"2026-05-10","liquidity":"LOW"}' | head -1 | id)
```

```bash
api "$A" POST /assets/$AS1/valuations '{"value":"4120000000.00","currency":"VND","effectiveDate":"2026-08-02","note":"Định giá lại theo giá giao dịch cùng tòa."}'
```

```bash
api "$A" POST /assets/$AS1/valuations '{"value":"4200000000.00","currency":"VND","effectiveDate":"2026-11-15","note":"Định giá ngân hàng khi tái thẩm định khoản vay."}'
```

The other assets the same way: `AS2` `Tài khoản Vietcombank` `BANK_ACCOUNT` `85000000.00` `HIGH`; `AS3`
`Sổ tiết kiệm BIDV 12 tháng` `TERM_DEPOSIT` `300000000.00`; `AS4` `Xe Honda SH 160i` and `AS5`
`Vàng SJC 5 lượng`, both `VEHICLE_OR_VALUABLE_ASSET`; `ASB1` `Nhà phố Thảo Điền` with token `B`.

```bash
LB1=$(api "$A" POST /liabilities "{\"name\":\"Vay mua nhà Vietcombank\",\"category\":\"BANK_LOAN\",\"outstandingBalance\":\"2400000000.00\",\"currency\":\"VND\",\"interestRate\":\"8.5\",\"recurringPaymentAmount\":\"28500000.00\",\"recurringPaymentFrequency\":\"MONTHLY\",\"nextPaymentDate\":\"2026-12-05\",\"collateralAssetId\":\"$AS1\",\"notes\":\"Thế chấp căn hộ Thảo Điền.\"}" | head -1 | id)
```

`LB2` `Thẻ tín dụng Techcombank Visa` `CREDIT_CARD_BALANCE` with no collateral; `LBB1` for B with
`collateralAssetId` `ASB1`.

```bash
CF1=$(api "$A" POST /cash-flow-entries "{\"entryType\":\"INCOME\",\"category\":\"RENTAL_INCOME\",\"amount\":\"18000000.00\",\"currency\":\"VND\",\"timing\":\"RECURRING\",\"recurrence\":\"MONTHLY\",\"effectiveDate\":\"2026-01-01\",\"linkedAssetId\":\"$AS1\",\"notes\":\"Cho thuê căn hộ Thảo Điền\"}" | head -1 | id)
```

And `CF2` (`HOUSING`, `Phí quản lý căn hộ Thảo Điền`, linked to `AS1`), `CF3`
(`DEBT_PRINCIPAL_PAYMENT`, `Trả góp vay mua nhà Vietcombank`, `linkedLiabilityId` `LB1`), `CF4`
(`DEPOSIT_INTEREST`, one-time, linked to `AS3`), `CF5` (`EDUCATION`, one-time 2026-09-03, no link), `CF6`
(`INSURANCE`, yearly, linked to `AS4`), `CF7` (`OTHER_INCOME`, linked to `AS5`), `CFB1` for B linked to
`ASB1`; goals `G1` `Quỹ học đại học cho con`, `G2` `Du lịch Nhật Bản`, `GB1` for B.

## The cases

Run them in this order: the safe-edit cases change the data the guard cases then delete. Every error
is `application/problem+json`; read the `type`, not only the status.

### Preview

```bash
api "$A" GET /assets/$AS1/deletion-impact
```

`200`, `confirmationRequired: true`, three `linkedEffects` in this order: `ASSET_VALUATION` `DELETED`
count 3; `LIABILITY` `UNLINKED` `collateralAssetId` listing `Vay mua nhà Vietcombank`; `CASH_FLOW_ENTRY`
`UNLINKED` `linkedAssetId` listing `Cho thuê căn hộ Thảo Điền` and `Phí quản lý căn hộ Thảo Điền`.

- `AS2`: one effect, `ASSET_VALUATION` count 1, and `confirmationRequired: false`.
- `/liabilities/$LB1/deletion-impact`: `CASH_FLOW_ENTRY` `linkedLiabilityId` listing CF3, `true`.
  `/liabilities/$LB2/...`: both lists empty, `false`.
- `/cash-flow-entries/$CF1/...` and `$CF5`: `linkedEffects: []`, `derivedEffects` `CASH_FLOW_OCCURRENCES`,
  `MONTHLY_CASH_FLOW_REPORTS`, `CASH_FLOW_FORECAST`, `false`.
- `/goals/$G1/...`: both empty, `false`.
- Afterwards `pg "SELECT count(*) FROM wealth.asset_valuations WHERE asset_id='$AS1'"` is still `3`.

### Ownership, authentication, method

- With token `A` on `ASB1`, `LBB1`, `CFB1`, `GB1`, and on a random UUID: all `404 not-found`, and
  the bodies are identical once `instance` and `correlationId` are removed (`jq -c 'del(.instance,
  .correlationId)'`).
- `api "$A" DELETE "/assets/$ASB1?confirm=true"` is `404`; B's asset, `LBB1`'s collateral and `CFB1`'s link
  are unchanged in `pg`. `api "$A" DELETE /liabilities/$LBB1` is `404`, never `409`.
- No token, the expired one, the forged one: `401 unauthorized`. A `DELETE ...?confirm=true` without a
  token deletes nothing.
- `POST` or `DELETE` on a `deletion-impact` path: `405 method-not-allowed`. A non-UUID id: `400
  malformed-request`. `DELETE /assets/$AS1?confirm=co-le`: `400 malformed-request`, nothing deleted.

### Editing keeps links

- `PUT /liabilities/$LB1` with the body `GET` returned, `collateralAssetId` still `AS1`, and a new name:
  `200`, the collateral is kept, and the asset's preview lists the new name.
- `PUT /cash-flow-entries/$CF1` with `linkedAssetId` resent and amount `19000000.00`: link kept.
- `PUT /assets/$AS1` renaming it `Căn hộ Thảo Điền (Masteri)`: `200`; in `pg`, `LB1` and two entries still
  point at it, and the preview counts are still 3, 1, 2.
- `PUT /cash-flow-entries/$CF6` **without** `linkedAssetId`: `200` with `linkedAssetId: null`. That is
  the documented replacement semantics, not a regression; `AS4`'s preview then shows only its valuation.

### The guard

```bash
api "$A" DELETE /assets/$AS1
```

`409`, `type` `urn:rikkaus:problem:linked-records`, title `Linked records would change`, and an `impact`
member equal to the preview body. In `pg`, the asset, its three valuations, `LB1`'s collateral and both
entries' links are all still there. `?confirm=false` answers the same. `DELETE /liabilities/$LB1` is
`409` with CF3 in `impact`.

Records with nothing unlinked delete straight away with `204`: `AS2` (then `0` valuations for it in `pg`),
`LB2`, `CF5` (and `GET /cash-flow-occurrences?from=2026-09-01&to=2026-09-30` no longer has an item whose
`sourceEntryId` is CF5, where it had one before), `G1`, and `AS4` after its entry was unlinked.

```bash
api "$A" DELETE "/assets/$AS1?confirm=true"
```

`204`. Then read the database directly:

```bash
pg "SELECT (SELECT count(*) FROM wealth.assets WHERE id='$AS1'), (SELECT count(*) FROM wealth.asset_valuations WHERE asset_id='$AS1'), (SELECT collateral_asset_id FROM wealth.liabilities WHERE id='$LB1'), (SELECT count(*) FROM wealth.cash_flow_entries WHERE id IN ('$CF1','$CF2') AND linked_asset_id IS NULL)"
```

`0|0||2`: the asset and its history are gone (`CASCADE`), `LB1` survives with no collateral and both
entries survive unlinked (`SET NULL`). A second confirmed delete is `404`. `DELETE
/liabilities/$LB1?confirm=true` is `204` and leaves CF3 with `linked_liability_id` `NULL`.
`DELETE /cash-flow-entries/$CF1?confirm=true` is `204`: the parameter is ignored there.

### Concurrency

```bash
for i in 1 2 3 4; do curl -s -o /dev/null -w '%{http_code}\n' -X DELETE -H "Authorization: Bearer $A" "http://127.0.0.1:8104/api/v1/assets/$AS3?confirm=true" & done; wait
```

Exactly one `204` and three `404`, never `500`; `CF4` survives unlinked. Without `confirm` on `AS5`: four
`409`, and `AS5` is still there. Four deletes of `G2`: one `204`, three `404`.

### Contract and log

- `GET http://127.0.0.1:8104/v3/api-docs` lists the four `.../deletion-impact` paths, a `confirm` parameter
  and a `409` on the asset and liability `DELETE`, `urn:rikkaus:problem:linked-records` in the
  `ProblemDetail` `type` enum and an `impact` property.
- `grep -c -E 'Căn hộ Thảo Điền|Vay mua nhà|confirm=' api.log` is `0`: no record label and no query
  string reaches the log.

## Clean up

Stop the application, then:

```bash
docker rm -f rikkaus-pg-issue-104
```
