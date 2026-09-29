# Database and the runtime secrets.
#
# Every value the application must not print lives in Parameter Store as a SecureString and reaches
# the container through the task definition's `secrets` block, never its `environment` block. The
# distinction matters: `environment` values are returned in plain text by `ecs describe-task-
# definition`, which the deploy job calls on every run and prints nothing of, but which any reader
# of the console can call too.

resource "random_password" "database" {
  length = 32
  # RDS rejects '/', '@', '"' and ' ' in a master password, and the value also has to survive being
  # placed in a JDBC URL, so the punctuation set is narrowed rather than left to chance.
  special          = true
  override_special = "!#$%&*()-_=+[]{}<>:?"
}

resource "random_password" "jwt_signing_key" {
  # The application refuses to start below 32 bytes, because a guessable key lets anyone mint a
  # token for any user. 64 leaves margin and costs nothing.
  length  = 64
  special = false
}

resource "aws_db_subnet_group" "main" {
  name       = local.name
  subnet_ids = [for subnet in aws_subnet.private : subnet.id]
}

resource "aws_db_instance" "main" {
  identifier     = local.name
  engine         = "postgres"
  engine_version = var.db_engine_version
  instance_class = var.db_instance_class

  allocated_storage = var.db_allocated_storage
  storage_type      = "gp3"
  storage_encrypted = true

  db_name  = "rikkaus"
  username = "rikkaus"
  password = random_password.database.result

  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.database.id]

  # The architecture's rule that cost control must not expose PostgreSQL publicly, stated as
  # configuration. Combined with the private subnets and the single security-group rule, there is
  # no route from the internet to this instance.
  publicly_accessible = false

  backup_retention_period    = 7
  auto_minor_version_upgrade = true
  apply_immediately          = true

  # This stack builds Development and Testing only, never Production, and both hold disposable
  # data. A final snapshot on destroy would leave a charged artifact behind every time the
  # environment is torn down and rebuilt. Production is on a VPS with its own backup regime.
  skip_final_snapshot = true
  deletion_protection = false
}

locals {
  # Assembled here so the task definition never has to concatenate a URL, and so the shape of the
  # URL is stated in exactly one place.
  jdbc_url = "jdbc:postgresql://${aws_db_instance.main.address}:${aws_db_instance.main.port}/${aws_db_instance.main.db_name}"

  parameter_prefix = "/rikkaus/${var.environment}"

  # Google's client id and secret are optional. Until issue #42 merges there is no sign-in to
  # configure, and a public PKCE client legitimately has no secret at all, so an absent value is a
  # valid configuration rather than a missing one. Creating a SecureString with an empty value is
  # rejected by Parameter Store, which is why these are conditional rather than defaulted.
  google_client_id_set     = trimspace(var.google_oauth_client_id) != ""
  google_client_secret_set = trimspace(var.google_oauth_client_secret) != ""
}

resource "aws_ssm_parameter" "database_password" {
  name  = "${local.parameter_prefix}/database-password"
  type  = "SecureString"
  value = random_password.database.result
}

resource "aws_ssm_parameter" "jwt_secret" {
  name  = "${local.parameter_prefix}/jwt-secret"
  type  = "SecureString"
  value = random_password.jwt_signing_key.result
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
