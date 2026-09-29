# The running service.

resource "aws_cloudwatch_log_group" "api" {
  name              = "/ecs/${local.name}-api"
  retention_in_days = var.log_retention_days
}

resource "aws_ecs_cluster" "main" {
  name = local.name

  setting {
    name  = "containerInsights"
    value = "disabled"
  }
}

resource "aws_ecs_cluster_capacity_providers" "main" {
  cluster_name = aws_ecs_cluster.main.name

  # FARGATE_SPOT is deliberately absent. A Spot task can be reclaimed with two minutes' notice, and
  # a preview environment that disappears mid-review teaches a QE engineer to distrust it. Scaling
  # to zero between test windows is the cost lever here, not interruptible capacity.
  capacity_providers = ["FARGATE"]

  default_capacity_provider_strategy {
    capacity_provider = "FARGATE"
    weight            = 1
  }
}

# This is the initial revision only. Every deployment registers a new revision with a new image
# digest, from the live definition rather than from this file, so after the first deploy the
# running revision is no longer the one described here. That is why the service below ignores
# changes to `task_definition`, and why the placeholder image is harmless: it is replaced before
# anyone sees it.
resource "aws_ecs_task_definition" "api" {
  family                   = "${local.name}-api"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.api_cpu
  memory                   = var.api_memory
  execution_role_arn       = aws_iam_role.task_execution.arn
  task_role_arn            = aws_iam_role.task.arn

  runtime_platform {
    operating_system_family = "LINUX"
    # The Dockerfile's base images are multi-architecture, but the pipeline builds on an x86_64
    # GitHub runner without emulation, so the pushed image has one architecture and the task must
    # ask for that one. A mismatch here surfaces as a task that starts and immediately exits with
    # "exec format error".
    cpu_architecture = "X86_64"
  }

  container_definitions = jsonencode([
    {
      # The deploy job matches this name to find the container whose image to replace. Renaming it
      # here without renaming it there makes deployments silently no-ops.
      name  = "api"
      image = "public.ecr.aws/docker/library/busybox:1.37.0"
      # Sleeps rather than exiting immediately, so the placeholder does not spend the window before
      # the first deployment in a restart loop. It still fails the load balancer's health check,
      # because it answers nothing: the service is expected to have no healthy target until the
      # pipeline deploys a real image over it.
      command = ["sh", "-c", "sleep infinity"]

      essential = true

      portMappings = [
        {
          containerPort = var.api_container_port
          protocol      = "tcp"
        }
      ]

      # Non-sensitive configuration only. Anything secret is in `secrets` below.
      environment = [
        { name = "DB_URL", value = local.jdbc_url },
        { name = "POSTGRES_USER", value = aws_db_instance.main.username },
        { name = "API_ALLOWED_ORIGINS", value = var.api_allowed_origins },
        { name = "GOOGLE_OAUTH_REDIRECT_URIS", value = var.google_oauth_redirect_uris },
        # Names the environment in the task rather than relying on the reader inferring it from a
        # log group. The application has no profile of this name and ignores it today; it exists so
        # a person reading `describe-tasks` knows what they are looking at.
        { name = "RIKKAUS_ENVIRONMENT", value = var.environment },
      ]

      # Resolved by the ECS agent from Parameter Store at start-up and injected into the process
      # environment. The values appear in neither the task definition nor any API response, which
      # is the whole reason for the split.
      secrets = concat(
        [
          { name = "POSTGRES_PASSWORD", valueFrom = aws_ssm_parameter.database_password.arn },
          { name = "RIKKAUS_JWT_SECRET", valueFrom = aws_ssm_parameter.jwt_secret.arn },
        ],
        local.google_client_id_set ? [
          { name = "GOOGLE_OAUTH_CLIENT_ID", valueFrom = aws_ssm_parameter.google_client_id[0].arn }
        ] : [],
        local.google_client_secret_set ? [
          { name = "GOOGLE_OAUTH_CLIENT_SECRET", valueFrom = aws_ssm_parameter.google_client_secret[0].arn }
        ] : [],
      )

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.api.name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "api"
        }
      }
    }
  ])

  # The placeholder image above would otherwise be proposed on every plan, reverting whatever the
  # pipeline last deployed.
  lifecycle {
    ignore_changes = [container_definitions]
  }
}

resource "aws_ecs_service" "api" {
  name            = "${local.name}-api"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.api.arn
  desired_count   = var.api_desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets         = [for subnet in aws_subnet.public : subnet.id]
    security_groups = [aws_security_group.api.id]
    # Required in a public subnet with no NAT gateway: without a public IP the task cannot reach
    # ECR and never pulls its image. Inbound reachability is still governed by the security group,
    # which admits the load balancer and nothing else.
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.api.arn
    container_name   = "api"
    container_port   = var.api_container_port
  }

  # The JVM needs roughly two seconds to start and the target group needs two passing checks
  # thirty seconds apart, so without a grace period ECS would kill each new task as unhealthy
  # before it was ever given a chance to answer, in a loop.
  health_check_grace_period_seconds = 120

  deployment_circuit_breaker {
    enable = true
    # A deployment whose tasks never become healthy rolls back to the last working revision by
    # itself. Without this, a broken image leaves the service cycling failed tasks and the
    # environment down until somebody notices.
    rollback = true
  }

  # Keeps one healthy task serving while the replacement starts, so a deploy has no gap.
  deployment_minimum_healthy_percent = 100
  deployment_maximum_percent         = 200

  # `wait_for_steady_state` is deliberately not set. On the very first apply the service is running
  # the placeholder below, which by design never becomes a healthy target, so waiting here would
  # block until the timeout and fail an apply that had in fact succeeded. Waiting for a rollout is
  # the deploy job's responsibility, and it does it with `aws ecs wait services-stable`, at the one
  # moment when a steady state is actually the expected outcome.

  lifecycle {
    # Both are owned by the pipeline and by whoever scales the environment down between test
    # windows, not by this file. Without this, the next `tofu apply` would roll the service back to
    # the placeholder revision and undo the most recent deployment.
    ignore_changes = [task_definition, desired_count]
  }

  depends_on = [aws_lb_listener.http]
}
