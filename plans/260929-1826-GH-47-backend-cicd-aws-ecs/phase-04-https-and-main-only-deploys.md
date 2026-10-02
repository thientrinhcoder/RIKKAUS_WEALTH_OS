---
phase: 4
title: "HTTPS at the edge, and main as the deploy branch"
status: done
issue: 47
dependencies: [3]
---

# Phase 4: HTTPS at the edge, and `main` as the deploy branch

Two Product Owner decisions taken after reading phase 3: deploy Development from `main`, and serve
the API over HTTPS.

## `main` is the integration branch

The architecture's branch policy deploys Development from `develop`. That branch was never created
and the repository integrates on `main`, so `main` is now the only push trigger in
`.github/workflows/backend.yml` and the only entry in `github_deploy_branches`. Adopting `develop`
later means adding it in both places and nowhere else.

## Why this could not simply be "add a 443 listener"

The obvious reading of "add HTTPS" is a certificate and a second listener on the load balancer.
That is impossible here, and the reason is worth recording because it is the thing that decided the
design.

**An ALB cannot serve HTTPS on its own hostname.** A listener needs a certificate, ACM issues a
certificate only for a domain whose control you can prove, and the load balancer's generated name
lives under `elb.amazonaws.com`, which AWS owns. There is no validation anyone but AWS could pass.
Unlike CloudFront or API Gateway, an ALB has no default certificate to fall back on.

So HTTPS costs either a domain or a CloudFront distribution. Three options were put to the Product
Owner — a domain in Route 53, a domain hosted elsewhere with manual DNS validation, or CloudFront's
default certificate with no domain at all — and the third was chosen.

## What was built

`infra/aws/cdn.tf`: an `aws_cloudfront_distribution` with the ALB as its origin, serving on
CloudFront's own `*.cloudfront.net` certificate with `viewer_protocol_policy = "redirect-to-https"`,
so a viewer arriving on HTTP is redirected rather than served.

Three choices inside it are load-bearing:

- **Caching is disabled**, through the managed `CachingDisabled` policy. A cached
  `/actuator/health` would report a state that is no longer true, which is worse than having no
  health endpoint, and a cached authenticated response would eventually be served to the wrong
  person. Nothing this API returns is cacheable.
- **All methods are allowed**, not just the cacheable ones, because this is an API. Writes are
  forwarded rather than rejected at the edge.
- **`AllViewerExceptHostHeader`** forwards the viewer's headers, query string and cookies but not
  Host, which is what AWS documents for an ALB origin.

The managed policies are looked up by name through data sources rather than pasted as UUIDs, so the
configuration says what it means.

## The part that makes it a guarantee rather than a suggestion

Terminating TLS at the edge does nothing on its own if the origin still answers the whole internet.
The ALB's DNS name would have kept serving plain HTTP to anyone who found it, and that is not
theoretical: once issue #42 merges, a person handed that URL sends a session token in clear text.

`network.tf` therefore replaces the `0.0.0.0/0` ingress rule with one referencing AWS's published
`com.amazonaws.global.cloudfront.origin-facing` managed prefix list. The load balancer now accepts
connections from CloudFront's edge servers and nothing else, so a request from anywhere else times
out. There is no way to reach this API in clear text.

What that does **not** prove is that a request came from *this* distribution — any CloudFront
distribution originates from those addresses. Pinning it further means a shared secret header and a
listener rule to check it, deliberately not added: CloudFront performs no authentication here, so
reaching the ALB directly grants an attacker nothing the front door would not. The risk that
actually exists is a reviewer, a script or a bookmark using HTTP by accident, and the prefix list
closes exactly that.

One trap noted in the file for whoever edits that security group next: a prefix list consumes one
rule entry per address in it, not one in total, and CloudFront's is large against a default quota
of 60.

## The consequence that would have bitten silently

With TLS ending at the edge, the last two hops are HTTP, so the application sees an HTTP request on
port 8080 and would build every absolute URL with the wrong scheme and host. Nothing on `main`
exposes that today — `/api/v1/meta` returns no URLs and Problem Details' `instance` is a path — but
the OAuth redirect in issue #42 would be generated as `http://`, and Google rejects a plain-HTTP
redirect URI outright. The failure would have appeared as a sign-in that does not work, weeks after
this change, with nothing pointing back here.

The task therefore runs with `SERVER_FORWARD_HEADERS_STRATEGY=framework`, which tells Spring to
honour `X-Forwarded-Proto` and friends. That is safe precisely because of the prefix-list rule
above: a client that cannot reach the load balancer cannot forge those headers.

## Verification

Run against the real image, because relaxed binding is the kind of thing that is usually true and
occasionally not:

| Checked | Result |
|---|---|
| `SERVER_PORT=9090` | `Tomcat started on port 9090`, health `UP` — the port variable is real, not decorative |
| `SERVER_FORWARD_HEADERS_STRATEGY=framework` | Context starts clean, no errors in the log |
| `/api/v1/meta` with `X-Forwarded-Proto: https` and a forwarded host | Answers normally |
| `/api/v1/nope` under forwarding | RFC 9457 body intact, `instance` still a path |
| `tofu fmt -check`, `tofu validate` | Both pass against provider aws 6.66.0 |

Nothing was applied. The distribution, the prefix-list rule and the redirect are unproven against
real AWS, and the first apply is what will prove them.

## What this leaves open

The hostname is a generated `*.cloudfront.net` name — acceptable for a QE engineer, wrong for a
pilot user, and it changes if the distribution is ever recreated. Adopting a domain later means an
ACM certificate issued **in us-east-1**, which is the only region CloudFront accepts certificates
from, plus `aliases` on the distribution and a DNS record. `API_BASE_URL` and every registered
Google OAuth redirect URI change with it, because Google compares them character for character.

The CloudFront-to-ALB hop stays unencrypted. Closing it needs a certificate on the load balancer,
which needs a domain, which is the option not taken.
