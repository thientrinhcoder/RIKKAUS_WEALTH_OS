# Backend deployment

How the API gets from a commit to a URL a reviewer can open, and what to do when it does not.

Scope is the Development environment on AWS. Production is a VPS running the same image and is not
deployed from here.

Testing is not yet deployable. The stack's ECR repository is shared by design — promotion means the
same image, so a repository per environment would make that impossible to express — and the GitHub
OIDC provider is an account-wide singleton, so `tofu apply -var environment=testing` in the same
account fails on both. Standing Testing up beside Development needs those two resources moved behind
a data source or a toggle first.

## What exists

| Piece | Where |
|---|---|
| Quality gate, image build, scan, push, deploy | [`.github/workflows/backend.yml`](../.github/workflows/backend.yml) |
| Container image | [`services/api/Dockerfile`](../services/api/Dockerfile) |
| AWS infrastructure | [`infra/aws/`](../infra/aws/) |
| HTTPS front end | [`infra/aws/cdn.tf`](../infra/aws/cdn.tf) |
| Formatting, test and coverage gates | [`services/api/pom.xml`](../services/api/pom.xml) |
| Secret-scanning rules | [`.gitleaks.toml`](../.gitleaks.toml) |
| Triaged infrastructure findings | [`infra/aws/.trivyignore.yaml`](../infra/aws/.trivyignore.yaml) |
| Dependency update policy | [`.github/dependabot.yml`](../.github/dependabot.yml) |

## The pipeline

A push to `main` that touches `services/api/**`, `infra/aws/**` or the workflow itself runs three
jobs in order. A pull request runs only the first.

1. **verify** — Gitleaks over the branch's full history, then `./mvnw verify` in `services/api`
   (Spotless formatting check, compile, Surefire unit tests, Failsafe integration tests against a
   real PostgreSQL started by Testcontainers, a JaCoCo report per tier), then Trivy over the
   resolved dependencies. Test and coverage reports are uploaded whether the job passes or fails.
   This job holds no AWS credential.
2. **publish** — builds the image, scans it with Trivy, and pushes it to ECR tagged with the commit
   SHA. The scan runs against the locally built image, before the push, so a vulnerable image never
   reaches the registry. A fixable HIGH or CRITICAL finding fails the job.
3. **deploy** — reads the OpenTofu-owned ECS task definition, replaces the `api` container's image with the
   digest just published, registers a new revision, rolls the service, waits for it to stabilise,
   and then calls `/actuator/health` and `/api/v1/meta` on the public URL. A deployment that does
   not answer is a red build, not a surprise for whoever opens the page next.

A pull request cannot deploy for two independent reasons: the `publish` and `deploy` jobs carry an
`if` that excludes `pull_request`, and the IAM trust policy admits only tokens whose subject it
lists. Deleting the `if` would not be enough to break the rule.

The subject a run presents depends on the job. `publish` declares no environment and presents
`repo:<owner>/<name>:ref:refs/heads/<branch>`, so the branch allowlist applies to it directly.
`deploy` declares the `development` environment, which makes GitHub emit
`repo:<owner>/<name>:environment:development` instead, carrying no branch at all — both forms are
therefore in the trust policy. What keeps `deploy` on an allowed branch is `needs: publish`: it
cannot run unless `publish` succeeded, and `publish` is admitted only on a listed branch.

> **Development deploys from `main`.** The architecture's branch policy names `develop`, but that
> branch was never created and this repository integrates on `main`, so the Product Owner chose
> `main` as the integration branch. It is the only push trigger and the only branch in the deploy
> role's trust policy. Adopting `develop` later means adding it in both places, which is
> `.github/workflows/backend.yml` and the `github_deploy_branches` variable.

## What is checked for security, and what is not

| Concern | Where | Runs on a pull request |
|---|---|---|
| Committed secrets and keys | Gitleaks in `verify`, full history | yes |
| Provider tokens at push time | GitHub secret scanning with push protection | n/a — blocks the push itself |
| Dependency vulnerabilities | Trivy filesystem scan in `verify` | yes |
| Container and OS vulnerabilities | Trivy image scan in `publish` | no — nothing is built on a PR |
| Security defects in our own code | CodeQL **default setup**, not this workflow | yes |
| Infrastructure misconfiguration | Trivy config scan in `verify` | yes |
| Vulnerable dependencies over time | Dependabot | n/a — opens pull requests |

Two limits are worth stating rather than assuming.

