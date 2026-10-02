# Backend deployment

How the API gets from a commit to a URL a reviewer can open, and what to do when it does not.

Scope is the Development environment: **AWS App Runner** for the API, **Neon** for PostgreSQL.
Production is a VPS running the same image and is not deployed from here.

Testing is not yet deployable. The ECR repository is shared by design — promotion means the same
image, so a repository per environment would make that impossible to express — and the GitHub OIDC
provider is an account-wide singleton, so `tofu apply -var environment=testing` in the same account
fails on both. Standing Testing up beside Development needs those two moved behind a data source or
a toggle first.

## What exists

| Piece | Where |
|---|---|
| Quality gate, image build, scan, push, deploy | [`.github/workflows/backend.yml`](../.github/workflows/backend.yml) |
| Container image | [`services/api/Dockerfile`](../services/api/Dockerfile) |
| AWS infrastructure | [`infra/aws/`](../infra/aws/) |
| The running service | [`infra/aws/apprunner.tf`](../infra/aws/apprunner.tf) |
| Formatting, test and coverage gates | [`services/api/pom.xml`](../services/api/pom.xml) |
| Secret-scanning rules | [`.gitleaks.toml`](../.gitleaks.toml) |
| Triaged infrastructure findings | [`infra/aws/.trivyignore.yaml`](../infra/aws/.trivyignore.yaml) |
| Dependency update policy | [`.github/dependabot.yml`](../.github/dependabot.yml) |

## Why this shape

The first version was ECS Fargate behind an Application Load Balancer behind a CloudFront
distribution, with RDS PostgreSQL. It was a textbook ECS environment and it cost roughly
`1.500.000 ₫` a month — for something that serves one or two people for a few hours on weekdays. The
load balancer alone was the largest line on the bill and the only part that could not be scaled to
zero.

The Product Owner rejected it on cost. This is the replacement:

| Concern | Then | Now |
|---|---|---|
| Compute | ECS Fargate, always on | App Runner, 0.25 vCPU / 0.5 GB, pausable |
| HTTPS | CloudFront + ALB | included, on `*.awsapprunner.com` |
| Database | RDS `db.t4g.micro` | Neon free plan, suspends after 5 minutes idle |
| Network | VPC, 4 subnets, 3 security groups | none |
| Cost | ~`1.500.000 ₫`/month | ~`50.000 ₫`/month paused outside office hours; ~`95.000 ₫` always on |

It is also the smaller system: the infrastructure scan went from thirteen findings to one, not by
exempting twelve but by deleting the resources they were about.

**0.25 vCPU / 0.5 GB was verified, not assumed.** The real image was run under
`--memory=512m --cpus=0.25` and answered `/actuator/health`. Raise `api_memory` to `"1024"` if
start-up ever fails on memory as the application grows.

## How a request reaches the API

```
browser ──HTTPS──> App Runner ──TLS──> Neon PostgreSQL
        (*.awsapprunner.com)   (public internet, sslmode=require)
```

Two things follow, both deliberate.

**There is no VPC.** Once the database moved to Neon there was no private resource left to reach, so
the VPC, four subnets, internet gateway, route table and three security groups were deleted, along
with the App Runner VPC connector that would have been needed to reach them.

**Database traffic crosses the public internet.** It is protected by TLS — `database_url` is
validated to require `sslmode=require` or stronger, so a URL without it fails the apply rather than
silently sending credentials in clear — but it does not stay inside a VPC. This environment holds no
real user data, and that is the condition under which the trade is acceptable. Production would not
make it; the architecture puts production on a VPS with PostgreSQL alongside it.

## The pipeline

A push to `main` touching `services/api/**`, `infra/aws/**` or the workflow itself runs three jobs
in order. A pull request runs only the first.

1. **verify** — Gitleaks over the branch's full history, then `./mvnw verify` in `services/api`
   (Spotless, compile, Surefire unit tests, Failsafe integration tests against a real PostgreSQL
   started by Testcontainers, a JaCoCo report per tier), then Trivy over the resolved dependencies,
   then Trivy over the OpenTofu. This job holds no AWS credential.
2. **publish** — builds the image, scans it with Trivy, pushes it to ECR tagged with the commit SHA.
   The scan runs against the locally built image before the push, so a vulnerable image never
   reaches the registry. If this commit's image is already in ECR it is reused rather than rebuilt,
   because tags are immutable and re-pushing one is refused.
