---
phase: 5
title: "Security scanning in the pipeline"
status: done
issue: 47
dependencies: [4]
---

# Phase 5: Security scanning in the pipeline

The Product Owner asked what in CI verified code security, vulnerabilities and key leaks. The
honest answer was "one of the three, in the wrong place", and this phase is the repair.

## What was actually there

| Concern | Before |
|---|---|
| Container and dependency vulnerabilities | Trivy, but only in `publish` — which never runs on a pull request |
| Static analysis of our own source | nothing |
| Secret scanning in the pipeline | nothing |
| Provider tokens at push time | GitHub secret scanning and push protection, both enabled |
| Vulnerable dependency alerts | Dependabot security updates disabled, no `dependabot.yml` |

Two of those readings were more misleading than a plain "no" would have been.

**The vulnerability scan could not see a pull request.** Trivy lived in `publish`, which carries
`if: github.event_name != 'pull_request'`. A change introducing a dependency with a known CVE
therefore passed every check, merged, and failed only on the push to `main` — at which point the
remedy is a revert rather than a review comment. The architecture puts the scan alongside the tests
precisely to avoid this (`ARCHITECTURE_TECHNOLOGY_DECISIONS.md`, required stage 5).

**GitHub's secret scanning stops where this project's exposure starts.** Push protection blocks
recognised provider tokens, but `secret_scanning_non_provider_patterns` is disabled, so a
high-entropy string with no vendor prefix is not matched. `RIKKAUS_JWT_SECRET` is 64 random bytes
and whoever holds it can mint a valid access token for any user. Nothing would have stopped it being
committed.

## What was added

Four things, all free on a public repository.

**Trivy moved into `verify`** as a filesystem scan over `services/api`, placed after the Maven build
so it sees the resolved artifact and not only the declared dependencies. The image scan stays in
`publish`, because the two answer different questions: one covers what we depend on, the other the
OS packages in the base layer.

**Gitleaks in `verify`**, over the branch's full history rather than the working tree — a secret
added in one commit and removed in the next is still in the branch and still published the moment
it is pushed, and the default shallow checkout would not see it. It runs before anything is
compiled, because a leaked credential is not made less leaked by a passing test suite.

**CodeQL `security-extended`** over the Java source, as a job beside `verify` rather than after it,
since neither depends on the other. Trivy answers whether a library has a known CVE; this answers
what our own code does with it. `publish` now needs both jobs, so nothing is published from a commit
whose source analysis has not run.

**`.github/dependabot.yml`** for `maven`, `npm` and `github-actions`, with Spring Boot's modules
grouped because the parent POM versions them together and one-at-a-time bumps cannot pass on their
own. It also covers `apps/mobile`, which the backend pipeline never scans and which has no pipeline
of its own until issue #46.

## The secret rules were written against evidence, not guesswork

A rule set that has never fired is indistinguishable from one that cannot. The first run of Gitleaks
over the real history returned **five findings**, every one a documented example: `@Schema(example =
...)` annotations on `SessionResponse`, the generated OpenAPI contract, the API conventions document
and a design plan, all from the unmerged issue #42 branch. Real, and not secrets.

Each exemption in `.gitleaks.toml` was then narrowed until it covered the example and nothing else:

- Example tokens are matched **on the declaring line** — a line containing `@Schema(... example`,
  `example =` or `"example":` — so a real credential pasted elsewhere in the same file still fails.
- The Markdown exemption requires **both** a `.md` path and a line declaring an `accessToken` or
  `refreshToken` field, using `condition = "AND"`.
- The committed local credentials are exempted by their literal values — `rikkaus-local-only`,
  `local-development-only-jwt-signing-secret` — not by exempting the files that hold them.

After that, the full history was clean. Then the rules were proved in the other direction with
planted canaries:

| Planted | Result |
|---|---|
| `RIKKAUS_JWT_SECRET=<64 chars>` in a `.env` | caught by `rikkaus-jwt-signing-key` |
| `{"name":"RIKKAUS_JWT_SECRET","value":"<44 chars>"}` | **missed on the first attempt**, and by every default rule too |
| `jdbc:postgresql://user:password@host` | caught by `jdbc-url-with-inline-password` |
| `POSTGRES_PASSWORD=<literal>` | caught by `postgres-password-literal` |
| `RIKKAUS_JWT_SECRET=${RIKKAUS_JWT_SECRET:}` and an empty value | correctly ignored |

The miss is the useful one. The JSON `name`/`value` pair is exactly the shape of an ECS task
definition — the file the deploy job itself writes as `taskdef.json` — and the original regex only
allowed twelve characters between the key and the value, which `","value":"` exceeds. The rule was
widened and re-tested until both shapes fire and environment references still do not.

