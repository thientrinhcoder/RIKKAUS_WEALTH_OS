# HTTPS, via CloudFront.
#
# An Application Load Balancer cannot serve HTTPS on its own hostname. AWS owns `elb.amazonaws.com`
# and will not issue a certificate for a name under it, so an ALB reached at its generated DNS name
# is plain HTTP and nothing about the load balancer can change that. The alternatives are a domain
# you control plus an ACM certificate, or this: CloudFront's default certificate, which covers
# `*.cloudfront.net` and is free, and gives the distribution below a working HTTPS hostname with no
# domain to buy, register or renew. The Product Owner chose this one.
#
# What it costs, so nobody has to rediscover it:
#   * The hostname is opaque — something like `d2x3k9abcdef.cloudfront.net`. It is fine to hand to
#     a QE engineer and wrong to put in front of a pilot user.
#   * A change to the distribution takes roughly ten minutes to propagate, and `tofu apply` waits
#     for it. Changes to the API itself do not go through here and are unaffected.
#   * The CloudFront-to-ALB hop is HTTP. It is inside AWS and reaches an ALB that now accepts
#     connections only from CloudFront, but it is not encrypted; encrypting it needs a certificate
#     on the ALB, which needs a domain, which is the option not taken.
#
# Adopting a domain later replaces this file with an ACM certificate and a 443 listener, or keeps
# it and adds `aliases` plus an ACM certificate in us-east-1. Either way `API_BASE_URL` changes and
# the Google OAuth redirect URIs must be re-registered to match, because Google compares them
# exactly.

# Managed policies, looked up by name rather than pasted as opaque UUIDs.
data "aws_cloudfront_cache_policy" "disabled" {
  name = "Managed-CachingDisabled"
}

data "aws_cloudfront_origin_request_policy" "all_viewer_except_host" {
  name = "Managed-AllViewerExceptHostHeader"
}

locals {
  alb_origin_id = "${local.name}-alb"
}

resource "aws_cloudfront_distribution" "api" {
  enabled         = true
  comment         = "${local.name} API — HTTPS front for the ALB"
  is_ipv6_enabled = true
  price_class     = var.cloudfront_price_class

  origin {
    origin_id   = local.alb_origin_id
    domain_name = aws_lb.api.dns_name

    custom_origin_config {
      http_port  = 80
      https_port = 443
      # The origin speaks HTTP only; there is no certificate on the ALB to speak TLS with.
      origin_protocol_policy = "http-only"
      origin_ssl_protocols   = ["TLSv1.2"]
      # Longer than the ALB's 60-second idle timeout, so a slow response is ended by the
      # application's own timeout rather than truncated here first.
      origin_read_timeout = 60
    }
  }

  default_cache_behavior {
    target_origin_id = local.alb_origin_id

    # The whole reason this distribution exists. A viewer arriving on HTTP is redirected to HTTPS
    # rather than served, so there is no way to reach the API in clear text.
    viewer_protocol_policy = "redirect-to-https"

    # Every method, because this is an API and not a web site. Writes are forwarded, not rejected.
    allowed_methods = ["GET", "HEAD", "OPTIONS", "PUT", "POST", "PATCH", "DELETE"]
    cached_methods  = ["GET", "HEAD"]

    # Caching is disabled outright. A cached `/actuator/health` would report a state that is no
    # longer true, which is worse than no health endpoint at all, and a cached authenticated
    # response would eventually be served to the wrong person. Nothing this API returns is
    # cacheable, so there is no cache to tune.
    cache_policy_id = data.aws_cloudfront_cache_policy.disabled.id

    # Forwards the viewer's headers, query string and cookies to the origin, except Host. Host is
    # excluded because AWS documents that as the correct choice for an ALB origin: forwarding the
    # viewer's Host would present the CloudFront name to a load balancer that does not route on it.
    origin_request_policy_id = data.aws_cloudfront_origin_request_policy.all_viewer_except_host.id

    compress = true
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    # The point of the whole approach: CloudFront's own `*.cloudfront.net` certificate, which needs
    # no domain, no validation record and no renewal. It works only for the generated hostname; the
    # moment `aliases` is set, this must become an ACM certificate issued in us-east-1.
    cloudfront_default_certificate = true
    minimum_protocol_version       = "TLSv1.2_2021"
  }
}
