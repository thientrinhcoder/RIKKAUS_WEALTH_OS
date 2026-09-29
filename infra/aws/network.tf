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

# Port 80 only, and open to the world, because the accepted decision for this environment is a
# plain-HTTP listener on the load balancer's own DNS name with no certificate. Traffic between a
# reviewer's browser and this listener is unencrypted; that is the known cost of skipping ACM, it
# is recorded in the plan and the runbook, and it is why no real credential should ever be sent to
# this environment. Adding HTTPS later is a listener, a certificate and one more rule here.
resource "aws_vpc_security_group_ingress_rule" "alb_http" {
  security_group_id = aws_security_group.alb.id
  description       = "HTTP from the internet"
  cidr_ipv4         = "0.0.0.0/0"
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

# Unrestricted egress. The task pulls its image from ECR, writes to CloudWatch Logs and fetches
# Google's signing keys, all over HTTPS to endpoints whose addresses are not fixed.
resource "aws_vpc_security_group_egress_rule" "api_all" {
  security_group_id = aws_security_group.api.id
  description       = "Outbound to ECR, CloudWatch and Google"
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1"
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
