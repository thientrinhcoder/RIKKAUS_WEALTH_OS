# Public entry point.
#
# An internet-facing Application Load Balancer on its AWS-generated DNS name, listening on HTTP
# port 80. No ACM certificate and no custom domain: that is the Product Owner's accepted decision
# for this environment, recorded in plans/260929-1826-GH-47-backend-cicd-aws-ecs/plan.md.
#
# What it costs, stated plainly so nobody has to rediscover it:
#   * Traffic between a reviewer's browser and this listener is unencrypted. Nothing that matters
#     should be sent to this environment, and the Google sign-in that issue #42 adds will not be
#     usable from here until HTTPS lands, because Google refuses a plain-HTTP redirect URI.
#   * A browser page served over HTTPS cannot call an HTTP API; it is blocked as mixed content. The
#     Expo web preview from issue #46 will therefore need HTTPS here before it can talk to this
#     environment from anywhere but a local page.
# Both are removed by adding a certificate and a 443 listener, which is a small change to this file
# and one more security-group rule.

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
