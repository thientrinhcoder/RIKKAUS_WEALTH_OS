---
issue: 47
title: "Backend CI/CD quality gates and AWS ECS preview baseline"
status: in-progress
branch: thientrinhcoder/feat/issue-47-backend-cicd-aws-ecs
---

# Issue #47 — Backend CI/CD quality gates and AWS ECS preview baseline

## Outcome

Every pull request that touches `services/api` is validated automatically without touching a shared
environment, and the integration branch publishes an immutable image that runs as an ECS Fargate
service reachable from the public internet, so the Product Owner and a QE engineer can verify the
running API from a browser or `curl` without a local checkout.

## Accepted decisions

Three decisions were put to the Product Owner before implementation began, because each one changes
what gets built and one of them contradicts a recorded architecture decision.

| Decision | Accepted answer | Consequence |
|---|---|---|
| CI runner | **GitHub Actions** | `ARCHITECTURE_TECHNOLOGY_DECISIONS.md` is amended; no `Jenkinsfile` is written |
| AWS provisioning | **Product Owner applies the OpenTofu** | This branch authors `infra/aws/` and a runbook; it creates no billed resource and verifies no live URL |
| Public entry point | **App Runner's own HTTPS endpoint** | No domain, no certificate, no load balancer and no CDN; the hostname is a generated `*.awsapprunner.com` name |
| Compute and database | **App Runner + Neon, chosen on cost** | The ECS/ALB/CloudFront/RDS build was rejected by the Product Owner at ~`1.500.000 ₫`/month and replaced at ~`50.000 ₫`; see phase 6 |
| Deploy branch | **`main`** | The architecture's `develop` was never created; `main` is the only push trigger and the only branch the deploy role trusts |

## Constraints

- Pull requests validate only. No workflow triggered by `pull_request` may hold AWS credentials or
  reach a shared environment.
- One image, built once, promoted by digest. A later environment must never rebuild from source.
- No secret may be written to a workflow log or baked into an image layer.
- PostgreSQL is never publicly reachable.
- No fake data, no skipped test, and no weakened check to make a gate pass.

## Non-goals

- Frontend CI and the Expo web preview, which are issue #46.
- Testing and Production environments, their promotion approvals, and version-tag releases.
- A custom domain and an ACM certificate. HTTPS itself is delivered, terminated at the edge; a readable hostname is not.
- Browser smoke tests, which need the frontend preview from #46.

## Phases

| Phase | Title | Record | Status |
|---|---|---|---|
| 1 | Backend quality gates in the Maven build | [phase-01](phase-01-backend-quality-gates.md) | done |
| 2 | Immutable image and the pipeline | [phase-02](phase-02-immutable-image-and-pipeline.md) | done |
| 3 | AWS baseline as OpenTofu, and the operator runbook | [phase-03](phase-03-aws-baseline-and-runbook.md) | done |
| 4 | HTTPS at the edge, and `main` as the deploy branch | [phase-04](phase-04-https-and-main-only-deploys.md) | done |
| 5 | Security scanning in the pipeline | [phase-05](phase-05-security-scanning.md) | done |
| 6 | Cost rebuild on App Runner and Neon | [phase-06](phase-06-cost-rebuild-on-app-runner-and-neon.md) | done |

The image and the workflow that produces it are recorded together, and so are the infrastructure
and the runbook for operating it, because in each pair neither half is verifiable without the
other.

## Acceptance criteria

- [ ] `./mvnw verify` runs formatting, compile, unit, Testcontainers integration tests and coverage.
- [ ] A pull request touching `services/api` runs every gate — formatting, tests, coverage, secret
      scanning, dependency vulnerabilities and static analysis — and deploys nothing.
- [ ] A push to the integration branch builds one image, pushes it to ECR under its commit SHA, and
      updates the ECS service to that digest.
- [ ] `infra/aws/` provisions ECR, an App Runner service and Parameter Store, with no secret in any
      output and no credential in any environment variable.
- [ ] After the Product Owner applies the stack, `GET <apprunner-host>/actuator/health` returns
      `UP` over HTTPS and `GET <apprunner-host>/api/v1/meta` returns the deployed commit, both
      anonymously.
- [ ] The environment costs under `100.000 ₫` a month at the stated usage, and both compute and
      database return to zero cost when idle.