3. **deploy** — calls `apprunner update-service` with the published digest, polls until the service
   reports `RUNNING`, then calls `/actuator/health` and `/api/v1/meta` on the public URL. A
   deployment that does not answer is a red build, not a surprise for whoever opens the page next.

A pull request cannot deploy for two independent reasons: `publish` and `deploy` carry an `if`
excluding `pull_request`, and the IAM trust policy admits only subjects it lists. Removing the `if`
would not be enough to break the rule.

The subject a run presents depends on the job. `publish` declares no environment and presents
`repo:<owner>/<name>:ref:refs/heads/main`. `deploy` declares the `development` environment, which
makes GitHub emit `repo:<owner>/<name>:environment:development` instead, carrying no branch at all —
both forms are therefore in the trust policy. What keeps `deploy` on an allowed branch is
`needs: publish`.

> **Development deploys from `main`.** The architecture's branch policy names `develop`, which was
> never created. `main` is the only push trigger and the only branch in the deploy role's trust
> policy. Adopting `develop` later means adding it in `backend.yml` and in the
> `github_deploy_branches` variable, and nowhere else.

## What is checked for security, and what is not

| Concern | Where | Runs on a pull request |
|---|---|---|
| Committed secrets and keys | Gitleaks in `verify`, full history | yes |
| Provider tokens at push time | GitHub secret scanning with push protection | n/a — blocks the push itself |
| Dependency vulnerabilities | Trivy filesystem scan in `verify` | yes |
| Container and OS vulnerabilities | Trivy image scan in `publish` | no — nothing is built on a PR |
| Infrastructure misconfiguration | Trivy config scan in `verify` | yes |
| Security defects in our own code | CodeQL default setup, `extended` suite | yes |
| Managed versions pinned below a fix | BOM property overrides in `services/api/pom.xml` | n/a — see the comment there |
| Vulnerable dependencies over time | Dependabot | n/a — opens pull requests |

**GitHub's secret scanning is narrower than its name suggests.** Push protection blocks recognised
provider tokens, but `secret_scanning_non_provider_patterns` is disabled on this repository, so a
high-entropy string with no vendor prefix is not matched. This project's most dangerous secret is
exactly that shape: `RIKKAUS_JWT_SECRET` is 64 random bytes, and whoever holds it can mint a valid
access token for any user. That is the gap [`.gitleaks.toml`](../.gitleaks.toml) closes, and its
rules were checked against planted canaries as well as the repository's real history.

**Static analysis belongs to GitHub, not to this workflow.** CodeQL default setup is configured on
this repository and analyses `java-kotlin`, `javascript-typescript` and `python`. An advanced CodeQL
job was written into `backend.yml` first and failed — the two configurations cannot coexist — so it
was removed. Because those checks live in a workflow GitHub owns, `publish` cannot list them under
`needs:`; requiring static analysis before a merge is a branch-protection rule on `main`, which is
the right place for it since `publish` runs after the merge.

**The infrastructure gate fails on any severity**, unlike the vulnerability gates. A LOW CVE is often
unfixable and arrives in numbers nobody can act on; a misconfiguration is finite, deterministic and
always somebody's choice in a file here. A new finding has two honest resolutions: change the
infrastructure, or add an entry to [`infra/aws/.trivyignore.yaml`](../infra/aws/.trivyignore.yaml)
stating what the check wants, why this environment does not do it, and what would change the answer.

### If the secret scan fails

Treat the credential as compromised the moment it is pushed, because the repository is public.
Rotate first, clean history second — a force-push does not un-publish anything already fetched or
indexed.

- `RIKKAUS_JWT_SECRET`: `tofu taint random_password.jwt_signing_key` then apply, which rotates the
  Parameter Store value and invalidates every issued token.
- The Neon password: reset the role's password in the Neon console, then re-apply with the new
  `TF_VAR_database_password`. Nothing in this stack can rotate it, because Neon is not an AWS
  resource.
- An AWS key: deactivate it in IAM before anything else.

If a finding is a documented example rather than a credential, add a narrow exemption. The existing
ones match on the declaring line rather than the value, so a real secret in the same file is still
caught; keep new ones that tight.

## First-time setup

Prerequisites: an AWS account with permission to create IAM, App Runner, ECR and SSM resources;
`tofu` and the `aws` CLI installed; `gh` authenticated against this repository.

> Use an IAM user or role, not root access keys. Root keys cannot be scoped and cannot be revoked per
> service, so a leak costs the whole account.

### 1. Create the Neon project