**GitHub's secret scanning is narrower than its name suggests.** Push protection blocks recognised
provider tokens — an AWS key, a GitHub PAT — but `secret_scanning_non_provider_patterns` is
disabled on this repository, so a high-entropy string with no vendor prefix is not matched. This
project's most dangerous secret is exactly that shape: `RIKKAUS_JWT_SECRET` is 64 random bytes, and
whoever holds it can mint a valid access token for any user. That is the gap
[`.gitleaks.toml`](../.gitleaks.toml) exists to close, and its rules were checked against planted
canaries in both the shapes that secret travels in — an environment assignment and the JSON
`name`/`value` pair an ECS task definition uses — as well as against the repository's real history.

**Static analysis belongs to GitHub, not to this workflow.** CodeQL default setup is configured on
this repository and analyses `java-kotlin`, `javascript-typescript` and `python`. An advanced CodeQL
job was written into `backend.yml` first and failed on the first pull request — the two configurations
cannot coexist — so it was removed. Default setup covers more languages and needs no workflow code;
what it gives up is the `extended` query suite, which is a setting on default setup rather than a
property of the analysis:

```bash
gh api -X PATCH repos/thientrinhcoder/RIKKAUS_WEALTH_OS/code-scanning/default-setup \
  -f query_suite=extended
```

Because those checks live in a workflow GitHub owns, `publish` cannot list them under `needs:`.
Requiring static analysis before a merge is a branch-protection rule on `main` — which is the right
place for it regardless, since `publish` runs after the merge and gating it was never the control
that mattered.

**The infrastructure gate fails on any severity, unlike the vulnerability gates.** A LOW or MEDIUM
CVE is often unfixable and arrives in numbers nobody can act on, so gating on it produces a check
that gets switched off. A misconfiguration is the opposite: finite, deterministic, and every one is
a choice somebody made in a file here. The existing findings are triaged to zero, which is what
makes the stricter setting affordable.

A new infrastructure finding has two honest resolutions: change the infrastructure, or add an entry
to [`infra/aws/.trivyignore.yaml`](../infra/aws/.trivyignore.yaml) stating what the check wants, why
this environment does not do it, and what would change the answer. Every existing entry carries that
statement and an expiry of **2027-04-01**, so none is permanent and all of them resurface together
at the next infrastructure review. An entry with no statement should fail review.

### If the secret scan fails

Treat the credential as compromised the moment it is pushed, because the repository is public.
Rotate first, clean history second — a force-push does not un-publish anything that was already
fetched or indexed.

- `RIKKAUS_JWT_SECRET`: `tofu taint random_password.jwt_signing_key` then apply, which rotates the
  Parameter Store value and invalidates every issued token.
- Database password: `tofu taint random_password.database` then apply.
- An AWS key: deactivate it in IAM before anything else.

If a finding is a documented example rather than a credential, add a narrow exemption to
`.gitleaks.toml`. The existing ones match on the declaring line rather than the value, so a real
secret in the same file is still caught; keep new ones that tight.

## How a request reaches the API

```
browser ──HTTPS──> CloudFront ──HTTP──> ALB ──HTTP──> Fargate task ──> RDS
          (*.cloudfront.net cert)   (CloudFront IPs only)   (ALB SG only)   (task SG only)
```

HTTPS is terminated at CloudFront, on its own `*.cloudfront.net` certificate. That is why there is
no ACM certificate and no domain anywhere in this stack: an ALB cannot serve HTTPS on its generated
hostname, because AWS owns `elb.amazonaws.com` and issues no certificate for it, and CloudFront's
default certificate is the only HTTPS that needs nothing bought or renewed.

