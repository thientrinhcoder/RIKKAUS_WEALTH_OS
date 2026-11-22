# Manual test guide — search & basic filters

The search, filter and sort parameters of the four catalog lists (`GET /api/v1/assets`,
`/liabilities`, `/cash-flow-entries`, `/goals`, issue #101) ship with no automated test, by Product
Owner direction. This guide is the replacement: rerunning it is the only way a regression in them is
caught. The contract is described in
[`api-contract-conventions.md`](api-contract-conventions.md#search--basic-filters--implemented); the
full list of cases, with the data set and the expected result of each, is
[`plans/261122-1538-GH-101-search-basic-filters/phase-01-test-scenarios.md`](../plans/261122-1538-GH-101-search-basic-filters/phase-01-test-scenarios.md);
the recorded result of the implementing run is
[`plans/reports/manual-verification-261122-1538-GH-101-search-basic-filters.md`](../plans/reports/manual-verification-261122-1538-GH-101-search-basic-filters.md).

Nothing here needs a Google account, a deployed environment or the mobile app.

## Set up the run

A shell in the repository root, a Docker daemon, and JDK 25 (the system default is often 21, which
fails on `release 25`).

### 1. A disposable database

Its own name and port, so the run cannot touch a development database or another session's.

```bash
docker run -d --name rikkaus-pg-issue-101 -p 127.0.0.1:5401:5432 -e POSTGRES_DB=rikkaus -e POSTGRES_USER=rikkaus -e POSTGRES_PASSWORD=rikkaus-local-only postgres:18.6-alpine3.24
```

```bash
docker exec rikkaus-pg-issue-101 pg_isready -U rikkaus -d rikkaus
```

### 2. Two jars: `main` and the branch

Case X01 compares the parameterless lists of the branch with those of `main` on the same data, so
package both. From a checkout of `main`, then of the branch:

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./services/api/mvnw -f services/api/pom.xml package -DskipTests
```

Copy each `rikkaus-wealth-api-0.0.1-SNAPSHOT.jar` out of the Maven output folder under a name of its
own (for example `$WORK/api-main.jar` and `$WORK/api-branch.jar`), because the second package
overwrites the first. Start the `main` jar first:

```bash
DB_URL=jdbc:postgresql://localhost:5401/rikkaus /opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home/bin/java -jar "$WORK/api-main.jar" --spring.profiles.active=local --server.port=8101
```

Flyway must report `now at version v12`; the branch adds no migration.

### 3. Two users and their tokens

Insert user A (`0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93`) and user B
(`7b41e590-3c82-4f16-b0d7-59e1c4f3a842`) and mint four tokens — A, B, an expired one and one signed
with a different key — exactly as [`manual-test-guide-assets.md`](manual-test-guide-assets.md) steps 3
and 4 describe, with the container name above. The signing secret is the `local` profile's committed
development value in `services/api/src/main/resources/application-local.yml`; read it from there in
the minting script rather than pasting it anywhere. Keep the tokens in environment variables only and
never write one into a report:

```bash
export A=<token A> B=<token B> EXPIRED=<expired> FORGED=<forged> BASE=http://127.0.0.1:8101/api/v1
```

### 4. The data set

Create, through the API, every record in the phase-1 data tables: seven assets and two for B, four
liabilities and one for B, six cash-flow entries and one for B, three goals and one for B. Three
details matter:

- `Nhà phố Đà Lạt` must be sent **decomposed** (NFD), so the case that a stored NFD name still matches
  is real. In JSON: `"name":"Nhà phố Đà Lạt"`.
- `Vay mua nhà Vietcombank` takes `Căn hộ Thảo Điền`'s id as `collateralAssetId`, so create the asset
  first.
- `CF6` (`BUSINESS_INCOME`, 26.200.000 ₫ on 2026-09-10) has **no** notes: it is the record no search
  can match, while the enum filters still find it.

Two of the creates, as a pattern for the rest:

```bash
curl -s -H "Authorization: Bearer $A" -H 'Content-Type: application/json' -X POST "$BASE/assets" -d '{"name":"Căn hộ Thảo Điền","category":"REAL_ESTATE","currentValue":"6100000000.00","currency":"VND","valuationDate":"2026-08-02","liquidity":"LOW","institution":"Sổ hồng cá nhân"}'
```

```bash
curl -s -H "Authorization: Bearer $A" -H 'Content-Type: application/json' -X POST "$BASE/cash-flow-entries" -d '{"entryType":"EXPENSE","category":"EDUCATION","amount":"12500000.00","currency":"VND","timing":"ONE_TIME","effectiveDate":"2026-09-03","notes":"Học phí học kỳ 1 trường Quốc tế Á Châu"}'
```

### 5. The baseline, then the branch

With the `main` jar running, save the eight parameterless bodies:

```bash
for u in A B; do for r in assets liabilities cash-flow-entries goals; do eval t=\$$u; curl -s -H "Authorization: Bearer $t" "$BASE/$r" -o "$WORK/baseline-$u-$r.json"; done; done
```

Stop it, start the branch jar on the same port and database, and save the same eight bodies again
under another prefix. `cmp` each pair: every one must be identical. That is case X01.

## The cases

Send each request with `curl -s -i -G` and `--data-urlencode` for every parameter, so Vietnamese text,
spaces, `%` and `_` arrive exactly as typed:

```bash
curl -s -i -G -H "Authorization: Bearer $A" "$BASE/assets" --data-urlencode "q=thao dien"
```

```bash
curl -s -i -G -H "Authorization: Bearer $A" "$BASE/cash-flow-entries" --data-urlencode "q=hoc phi" --data-urlencode "entryType=EXPENSE" --data-urlencode "effectiveFrom=2026-09-01" --data-urlencode "effectiveTo=2026-09-30"
```

The one exception is `U+0000`, which must be sent already encoded:

```bash
curl -s -i -H "Authorization: Bearer $A" "$BASE/goals?q=vay%00mua"
```

Run every case of the phase-1 list, in its five groups: X (all four routes), AS, LB, CF and GL. Read
for each:

- **Lists.** The status is `200` and the records — by name, or by notes for cash-flow — are exactly the
  expected ones in the expected order. An empty result is `200 []`, never `404`.
- **Orders by name.** The expected order is whatever the same database returns for the documented
  `ORDER BY`, because the collation decides where `Đ` falls. For example, for AS28:
  `docker exec rikkaus-pg-issue-101 psql -U rikkaus -d rikkaus -Atc "select name from wealth.assets where user_id = '0f2c8d14-6b3a-4c7e-9f51-8ad2e6b40c93' order by name, valuation_date desc, id"`.
- **Rejections.** `400`, `application/problem+json`, type `urn:rikkaus:problem:validation-failed`, and
  the `errors` array is the assertion: each entry's `field` and `message`, in order. The body must
  not contain the rejected value (`GOLD`, `EUR`, the 121 characters).
- **Without a valid token.** `401`, type `urn:rikkaus:problem:unauthorized`, for no header, an expired
  token and a token signed with another key.
- **Ownership.** As B, the same searches return only B's records even where B's names are identical to
  A's (`Tài khoản Vietcombank`, `Vay mua nhà Vietcombank`, `Quỹ dự phòng 6 tháng`); compare the ids.

### Optional: the search text is never in the SQL

Start the branch jar with `--logging.level.org.hibernate.SQL=DEBUG
--logging.level.org.hibernate.orm.jdbc.bind=TRACE`, run a few searches, and check that no
`org.hibernate.SQL` line contains the text searched for, while the `jdbc.bind` lines show it as a
bound `VARCHAR` between two `%`. The statement itself reads
`lower(translate(?, …)) escape '\'` for the pattern.

## Clean up

Stop the API, then:

```bash
docker rm -f rikkaus-pg-issue-101
```