At [console.neon.tech](https://console.neon.tech), create a project in region **AWS Asia Pacific
(Singapore)** — the same region as the App Runner service, because a database on another continent
adds a round trip to every query. Create a database named `rikkaus`.

Neon shows a `postgresql://` connection string with the password inside it. **Do not paste that into
a file.** Take the host and database name and write the JDBC form; the password goes in the
environment. The stack validates that `database_url` carries no credentials and requires TLS, so a
mistake here fails the apply rather than leaking quietly.

### 2. Apply the infrastructure

```bash
cd infra/aws && cp development.tfvars.example development.tfvars && tofu init
```

Edit `development.tfvars`: set `database_url` and `database_username`. The file is git-ignored.

Supply the password through the environment, never a file:

```bash
export TF_VAR_database_password='<the Neon role password>'
```

```bash
cd infra/aws && tofu plan -var-file=development.tfvars
```

Read the plan before applying. It creates about fifteen resources, none of them a standing hourly
charge except the App Runner service's provisioned memory.

```bash
cd infra/aws && tofu apply -var-file=development.tfvars
```

**The first apply ends with the service unable to run, and that is expected.** It is created
pointing at a `latest` tag that does not exist in the repository, so its first deployment fails and
it settles with nothing running. Step 4 deploys a real image and it recovers.

**If the apply fails with `EntityAlreadyExists` on the OIDC provider**, the account already has a
GitHub provider from another repository. Adopt it rather than creating a second:

```bash
cd infra/aws && tofu import -var-file=development.tfvars aws_iam_openid_connect_provider.github "arn:aws:iam::$(aws sts get-caller-identity --query Account --output text):oidc-provider/token.actions.githubusercontent.com"
```

### 3. Hand the outputs to GitHub

```bash
cd infra/aws && gh variable set AWS_REGION --body "$(tofu output -raw aws_region)" && gh variable set ECR_REPOSITORY --body "$(tofu output -raw ecr_repository)" && gh variable set APPRUNNER_SERVICE_ARN --body "$(tofu output -raw apprunner_service_arn)" && gh variable set API_BASE_URL --body "$(tofu output -raw api_base_url)" && gh secret set AWS_DEPLOY_ROLE_ARN --body "$(tofu output -raw github_deploy_role_arn)"
```

Until `AWS_REGION` and `ECR_REPOSITORY` are set, `publish` and `deploy` skip themselves and only the
quality gate runs. That is deliberate: the pipeline is useful from the first commit and does not fail
red while the account is still being set up.

### 4. Deploy

```bash
gh workflow run Backend --ref main
```

```bash
gh run watch "$(gh run list --workflow Backend --branch main --limit 1 --json databaseId --jq '.[0].databaseId')"
```

### 5. Verify

```bash
cd infra/aws && curl -s "$(tofu output -raw health_check_url)" | jq
```

```bash
cd infra/aws && curl -s "$(tofu output -raw meta_url)" | jq
```

Expected:

```json
{ "groups": ["liveness", "readiness"], "status": "UP" }
{ "application": "rikkaus-wealth-api", "apiVersion": "v1",
  "buildVersion": "0.0.1-SNAPSHOT", "serverTime": "2026-10-02T15:42:26.393Z" }
```

Both endpoints are anonymous by design, so a QE engineer needs no credential and no VPN — a browser
is enough, and the URL is HTTPS so the browser will not warn. `buildVersion` and the workflow's run
summary together identify which commit is live.

The first request after a quiet spell is slower than the rest: Neon's compute suspends after five
minutes idle and takes a moment to resume. That is the mechanism that makes the database free.

Give QE the URL:

```bash
cd infra/aws && tofu output -raw api_base_url
```

## Running it cheaply

**Pause when nobody is reviewing.** A paused App Runner service bills nothing at all.

```bash
cd infra/aws && aws apprunner pause-service --service-arn "$(tofu output -raw apprunner_service_arn)" --region "$(tofu output -raw aws_region)"
```

```bash
cd infra/aws && aws apprunner resume-service --service-arn "$(tofu output -raw apprunner_service_arn)" --region "$(tofu output -raw aws_region)"
```

At 0.25 vCPU / 0.5 GB the service costs roughly `95.000 ₫` a month left running continuously, so
pausing saves about `45.000 ₫` a month. Decide whether that is worth remembering; leaving it running
is a legitimate choice.

**The database needs nothing.** Neon suspends its compute after five minutes without a connection
and charges nothing while suspended. That only works because the service is configured not to hold
idle connections open — `SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=0` and a short `IDLE_TIMEOUT`, set in
[`apprunner.tf`](../infra/aws/apprunner.tf). HikariCP's default is to hold ten connections open
forever, which Neon reads as continuous activity; the free plan's hundred CU-hours would be gone in
about four days. If the bill or the CU-hours ever look wrong, check those variables first.

## Routine operations

**Read the logs.**

```bash
cd infra/aws && aws logs tail "$(tofu output -raw log_group)" --follow
```

A second log group ending `/service` instead of `/application` carries App Runner's own deployment
events, which is where a failed deployment explains itself.

**Roll back.** Every deployed image is still in ECR under its commit SHA, so a rollback is a
deployment of an older one:

```bash
cd infra/aws && aws apprunner update-service --service-arn "$(tofu output -raw apprunner_service_arn)" --source-configuration "ImageRepository={ImageIdentifier=$(tofu output -raw ecr_repository_url):<previous-sha>,ImageRepositoryType=ECR}"
```

## Changing runtime configuration

Anything in the service's shape — `api_allowed_origins`, the Google parameters, CPU, memory — is
owned by OpenTofu, not by the pipeline. Change the variable and apply:

```bash
cd infra/aws && tofu apply -var-file=development.tfvars
```

App Runner redeploys the service itself when its configuration changes, keeping whatever image is
currently deployed, because `ignore_changes` covers the image identifier alone. No pipeline run is
needed.

## When something is wrong

**The service never reaches `RUNNING`.** Read the `/service` log group first for the deployment
event, then `/application` for the start-up. The usual cause is the application failing to start
rather than the health check being wrong, and the reason is in the first thirty lines.

**Flyway fails to validate.** A migration was edited after it had been applied. Checksums are
compared on every start-up and `clean-disabled` is true, so the fix is a new migration, never an edit
to an applied one.

**Connection failures to Neon.** Check `sslmode=require` is still in `database_url`, and that the
project has not been deleted for inactivity — see Known limitations. A suspended compute is not a
failure; it resumes on connection.

**The deploy job fails at `configure-aws-credentials`.** The OIDC subject did not match the trust
policy. `deploy` presents an `environment:` subject rather than a branch one, and both forms must be
listed — which [`iam.tf`](../infra/aws/iam.tf) handles.

## Known limitations

These are accepted for this environment, not oversights.

- **The hostname is opaque and not ours.** HTTPS works, but the address is a generated
  `*.awsapprunner.com` name. Fine for a QE engineer, wrong for a pilot user. App Runner supports
  custom domains; adopting one needs a domain and a DNS record, and `API_BASE_URL` plus every
  registered Google OAuth redirect URI change with it.
- **Database traffic crosses the public internet**, TLS-protected but outside a VPC. Acceptable only
  because this environment holds no real user data.
- **PostgreSQL is not on AWS.** The architecture names RDS for Development; this uses Neon, recorded
  as an amendment in `ARCHITECTURE_TECHNOLOGY_DECISIONS.md`. Production's system of record is
  unaffected.
- **Neon's free plan has limits that can bite.** One gigabyte of storage per project, a hundred
  CU-hours a month, and projects inactive for ninety days are subject to deletion. Ninety days is
  forgiving for a preview that sees weekly use, but it is not never: if this environment is mothballed
  for a quarter, expect to recreate the project and re-apply.
- **Cold start on the first request.** App Runner after a resume, and Neon after a suspend, both take
  a moment. That is the mechanism that makes the environment nearly free, not a defect.
- **State is local.** `tofu` writes `terraform.tfstate` next to the configuration and it contains the
  JWT signing key and the Neon password in plain text. It is git-ignored. Before a second person
  applies, move it to an S3 backend with locking.
- **No coverage floor.** JaCoCo reports both tiers and fails on neither. A threshold nobody agreed to
  gets satisfied by deleting assertions; agreeing one is a separate decision.
- **No browser smoke test.** The pipeline verifies the API's own endpoints. End-to-end browser checks
  need the frontend preview from issue #46.
- **Dependabot security updates are disabled at the repository level.** `.github/dependabot.yml` opens
  scheduled version bumps, but an out-of-cycle advisory raises nothing until the toggle in
  Settings → Code security is switched on.
- **One infrastructure finding is accepted**, recorded with its reasoning and an expiry in
  [`infra/aws/.trivyignore.yaml`](../infra/aws/.trivyignore.yaml).