The load balancer's own DNS name is **not** a usable address. Its security group admits only
CloudFront's published edge ranges, so a request from anywhere else times out. `tofu output -raw
alb_dns_name` exists for diagnosis, nothing more. That closure is what stops a bookmark, a script
or a reviewer from reaching this API in clear text — which matters from the moment issue #42 lands,
because a session token on a plain-HTTP hop is readable in transit.

The last two hops are HTTP, inside the VPC. Encrypting them would need a certificate on the load
balancer, which needs a domain, which is the option not taken. Because TLS ends at the edge, the
task runs with `SERVER_FORWARD_HEADERS_STRATEGY=framework` so the application honours
`X-Forwarded-Proto` and builds absolute URLs — an OAuth redirect above all — as `https`. That is
safe here precisely because nothing but CloudFront can reach the load balancer, so those headers
cannot be forged by a client.

## First-time setup

Prerequisites: an AWS account with permission to create IAM, VPC, RDS, ECS, ELB and ECR resources;
`tofu` and the `aws` CLI installed; `gh` authenticated against this repository.

### 1. Apply the infrastructure

```bash
cd infra/aws
cp development.tfvars.example development.tfvars   # edit; the real file is git-ignored
tofu init
tofu plan -var-file=development.tfvars
```

Read the plan before applying it. It creates roughly forty resources, and RDS and the load balancer
are the two that cost money whether or not anyone uses them.

```bash
tofu apply -var-file=development.tfvars
```

The first apply takes fifteen to twenty-five minutes. RDS and the CloudFront distribution are both
slow to create, and OpenTofu waits for the distribution to finish deploying so that when the apply
returns the URL genuinely works.

**The first apply ends with a failed deployment on record, and that is expected.** The service
starts on a placeholder image that answers nothing, so its target never becomes healthy, so the
deployment circuit breaker marks the deployment `FAILED`; with no earlier revision to return to,
the service settles at zero running tasks. `describe-services` therefore reads like a broken apply.
Step 3 starts a fresh deployment with a real image and the service recovers on its own.

**If the apply fails with `EntityAlreadyExists` on the OIDC provider**, the account already has a
GitHub provider from another repository. Adopt it rather than creating a second:

```bash
tofu import -var-file=development.tfvars \
  aws_iam_openid_connect_provider.github \
  "arn:aws:iam::$(aws sts get-caller-identity --query Account --output text):oidc-provider/token.actions.githubusercontent.com"
```

**If it fails on the PostgreSQL version**, the default major is not offered in your region. Ask
what is, and set `db_engine_version` accordingly:

```bash
aws rds describe-db-engine-versions --engine postgres \
  --query 'DBEngineVersions[].EngineVersion' --output text
```

### 2. Hand the outputs to GitHub

Every value the workflow needs is an output; none has to be found in the console.

```bash
cd infra/aws
gh variable set AWS_REGION      --body "$(tofu output -raw aws_region)"
gh variable set ECR_REPOSITORY  --body "$(tofu output -raw ecr_repository)"
gh variable set ECS_CLUSTER     --body "$(tofu output -raw ecs_cluster)"
gh variable set ECS_SERVICE     --body "$(tofu output -raw ecs_service)"
gh variable set API_BASE_URL    --body "$(tofu output -raw api_base_url)"
gh variable set BASE_TASK_DEFINITION_PARAMETER --body "$(tofu output -raw base_task_definition_parameter)"
gh secret   set AWS_DEPLOY_ROLE_ARN --body "$(tofu output -raw github_deploy_role_arn)"
```

Until `AWS_REGION` and `ECR_REPOSITORY` are set, `publish` and `deploy` skip themselves and only
the quality gate runs. That is deliberate: the pipeline is useful from the first commit and does
not fail red while the account is still being set up.

### 3. Deploy

```bash
gh workflow run Backend --ref main
gh run watch
```

Or merge anything into `main`. Either way the run ends by printing the verified URL.

### 4. Verify

```bash
curl -s "$(cd infra/aws && tofu output -raw health_check_url)" | jq
curl -s "$(cd infra/aws && tofu output -raw meta_url)" | jq
```

Expected:

```json
{ "groups": ["liveness", "readiness"], "status": "UP" }
{ "application": "rikkaus-wealth-api", "apiVersion": "v1",
  "buildVersion": "0.0.1-SNAPSHOT", "serverTime": "2026-09-29T11:46:54.956Z" }
```

Both endpoints are anonymous by design, so a QE engineer needs no credential and no VPN — a browser
is enough, and the URL is HTTPS so the browser will not warn. `buildVersion` and the workflow's run
summary together identify which commit is live.

The hostname is opaque, something like `https://d2x3k9abcdef.cloudfront.net`. That is fine for a QE
engineer and wrong for a pilot user; a readable address means adopting a domain, which is the
follow-up described under Known limitations.

## Changing runtime configuration

Anything in the task's shape — `api_allowed_origins`, the Google parameters, CPU, memory, the log
group — is owned by OpenTofu, not by the pipeline. Change the variable and apply:

```bash
cd infra/aws
tofu apply -var-file=development.tfvars
```

That registers a new base revision and updates the pointer the deploy job reads, so the change
reaches a running task on the next deployment. It does not roll the service by itself, because the
base revision still carries the placeholder image; deploy to pick it up:

```bash
gh workflow run Backend --ref main
```

