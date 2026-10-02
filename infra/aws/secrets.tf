# Runtime secrets.
#
# Everything the application must not print lives here as a SecureString and reaches the container
# through App Runner's `runtime_environment_secrets`, never its `runtime_environment_variables`.
# The distinction is not cosmetic: variables are returned in plain text by
# `aws apprunner describe-service`, which anyone with read access to the account can call, while
# secrets are returned as the parameter ARN and resolved only inside the running instance.

locals {
  name             = "rikkaus-${var.environment}"
  parameter_prefix = "/rikkaus/${var.environment}"

  # Optional until sign-in is exercised here. Parameter Store rejects an empty SecureString, which
  # is why these are conditional resources rather than parameters with an empty default.
  google_client_id_set     = trimspace(var.google_oauth_client_id) != ""
  google_client_secret_set = trimspace(var.google_oauth_client_secret) != ""
}

resource "random_password" "jwt_signing_key" {
  # Generated here and never seen by a person. The identity work refuses to start below 32 bytes,
  # because a guessable signing key lets anyone mint a token for any user; 64 leaves margin and
  # costs nothing.
  length  = 64
  special = false
}

resource "aws_ssm_parameter" "jwt_secret" {
  name        = "${local.parameter_prefix}/jwt-secret"
  description = "Signing key for the short-lived JWT access tokens."
  type        = "SecureString"
  value       = random_password.jwt_signing_key.result
}

# The Neon password. Neon is outside AWS, so nothing here can generate or rotate it: it is created
# in the Neon console and supplied as TF_VAR_database_password. Rotating it means resetting it in
# Neon and re-applying, which updates this parameter and redeploys the service.
resource "aws_ssm_parameter" "database_password" {
  name        = "${local.parameter_prefix}/database-password"
  description = "Password for the Neon PostgreSQL role. Set from TF_VAR_database_password."
  type        = "SecureString"
  value       = var.database_password
}

resource "aws_ssm_parameter" "google_client_id" {
  count = local.google_client_id_set ? 1 : 0

  name  = "${local.parameter_prefix}/google-client-id"
  type  = "SecureString"
  value = var.google_oauth_client_id
}

resource "aws_ssm_parameter" "google_client_secret" {
  count = local.google_client_secret_set ? 1 : 0

  name  = "${local.parameter_prefix}/google-client-secret"
  type  = "SecureString"
  value = var.google_oauth_client_secret
}
