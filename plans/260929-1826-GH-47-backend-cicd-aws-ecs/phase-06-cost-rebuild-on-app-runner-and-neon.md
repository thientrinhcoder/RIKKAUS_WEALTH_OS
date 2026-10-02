---
phase: 6
title: "Cost rebuild: App Runner and Neon"
status: done
issue: 47
dependencies: [5]
---

# Phase 6: Cost rebuild on App Runner and Neon

The Product Owner rejected the environment on cost before it was ever applied, and was right to.

## What was wrong

Phases 1–5 built a textbook ECS environment: Fargate behind an Application Load Balancer behind a
CloudFront distribution, with RDS PostgreSQL in private subnets. Every piece was defensible on its
own. The total was roughly **`1.500.000 ₫` a month** for an environment that serves one or two people
for a few hours on weekdays.

| Component | Monthly | Scales to zero? |
|---|---|---|
| ALB | ~`420.000 ₫` | **no** — fixed, 24/7 |
| RDS `db.t4g.micro` | ~`390.000 ₫` + storage | stoppable, but 7 days maximum |
| Fargate 0.5 vCPU / 1 GB | ~`460.000 ₫` | yes |
| CloudFront | ~`25.000 ₫` | effectively free |

The load balancer was both the largest line and the only component that could not be scaled to zero.
It was chosen because it is what an ECS service needs, not because anything about this environment
needed it. That is the mistake: a pattern was applied where a requirement should have been read.

## What was considered

Four options were priced against published rates rather than from memory, and two were eliminated by
checking:

- **Aurora Serverless v2** was the obvious "scales to zero" answer and does not. Its documented
  minimum is **0.5 ACU**, roughly `1.100.000 ₫` a month — more expensive than the RDS instance it
  would have replaced.
- **Supabase** free Postgres **pauses the entire project after 7 days of inactivity**, needing a
  manual resume and a 10–30 second cold start. That is precisely this usage pattern: a few hours,
  then a weekend. It would have failed in the second week.
- **A single VM** running the API, PostgreSQL and Caddy under Docker Compose — the shape the
  architecture already specifies for production — came to about `310.000 ₫` a month flat, or nothing
  on a free-tier EC2 instance. Cheap and predictable, but it trades managed deployment for a box
  somebody patches.
- **App Runner + Neon** was chosen.

The free-tier question turned out not to matter: `aws freetier get-free-tier-usage` returned an empty
list, so the account is past its twelve-month tier, and the chosen design uses neither EC2 nor RDS.

## What was built

| Concern | Before | After |
|---|---|---|
| Compute | ECS Fargate, always on | App Runner, 0.25 vCPU / 0.5 GB, pausable to zero |
| HTTPS | CloudFront + ALB | included on `*.awsapprunner.com` |
| Database | RDS in private subnets | Neon free plan, suspends after 5 minutes idle |
| Network | VPC, 4 subnets, IGW, route table, 3 security groups | none |
| Cost | ~`1.500.000 ₫`/month | ~`50.000 ₫` paused outside hours, ~`95.000 ₫` always on |

Deleted outright: `network.tf`, `load-balancer.tf`, `cdn.tf`, `service.tf`, and the RDS half of
`database.tf`. Added: `apprunner.tf` and `secrets.tf`.

**The cheaper system is also the smaller one.** The infrastructure scan went from thirteen findings
to **one**, and not by exempting twelve — by deleting the resources they were about.
`.trivyignore.yaml` shrank from eleven entries to one.

**No VPC connector is declared**, which is the second-order simplification. A connector exists to
reach private resources; once the database moved outside AWS there were none, so the whole private
network went with it.

## The two things that needed proving

Both were verified by running the real image, because both are load-bearing and both are the kind of
claim that is usually true and occasionally not.

**0.25 vCPU / 0.5 GB is enough.** App Runner's smallest configuration pairs 0.25 vCPU with 512 MB.
Spring Boot with Hibernate and Tomcat in 512 MB is not obviously safe, so the image was run under
`--memory=512m --cpus=0.25` against a real PostgreSQL: it started and answered `/actuator/health`
with `UP`. That is the difference between the cheapest tier and twice the memory cost.

**The Hikari settings are what make Neon free.** Neon suspends a compute after five minutes without
a connection. HikariCP's default holds ten idle connections open indefinitely, which Neon reads as
continuous activity: the compute would never suspend and the free plan's hundred CU-hours would be
consumed in about four days of wall time. The service therefore sets `MINIMUM_IDLE=0` with a short
idle timeout.

Spring silently ignores an environment variable whose name does not bind, so "I set it" proves
nothing. The image was run with `LOGGING_LEVEL_COM_ZAXXER_HIKARI=DEBUG` and the JVM printed its own
resolved configuration:

```
idleTimeout.....................30000
maxLifetime.....................240000
maximumPoolSize.................2
minimumIdle.....................0
Closing connection ... (initialization check complete and minimumIdle is zero)
```

The last line is the proof: Hikari closes the connection immediately after the start-up check, so
Neon sees nothing and suspends. Had the names not bound, every line would have shown a default and
the cost argument would have been false.

## Accepted trade-offs

- **Database traffic crosses the public internet**, under TLS. `database_url` is validated to require
  `sslmode=require` or stronger and to contain no credentials, so both mistakes fail the apply rather
  than leaking quietly. Acceptable only because this environment holds no real user data.
- **PostgreSQL for Development is not an AWS service.** Recorded as an amendment in
  `ARCHITECTURE_TECHNOLOGY_DECISIONS.md`. Production is unchanged.
- **Neon's free plan limits**: 1 GB storage, 100 CU-hours a month, and projects inactive for 90 days
  are subject to deletion. Forgiving for weekly use; not forgiving for a mothballed quarter.
- **Cold start** on the first request after a pause or suspend. This is the mechanism, not a defect.
- **Still no custom domain.** App Runner supports one; adopting it needs a domain and a DNS record.

## Verification

- `tofu fmt -check` and `tofu validate` pass against provider `aws` 6.66.0.
- `trivy config` over the new stack: one finding, triaged with a statement and an expiry.
- The workflow YAML parses and the job graph is unchanged in shape: `verify`, then `publish`, then
  `deploy`.
- The image starts in 512 MB at 0.25 vCPU, and the Hikari variables bind, both shown above.

Nothing was applied. The App Runner service, the Neon connection and the deploy job's
`update-service` call are unproven against real infrastructure, and the Product Owner's first apply
is what will prove them.

## What this phase says about the previous five

The pipeline, the gates and the image were right and survive unchanged. What was wrong was the
environment they deployed into, and it was wrong for a reason worth naming: the requirement — one or
two reviewers, a few hours, on weekdays — was in the issue from the start, and the design answered a
different question, which was what a well-architected ECS environment looks like. Reading the load
before choosing the instrument would have reached App Runner in the first pass.
