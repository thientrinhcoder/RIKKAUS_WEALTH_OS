# The origin behind the CDN.
#
# This load balancer is internet-facing in the AWS sense — it sits in public subnets and has a
# public DNS name — but it is not reachable from the internet: the security group in network.tf
# admits only CloudFront's edge addresses. Viewers arrive at the distribution in cdn.tf over HTTPS,
# and this is what it forwards to.
#
# It therefore stays on plain HTTP with no certificate, which is not a compromise but the direct
# consequence of the chosen approach: a certificate here would have to name a domain, and the whole
# point of fronting with CloudFront was to get HTTPS without owning one.
#
# `api_base_url` in outputs.tf is the CloudFront hostname, not this one. Nothing should be pointed
# at the ALB's DNS name — it will simply time out.

resource "aws_lb" "api" {
  name               = "${local.name}-alb"
  load_balancer_type = "application"
  internal           = false
  security_groups    = [aws_security_group.alb.id]
  subnets            = [for subnet in aws_subnet.public : subnet.id]

  # Off because this environment is expected to be destroyed and rebuilt; Production is not
  # deployed from this stack.
  enable_deletion_protection = false

  # Comfortably longer than any request this API serves, so a slow response is not cut short by the
  # load balancer before the application has answered.
  idle_timeout = 60
}

resource "aws_lb_target_group" "api" {
  name        = "${local.name}-api"
  port        = var.api_container_port
  protocol    = "HTTP"
  vpc_id      = aws_vpc.main.id
  target_type = "ip"

  health_check {
    # The same endpoint the pipeline's smoke check and a QE engineer call, so a target the load
    # balancer considers healthy and an endpoint a person can verify are the same fact. The
    # application permits it anonymously and it is the only actuator endpoint exposed.
    path                = "/actuator/health"
    matcher             = "200"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }

  # Long enough for in-flight requests to finish, short enough that a deployment is not dominated
  # by waiting for old tasks to drain.
  deregistration_delay = 30

  # `create_before_destroy` is deliberately not set. It cannot work with a fixed `name`: the
  # replacement would be created while the original still holds the name and the apply would fail
  # on the collision. Nothing in this block forces replacement in the first place — the health
  # check, the timeouts and the drain delay all update in place — and the attributes that would
  # (port, protocol, target type, VPC) are not ones that change without a wider redesign.
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.api.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.api.arn
  }
}
