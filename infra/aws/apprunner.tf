# The running service.
#
# App Runner replaced an ECS Fargate service behind an Application Load Balancer behind a CloudFront
# distribution. The reason was cost and it was the Product Owner's call: this environment serves one
# or two people for a few hours on weekdays, and the load balancer alone was the largest line on the
# bill while being the only component that could not be scaled to zero.
#
# What App Runner gives that the three of them together gave:
#   * an HTTPS endpoint on *.awsapprunner.com, with no certificate, domain or distribution to manage;
#   * pause and resume, where a paused service bills nothing at all;
#   * deployment from ECR without a task definition, a target group or a draining policy.
#
# What it gives up: no custom domain here either, request-driven scaling only, and a cold start on
# the first request after a resume. For a preview a QE engineer opens a few times a week, all three
# are acceptable, and the first two were already true of the design it replaced.

# No VPC connector is declared, which is the other half of the simplification.
#
# A connector exists to reach private resources, and after moving PostgreSQL to Neon there are none:
# the service talks to Neon and to Google, both over TLS on the public internet. That deletes the
# VPC, four subnets, an internet gateway, a route table and three security groups outright. The
# trade is explicit and worth stating plainly — database traffic now crosses the public internet
# rather than staying inside a VPC. It is protected by TLS, which `database_url` is validated to
# require, and this environment holds no real user data. A production deployment would not make this
# trade; the architecture puts production on a VPS with PostgreSQL alongside it.

resource "aws_apprunner_auto_scaling_configuration_version" "api" {
  auto_scaling_configuration_name = replace(local.name, "_", "-")

  # One instance, floor and ceiling. Two reviewers generate no load worth scaling for, and the
  # provisioned memory of a second instance would be billed for nothing. `min_size` cannot be zero
  # — App Runner's way of going to zero is pausing the service, not scaling the configuration.
  min_size = 1
  max_size = 1

  # Requests per instance before App Runner would add another. Set high because the limit here
  # would otherwise be the thing that decides one instance is not enough.
  max_concurrency = 100

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_apprunner_service" "api" {
  service_name = "${local.name}-api"

  source_configuration {
    authentication_configuration {
      access_role_arn = aws_iam_role.apprunner_ecr_access.arn
    }

    # Off deliberately. Auto-deployment watches one tag, and every image this pipeline publishes is
    # tagged with its commit SHA under an immutable-tag policy, so there is no moving tag to watch.
    # The pipeline calls UpdateService with the exact image instead, which keeps the deployed version
    # traceable to one commit and keeps the promotion-by-digest rule intact.
    auto_deployments_enabled = false

    image_repository {
      image_repository_type = "ECR"

      # A placeholder, replaced by the first deployment. `latest` does not exist in this repository
      # and will not resolve, so the service's first deployment fails and it settles with nothing
      # running. That reads like a broken apply and is not; the runbook says so, and the first
      # pipeline deployment supersedes it.
      image_identifier = "${aws_ecr_repository.api.repository_url}:latest"

      image_configuration {
        port = tostring(var.api_port)

        runtime_environment_variables = {
          # Makes api_port real rather than decorative: the application sets no server.port and
          # would otherwise listen on 8080 whatever this stack said.
          SERVER_PORT = tostring(var.api_port)

          DB_URL        = var.database_url
          POSTGRES_USER = var.database_username

          API_ALLOWED_ORIGINS        = var.api_allowed_origins
          GOOGLE_OAUTH_REDIRECT_URIS = var.google_oauth_redirect_uris

          # Names the environment for whoever is reading `describe-service`. The application has no
          # profile of this name and ignores it.
          RIKKAUS_ENVIRONMENT = var.environment

          # These two are what make Neon's free plan actually free, and they are the least obvious
          # thing in this file.
          #
          # Neon suspends a compute after five minutes without a connection. HikariCP's default is
          # to hold ten idle connections open forever, which Neon sees as continuous activity: the
          # compute would never suspend, the free plan's hundred CU-hours would be consumed in about
          # four days of wall time, and the service would be billed as though it ran day and night.
          #
          # Draining the pool when idle hands the cost back. The price is a connection handshake on
          # the first request after a quiet spell, plus Neon's own resume, which together add about
          # a second — invisible against a reviewer opening a page.
          SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE = "0"
          SPRING_DATASOURCE_HIKARI_IDLE_TIMEOUT = "30000"
          # Shorter than Neon's idle window, so a connection the pool still holds is retired before
          # Neon decides it is stale and closes it underneath us.
          SPRING_DATASOURCE_HIKARI_MAX_LIFETIME = "240000"
          # Two is enough for two reviewers, and a small ceiling keeps a runaway query from opening
          # connections Neon's free plan would rather not have.
          SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE = "2"
        }

        # Resolved inside the instance, so none of these appears in `describe-service` output.
        runtime_environment_secrets = merge(
          {
            POSTGRES_PASSWORD  = aws_ssm_parameter.database_password.arn
            RIKKAUS_JWT_SECRET = aws_ssm_parameter.jwt_secret.arn
          },
          local.google_client_id_set ? {
            GOOGLE_OAUTH_CLIENT_ID = aws_ssm_parameter.google_client_id[0].arn
          } : {},
          local.google_client_secret_set ? {
            GOOGLE_OAUTH_CLIENT_SECRET = aws_ssm_parameter.google_client_secret[0].arn
          } : {},
        )
      }
    }
  }

  instance_configuration {
    cpu    = var.api_cpu
    memory = var.api_memory
    # Reads the Parameter Store values above. It holds nothing else, because the application talks
    # to Neon and to Google and to no AWS API.
    instance_role_arn = aws_iam_role.apprunner_instance.arn
  }

  auto_scaling_configuration_arn = aws_apprunner_auto_scaling_configuration_version.api.arn

  health_check_configuration {
    protocol = "HTTP"
    # The same endpoint the pipeline's smoke check and a QE engineer call, so "App Runner thinks it
    # is healthy" and "a person can verify it" are the same fact.
    path              = "/actuator/health"
    interval          = 10
    timeout           = 5
    healthy_threshold = 1
    # Generous: the JVM plus Flyway against a Neon compute that may itself be resuming takes longer
    # to answer the first probe than a service with a warm local database would.
    unhealthy_threshold = 5
  }

  network_configuration {
    egress_configuration {
      # Public egress. There is no private resource left to reach.
      egress_type = "DEFAULT"
    }
    ingress_configuration {
      is_publicly_accessible = true
    }
  }

  lifecycle {
    ignore_changes = [
      # Owned by the pipeline from the first deployment onward. Without this, the next `tofu apply`
      # would put the placeholder back and undo whatever was last deployed.
      source_configuration[0].image_repository[0].image_identifier,
    ]
  }
}
