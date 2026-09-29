# Network.
#
# Two public subnets carry the load balancer and the Fargate tasks; two private subnets carry
# nothing but the database.
#
# Tasks run in public subnets with a public IP rather than in private subnets behind a NAT gateway.
# That is a deliberate cost decision: a NAT gateway is a fixed monthly charge larger than the rest
# of this stack put together, and the task needs outbound internet only to pull its image from ECR
# and to reach Google's JWKS endpoint. It does not weaken isolation, because reachability is
# governed by the security groups below and not by the subnet: nothing may open a connection to a
# task except the load balancer, and nothing may open one to the database except a task.
#
# Two availability zones because an Application Load Balancer requires subnets in at least two, and
# an RDS subnet group likewise.

data "aws_availability_zones" "available" {
  state = "available"
}

locals {
  name = "rikkaus-${var.environment}"
  azs  = slice(data.aws_availability_zones.available.names, 0, 2)
}

resource "aws_vpc" "main" {
  cidr_block           = var.vpc_cidr
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = { Name = local.name }
}

resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id
  tags   = { Name = local.name }
}

resource "aws_subnet" "public" {
  for_each = { for index, az in local.azs : az => index }

  vpc_id                  = aws_vpc.main.id
  availability_zone       = each.key
  cidr_block              = cidrsubnet(var.vpc_cidr, 8, each.value)
  map_public_ip_on_launch = true

  tags = { Name = "${local.name}-public-${each.key}" }
}

resource "aws_subnet" "private" {
  for_each = { for index, az in local.azs : az => index }

  vpc_id            = aws_vpc.main.id
  availability_zone = each.key
  cidr_block        = cidrsubnet(var.vpc_cidr, 8, each.value + 100)

  tags = { Name = "${local.name}-private-${each.key}" }
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = { Name = "${local.name}-public" }
}

resource "aws_route_table_association" "public" {
  for_each = aws_subnet.public

  subnet_id      = each.value.id
  route_table_id = aws_route_table.public.id
}

# The private subnets keep the VPC's default route table, which has no internet route at all. The
# database therefore has no path out and, with no public IP and no inbound rule from anywhere but
# the task security group, no path in.

# ---------------------------------------------------------------------------------------------
# Security groups. Each rule names another security group rather than a CIDR, so the permission
# follows the resource instead of an address that could later belong to something else.
# ---------------------------------------------------------------------------------------------

resource "aws_security_group" "alb" {
  name        = "${local.name}-alb"
  description = "Public HTTP entry point"
  vpc_id      = aws_vpc.main.id

  tags = { Name = "${local.name}-alb" }
}

# Port 80, and reachable only from CloudFront.
#
# HTTPS is terminated at the CloudFront distribution in cdn.tf, which speaks HTTP to this load
# balancer. That alone would leave the ALB's own DNS name answering on plain HTTP to anyone who
# found it, which is not a theoretical problem: once the identity work in issue #42 merges, a
# person handed that URL would send a session token in clear text. AWS publishes the set of
# addresses its edge servers originate from as a managed prefix list, so the rule below closes the
# ALB to everything else and the only way in is through the HTTPS front door.
#
# What this does not do is prove the request came from *our* distribution — any CloudFront
# distribution originates from these addresses. Pinning that further means a shared secret header
# and a listener rule to check it, which is not added here because there is nothing at the edge to
# bypass: CloudFront performs no authentication, so reaching the ALB directly grants an attacker
# nothing it would not already get through the front door. What the prefix list buys is the
# guarantee that no reviewer, script or bookmark can reach this API over plain HTTP by accident,
# and that is the risk that actually exists.
data "aws_ec2_managed_prefix_list" "cloudfront_origin_facing" {
  name = "com.amazonaws.global.cloudfront.origin-facing"
}

# Note for whoever adds the next rule to this group: a prefix list consumes one rule entry per
# address in it, not one in total, and CloudFront's is large. The group's rules count against a
# quota that defaults to 60, so this one rule accounts for most of it. Raising the quota is a
# support request, not a configuration change.
resource "aws_vpc_security_group_ingress_rule" "alb_http_from_cloudfront" {
  security_group_id = aws_security_group.alb.id
  description       = "HTTP from CloudFront edge servers only"
  prefix_list_id    = data.aws_ec2_managed_prefix_list.cloudfront_origin_facing.id
  from_port         = 80
  to_port           = 80
  ip_protocol       = "tcp"
}