## Verification

Everything here was run, not reasoned about:

- Gitleaks v8.30.1 against a full clone of the repository, all 29 commits across every branch:
  five findings, triaged, then clean after narrowing the exemptions.
- Canaries planted and removed, confirming three rules fire and references do not.
- `./mvnw -B -DskipTests -Dspotless.check.skip=true compile`, the CodeQL build step, succeeds.
- The workflow YAML parses and the job graph is as intended: `publish` needs `verify` and `codeql`.

**CodeQL itself has not run.** Whether its Java extractor handles this project's Java 25 sources is
unproven here and the first pull request is what will prove it. If it fails, the fallbacks in order
are `build-mode: none`, or dropping the job and recording the gap.

## Infrastructure misconfiguration, added and triaged

Enabled after the rest, on the Product Owner's instruction. The first scan of `infra/aws` returned
**thirteen findings**. Two were defects and were fixed; eleven were decisions and were recorded.

**AWS-0052, invalid header fields.** Fixed. `drop_invalid_header_fields = true` on the load
balancer. Two proxies sit in front of the application, and request smuggling is what happens when
two of them disagree about where one request ends and the next begins. No cost, nothing legitimate
rejected.

**AWS-0104, unrestricted egress.** Fixed in substance. The task security group allowed every
protocol to every address, justified by the fact that ECR, CloudWatch and Google have no fixed
addresses — true of the addresses, and not of the ports, since all three are HTTPS. It is now TCP
443 outbound, DNS scoped to the VPC range, and PostgreSQL scoped to the database security group.

That one is worth dwelling on, because **the check still fires**: it flags any `0.0.0.0/0`
destination regardless of port. The finding persisting does not mean nothing changed — a compromised
task no longer has a free outbound channel on any port, which is the path data leaves by. Removing
the finding entirely would mean VPC interface endpoints for ECR, S3 and CloudWatch at roughly seven
US dollars each per availability zone, and Google's JWKS endpoint would still require 443 outbound
afterwards, so the spend would not even buy the clean result. It is recorded as accepted with that
arithmetic written down.

The remaining eleven are in `infra/aws/.trivyignore.yaml`, each with a statement naming what the
check wants, why this environment does not do it, and what would change the answer:

| Finding | Why accepted |
|---|---|
| AWS-0054 plain HTTP listener | Structural: an ALB cannot serve HTTPS on its generated hostname. Only CloudFront reaches it |
| AWS-0053 internet-facing load balancer | True in the AWS sense, not the reachable sense. CloudFront VPC origins would remove it |
| AWS-0011 no WAF | A standing cost against an environment whose whole surface is three anonymous endpoints |
| AWS-0010 no CloudFront access logs | Needs a bucket; the application's own correlation-tagged logs cover diagnosis at this stage |
| AWS-0176 no RDS IAM auth | Would make the application depend on the AWS SDK to start, breaking the VPS portability the architecture requires |
| AWS-0177 no deletion protection | This environment is meant to be destroyed and rebuilt; Production is not deployed from this stack |
| AWS-0178 no VPC flow logs | Detects unexpected traffic, a question for an environment carrying real data |
| AWS-0017, AWS-0033 AWS-managed keys not CMKs | Logs carry no secrets by rule; image layers carry none by construction |
| AWS-0034 Container Insights off | Explicit cost decision, in an environment meant to scale to zero |
| AWS-0133 Performance Insights off | Profiles queries, on a database with one baseline migration and no domain tables |

Every entry expires on **2027-04-01**, so all twelve decisions resurface together at the next
infrastructure review rather than quietly becoming permanent.

**The gate fails on any severity**, which is stricter than the two vulnerability scans and
deliberately so. A LOW CVE is often unfixable and arrives in numbers; a misconfiguration is finite,
deterministic, and always somebody's choice in a file here. Triaging the existing ones to zero is
what makes the stricter setting affordable.

It was then proved to be a gate rather than a suppression file. Planted canaries — a publicly
accessible RDS instance, an unencrypted one, a security group opening SSH to the world, and a bare
S3 bucket — produced thirteen new findings across four rules, none of them masked by the existing
entries, because each entry is scoped to one rule identifier in one file.

## What is still not covered
- **Dependabot security updates are off at the repository level.** The committed file opens
  scheduled version bumps, but an out-of-cycle advisory raises nothing until the toggle in
  Settings → Code security is switched on. That is the repository owner's change to make, not one
  this branch can or should make for them.
- **`apps/mobile` has no pipeline.** Dependabot watches its dependencies; nothing lints, tests or
  statically analyses it. That is issue #46.