The pipeline never copies the previously deployed revision, so a change applied here cannot be
silently carried over and lost.

## Routine operations

**Read the logs.**

```bash
aws logs tail "$(cd infra/aws && tofu output -raw log_group)" --follow
```

**Stop paying between test windows.** Fargate is billed per running task; the load balancer and RDS
are billed regardless.

```bash
aws ecs update-service --cluster <cluster> --service <service> --desired-count 0
```

Set `api_desired_count = 0` in `development.tfvars` if the environment will be idle for a while, so
the next `tofu apply` does not start it again. The service ignores `desired_count` drift precisely
so scaling it by hand is not undone by an unrelated apply.

**Roll back.** Every deployed image is still in ECR under its commit SHA, so a rollback is a
deployment of an older revision:

```bash
aws ecs update-service --cluster <cluster> --service <service> \
  --task-definition <family>:<previous-revision> --force-new-deployment
```

The service's deployment circuit breaker also rolls back on its own when new tasks never become
healthy, so a broken image does not leave the environment down.

## When something is wrong

**Targets never become healthy.** Read the log group. The usual cause is the application failing to
start rather than the health check being wrong, and the failure is in the first thirty lines.

**`ResourceInitializationError: unable to pull secrets or registry auth`.** The task execution role
cannot read a Parameter Store value — normally because a parameter was renamed, or the environment
prefix does not match. `aws_iam_role_policy.task_execution_secrets` in
[`infra/aws/iam.tf`](../infra/aws/iam.tf) scopes the grant to `/rikkaus/<environment>/*`.

**`no container named 'api' in …`.** The deploy job finds the container by that name. Renaming it
in `container_definitions` in [`infra/aws/service.tf`](../infra/aws/service.tf) without renaming it
in the workflow stops the match. The job fails rather than registering an unchanged revision, which
is deliberate: a green build that deployed nothing is worse than a red one.

**Flyway fails to validate.** A migration was edited after it had been applied. Checksums are
compared on every start-up and `clean-disabled` is true, so the fix is a new migration, never an
edit to an applied one.

## Known limitations

These are accepted for this environment, not oversights.

- **The hostname is opaque and not ours.** HTTPS works, but the address is a generated
  `*.cloudfront.net` name. Handing that to a pilot user is wrong, and the name changes if the
  distribution is ever destroyed and recreated. Adopting a domain means an ACM certificate issued
  **in us-east-1** — CloudFront accepts certificates from no other region — plus `aliases` on the
  distribution and a DNS record pointing at it. `API_BASE_URL` and every registered Google OAuth
  redirect URI change with it.
- **The internal hops are unencrypted.** CloudFront reaches the load balancer, and the load
  balancer the task, over plain HTTP inside the VPC. Encrypting the first hop needs a certificate
  on the ALB, which needs a domain. This is the accepted trade for HTTPS without one.
- **A distribution change takes about ten minutes.** Editing `cdn.tf` and applying is slow, and
  `tofu apply` waits it out. Deployments of the API do not touch the distribution and are
  unaffected.
- **Certificate renewal is AWS's problem, not yours** — which is the upside of the default
  certificate, and disappears the day a custom domain is adopted, because an ACM certificate
  renews only while its DNS validation record stays in place.
- **State is local.** `tofu` writes `terraform.tfstate` next to the configuration, and it contains
  the generated database password and JWT signing key in plain text. It is git-ignored. Before a
  second person runs an apply, move it to an S3 backend with locking — two divergent local states
  is how one operator destroys the other's environment.
- **No coverage floor.** JaCoCo reports both tiers and fails on neither. A threshold nobody agreed
  to gets satisfied by deleting assertions; agreeing one is a separate decision.
- **No browser smoke test.** The pipeline verifies the API's own endpoints. End-to-end browser
  checks need the frontend preview from issue #46.
- **Dependabot security updates are disabled at the repository level.** `.github/dependabot.yml`
  opens scheduled version-bump pull requests, but an out-of-cycle security advisory raises nothing
  until the toggle in Settings → Code security is switched on. That setting is the repository
  owner's to change.
- **Twelve infrastructure findings are accepted, not fixed.** Each is recorded with its reasoning
  and an expiry in [`infra/aws/.trivyignore.yaml`](../infra/aws/.trivyignore.yaml). The ones most
  worth knowing: there is no WAF on the distribution, no CloudFront access logging, and no VPC flow
  logs — all cost decisions taken against an environment with no user data, and all of which should
  be re-asked before a pilot user is given the URL.
