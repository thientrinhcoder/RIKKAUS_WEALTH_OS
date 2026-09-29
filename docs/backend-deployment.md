# Backend deployment

How the API gets from a commit to a URL a reviewer can open, and what to do when it does not.

Scope is the Development environment on AWS. Testing reuses the same stack with
`-var environment=testing`; Production is a VPS running the same image and is not deployed from
here.

## What exists

| Piece | Where |
|---|---|
| Quality gate, image build, scan, push, deploy | [`.github/workflows/backend.yml`](../.github/workflows/backend.yml) |
| Container image | [`services/api/Dockerfile`](../services/api/Dockerfile) |
| AWS infrastructure | [`infra/aws/`](../infra/aws/) |
| Formatting, test and coverage gates | [`services/api/pom.xml`](../services/api/pom.xml) |

## The pipeline

A push to `main` or `develop` that touches `services/api/**`, `infra/aws/**` or the workflow itself
runs three jobs in order. A pull request runs only the first.

1. **verify** — `./mvnw verify` in `services/api`: Spotless formatting check, compile, Surefire
   unit tests, Failsafe integration tests against a real PostgreSQL started by Testcontainers, and
   a JaCoCo report per tier. Test and coverage reports are uploaded as a build artifact whether the
   job passes or fails. This job holds no AWS credential.
2. **publish** — builds the image, scans it with Trivy, and pushes it to ECR tagged with the commit
   SHA. The scan runs against the locally built image, before the push, so a vulnerable image never
   reaches the registry. A fixable HIGH or CRITICAL finding fails the job.
3. **deploy** — reads the live ECS task definition, replaces the `api` container's image with the
   digest just published, registers a new revision, rolls the service, waits for it to stabilise,
   and then calls `/actuator/health` and `/api/v1/meta` on the public URL. A deployment that does
   not answer is a red build, not a surprise for whoever opens the page next.

A pull request cannot deploy for two independent reasons: the `publish` and `deploy` jobs carry an
`if` that excludes `pull_request`, and the IAM role's trust policy admits only a token whose
subject is `repo:<owner>/<name>:ref:refs/heads/main` or `…/develop`. Deleting the `if` would not be
enough to break the rule.

> **`develop` does not exist yet.** The architecture's branch policy deploys Development from
> `develop`; this repository currently integrates on `main`, so both are wired as triggers and as
> permitted OIDC subjects. Creating `develop` later needs no change here.

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

The first apply takes about ten minutes, nearly all of it waiting for RDS.

The service comes up running a placeholder image and its targets will be unhealthy until the first
real deployment. That is expected; step 3 fixes it.

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
is enough. `buildVersion` and the workflow's run summary together identify which commit is live.

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

**The deploy job succeeds but the version does not change.** The job finds the container by the name
`api`. If `container_definitions` in [`infra/aws/service.tf`](../infra/aws/service.tf) is renamed,
the `jq` filter in the workflow matches nothing and registers an unchanged revision.

**Flyway fails to validate.** A migration was edited after it had been applied. Checksums are
compared on every start-up and `clean-disabled` is true, so the fix is a new migration, never an
edit to an applied one.

## Known limitations

These are accepted for this environment, not oversights.

- **No HTTPS.** The listener is plain HTTP on the load balancer's AWS DNS name. Traffic is
  unencrypted, so nothing real should be sent here. It also means two things do not work yet:
  Google refuses a plain-HTTP redirect URI, so the sign-in from issue #42 cannot be exercised
  through this environment; and a browser page served over HTTPS will block a call to it as mixed
  content, which the Expo web preview from issue #46 will hit. Fixing it is an ACM certificate, a
  443 listener and one security-group rule.
- **State is local.** `tofu` writes `terraform.tfstate` next to the configuration, and it contains
  the generated database password and JWT signing key in plain text. It is git-ignored. Before a
  second person runs an apply, move it to an S3 backend with locking — two divergent local states
  is how one operator destroys the other's environment.
- **No coverage floor.** JaCoCo reports both tiers and fails on neither. A threshold nobody agreed
  to gets satisfied by deleting assertions; agreeing one is a separate decision.
- **No browser smoke test.** The pipeline verifies the API's own endpoints. End-to-end browser
  checks need the frontend preview from issue #46.
