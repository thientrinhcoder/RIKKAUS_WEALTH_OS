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
| Public entry point | **CloudFront over HTTPS on its default certificate** | No domain and no ACM certificate; the hostname is a generated `*.cloudfront.net` name, and adopting a readable domain is the follow-up |
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

The image and the workflow that produces it are recorded together, and so are the infrastructure
and the runbook for operating it, because in each pair neither half is verifiable without the
other.

## Acceptance criteria

- [ ] `./mvnw verify` runs formatting, compile, unit, Testcontainers integration tests and coverage.
- [ ] A pull request touching `services/api` runs every gate and deploys nothing.
- [ ] A push to the integration branch builds one image, pushes it to ECR under its commit SHA, and
      updates the ECS service to that digest.
- [ ] `infra/aws/` provisions ECR, RDS, ECS Fargate, an ALB and Parameter Store, with no public
      database and no secret in state output.
- [ ] After the Product Owner applies the stack, `GET <cloudfront-host>/actuator/health` returns
      `UP` over HTTPS and `GET <cloudfront-host>/api/v1/meta` returns the deployed commit, both
      anonymously, and the load balancer's own DNS name is unreachable from the internet.
