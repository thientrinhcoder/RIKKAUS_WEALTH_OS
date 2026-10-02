---
phase: 2
title: "Immutable image and the pull-request/deploy pipeline"
status: done
issue: 47
dependencies: [1]
---

# Phase 2: Immutable image and the pipeline

Phases 2 and 3 of the index are recorded together, because the image and the workflow that produces
it are one contract: the Dockerfile decides what an image is, and the workflow decides when one may
exist and where it may go.

## The image

`services/api/Dockerfile`, two stages, build context `services/api` so nothing outside this service
can be copied into a layer by accident.

Both stages pin an exact Temurin patch — `25.0.4.1_1-jdk-noble` to build,
`25.0.4.1_1-jre-noble` to run. The tags were read from Docker Hub rather than assumed; `25-jre`
would float, and a floating base silently changes the runtime underneath a digest the pipeline
promotes between environments unchanged, which is exactly the property promotion exists to
guarantee.

Three decisions worth keeping:

- **Tests do not run in the image build.** The gate runs against the checkout before any image is
  produced, and repeating it here would need a Docker daemon inside the build and would double the
  slowest part of every deployment. An image is only ever built from a commit whose gate passed.
- **The process is not root.** It runs as uid 1001. A root process in the task is one container
  escape away from the task role's AWS credentials, and nothing this service does needs privilege.
- **The entrypoint `exec`s.** Without it the shell holds PID 1, swallows the SIGTERM that ECS sends
  to drain a task, and every deployment ends in a forced kill after the stop timeout instead of a
  clean shutdown.

Heap is set by `-XX:MaxRAMPercentage=75.0` rather than a fixed `-Xmx`, so the same image runs
unchanged on a 1 GiB Fargate task and on the production VPS.

## The pipeline

One workflow, `.github/workflows/backend.yml`, three jobs. One file rather than two because the
branch policy is a property of the relationship between the jobs, not of any job alone.

| Job | Runs on | Holds AWS credentials |
|---|---|---|
| `verify` | every trigger | no |
| `publish` | not `pull_request`, and only once the AWS variables are set | yes |
| `deploy` | after `publish` | yes |

Permissions are declared per job. Only `publish` and `deploy` receive `id-token: write`, so the job
that runs pull-request code has no path to an AWS credential at all.

**A pull request cannot deploy for two independent reasons.** The jobs carry an `if` excluding
`pull_request`, and the IAM trust policy admits only subjects it lists. A pull-request run's
subject is `repo:<owner>/<name>:pull_request`, and a fork's names the fork, so neither is listed.
Deleting the `if` would not be enough to break the rule — which is the point of having both.

Review caught this stated in a form that would have failed. The trust policy originally listed only
`repo:<owner>/<name>:ref:refs/heads/<branch>`, which is the subject a job with no environment
presents. `deploy` declares the `development` environment so that the deployment URL appears in the
GitHub UI, and GitHub then emits `repo:<owner>/<name>:environment:development` instead, with no
branch in it. Every push would have published an image and then failed to deploy, with an error
reading like a mis-set secret. Both forms are now listed, and it is `needs: publish` that keeps the
environment form — which carries no branch — behind the branch check.

**Scan before push, not after.** Trivy runs against the locally built image; only then is it
pushed. Scanning an image already in the registry would mean a vulnerable image existed in ECR,
however briefly, and a deploy could have picked it up. `ignore-unfixed` is set, because a base-image
vulnerability with no released fix is not something this pipeline can act on, and a gate that
cannot be satisfied is one that gets disabled.

**The digest is what is deployed, not the tag.** A tag is a pointer; a digest is the image. Pinning
it is what makes a later promotion to Testing provably the same bytes.

**The task definition is read, not rendered.** The deploy job calls `describe-task-definition` on
whatever is live, replaces one field, and registers a new revision. A task-definition JSON kept in
this repository would have drifted from the OpenTofu that owns its CPU, memory, secrets and log
configuration the first time either side changed alone.

**The deployment is not finished until the public URL answers.** After `aws ecs wait
services-stable`, the job calls `/actuator/health` and `/api/v1/meta` and asserts on the responses.
A broken deploy is a red build rather than something a person discovers by opening a browser.

## Graceful degradation before AWS exists

`publish` and `deploy` are also gated on `vars.AWS_REGION` and `vars.ECR_REPOSITORY` being
non-empty. Until the Product Owner applies the stack and sets them, only the quality gate runs, and
the workflow is green rather than red. The pipeline is therefore useful from the first commit
instead of after the account is finished.

## `develop` does not exist

The architecture's branch policy deploys Development from `develop`. This repository integrates on
`main` today and has no `develop` branch. Both are wired, as workflow triggers and as permitted
OIDC subjects, so creating `develop` later needs no change. Wiring only `develop` would have
shipped a pipeline that never runs; wiring only `main` would have contradicted the recorded policy.

## Verification

The image was built and run against PostgreSQL 18.6 before any of this was committed:

```
uid=1001(rikkaus) gid=1001(rikkaus)
Flyway   Successfully applied 1 migration to schema "public", now at version v1
GET /actuator/health   {"groups":["liveness","readiness"],"status":"UP"}
GET /api/v1/meta       {"application":"rikkaus-wealth-api","apiVersion":"v1",...}
GET /api/v1/nope       404
```

The workflow YAML parses, and the job graph was inspected to confirm `verify` carries no
`id-token` permission while `publish` and `deploy` do.

The pipeline itself has not run, because it cannot until the repository variables exist. That is
the first thing the Product Owner's run will prove.