resource "aws_vpc_security_group_egress_rule" "alb_to_api" {
  security_group_id            = aws_security_group.alb.id
  description                  = "Forward to the API tasks"
  referenced_security_group_id = aws_security_group.api.id
  from_port                    = var.api_container_port
  to_port                      = var.api_container_port
  ip_protocol                  = "tcp"
}

resource "aws_security_group" "api" {
  name        = "${local.name}-api"
  description = "Fargate tasks running the API"
  vpc_id      = aws_vpc.main.id

  tags = { Name = "${local.name}-api" }
}

resource "aws_vpc_security_group_ingress_rule" "api_from_alb" {
  security_group_id            = aws_security_group.api.id
  description                  = "Only the load balancer may reach the application port"
  referenced_security_group_id = aws_security_group.alb.id
  from_port                    = var.api_container_port
  to_port                      = var.api_container_port
  ip_protocol                  = "tcp"
}

# Egress, enumerated rather than opened.
#
# This was `ip_protocol = "-1"` to `0.0.0.0/0` on the reasoning that the task's destinations — ECR,
# CloudWatch Logs, Google's JWKS endpoint — have no fixed addresses. That is true of the addresses
# and not of the ports: every one of those is HTTPS. Allowing every protocol to every address also
# grants a compromised task a free outbound channel on any port, which is exactly the path data
# leaves by.
#
# Three rules replace it, which is the complete set of what the task actually does.
#
# If a deployment ever fails with CannotPullContainerError or a task starts and logs nothing, this
# block is the first place to look: widening it back to `ip_protocol = "-1"` will confirm or clear
# it in one apply.
resource "aws_vpc_security_group_egress_rule" "api_https" {
  security_group_id = aws_security_group.api.id
  description       = "HTTPS to ECR, S3 for image layers, CloudWatch Logs and Google"
  cidr_ipv4         = "0.0.0.0/0"
  from_port         = 443
  to_port           = 443
  ip_protocol       = "tcp"
}

# The VPC resolver, which lives inside the VPC's own range. Without this nothing resolves and the
# HTTPS rule above is useless — the failure looks like a network outage rather than a missing rule,
# which is why it is called out here.
resource "aws_vpc_security_group_egress_rule" "api_dns_udp" {
  security_group_id = aws_security_group.api.id
  description       = "DNS to the VPC resolver"
  cidr_ipv4         = var.vpc_cidr
  from_port         = 53
  to_port           = 53
  ip_protocol       = "udp"
}

resource "aws_vpc_security_group_egress_rule" "api_dns_tcp" {
  security_group_id = aws_security_group.api.id
  description       = "DNS over TCP, for responses too large for UDP"
  cidr_ipv4         = var.vpc_cidr
  from_port         = 53
  to_port           = 53
  ip_protocol       = "tcp"
}

resource "aws_vpc_security_group_egress_rule" "api_database" {
  security_group_id            = aws_security_group.api.id
  description                  = "PostgreSQL to the database"
  referenced_security_group_id = aws_security_group.database.id
  from_port                    = 5432
  to_port                      = 5432
  ip_protocol                  = "tcp"
}

resource "aws_security_group" "database" {
  name        = "${local.name}-database"
  description = "RDS PostgreSQL"
  vpc_id      = aws_vpc.main.id

  tags = { Name = "${local.name}-database" }
}

# The only way into the database. There is no rule admitting an office address, a VPN range or
# 0.0.0.0/0, and `publicly_accessible` is false on the instance, so PostgreSQL is not reachable
# from the internet by any route. Reaching it by hand means a bastion or a session-manager tunnel,
# which is a deliberate act rather than an open port.
resource "aws_vpc_security_group_ingress_rule" "database_from_api" {
  security_group_id            = aws_security_group.database.id
  description                  = "PostgreSQL from the API tasks only"
  referenced_security_group_id = aws_security_group.api.id
  from_port                    = 5432
  to_port                      = 5432
  ip_protocol                  = "tcp"
}
