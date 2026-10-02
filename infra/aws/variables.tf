variable "aws_region" {
  description = "Region for every resource in this stack. Keep it the same as the Neon project's region: Neon offers aws-ap-southeast-1, and putting the database on another continent adds a round trip to every query."
  type        = string
  default     = "ap-southeast-1"
}

variable "environment" {
  description = "Environment this stack instance represents. Part of most resource names, but see the note in registry.tf: the ECR repository and the account-wide GitHub OIDC provider are singletons, so a second instance in the same account needs those resolved first."
  type        = string
  default     = "development"

  validation {
    condition     = contains(["development", "testing"], var.environment)
    error_message = "Production is deliberately not deployable from this stack; the architecture places it on a VPS behind a manual approval."
  }
}

variable "github_repository" {
  description = "owner/name of the repository allowed to assume the deploy role. Anything else presenting a GitHub OIDC token is refused."
  type        = string
  default     = "thientrinhcoder/RIKKAUS_WEALTH_OS"
}

variable "github_deploy_branches" {
  description = "Branches whose workflow runs may assume the deploy role. The Product Owner deploys Development from main, so that is the only entry; the architecture's `develop` branch was never created."
  type        = list(string)
  default     = ["main"]
}

# ---------------------------------------------------------------------------------------------
# Compute
# ---------------------------------------------------------------------------------------------

variable "api_port" {
  description = "Port the application listens on inside the container. Passed through as SERVER_PORT, so this value genuinely drives the listener rather than only describing it."
  type        = number
  default     = 8080
}

variable "api_cpu" {
  description = "App Runner vCPU, in the units the service expects. 256 is 0.25 vCPU, the smallest it allows. Valid pairings are constrained: 0.25 vCPU takes 512 or 1024 MB only."
  type        = string
  default     = "256"
}

variable "api_memory" {
  description = "App Runner memory in MB. 512 was chosen after running the real image under `--memory=512m --cpus=0.25`, where it started and answered /actuator/health; it is the cheapest configuration the service offers. Raise to 1024 if start-up ever fails on memory after the application grows."
  type        = string
  default     = "512"
}

# ---------------------------------------------------------------------------------------------
# Database — Neon, outside AWS
# ---------------------------------------------------------------------------------------------
#
# These three have no defaults on purpose. There is nothing sensible to default to, and a stack
# that applied with a placeholder database would produce a service that starts and then fails every
# request, which is worse than an apply that refuses.

variable "database_url" {
  description = "JDBC URL of the Neon database, with no credentials in it. Example shape: jdbc:postgresql://ep-cool-name-123456.ap-southeast-1.aws.neon.tech/rikkaus?sslmode=require — the sslmode is not optional, because this connection crosses the public internet. Take the host from the Neon console and write the JDBC form by hand; Neon shows a libpq URL that embeds the password, and that password belongs in database_password instead."
  type        = string

  validation {
    condition     = can(regex("^jdbc:postgresql://", var.database_url))
    error_message = "Must be a JDBC URL beginning jdbc:postgresql://."
  }

  validation {
    condition     = !can(regex("@", var.database_url))
    error_message = "The URL must not contain credentials. An inline password would be returned in plain text by `apprunner describe-service`; put it in database_password, which is stored as a SecureString."
  }

  validation {
    condition     = can(regex("sslmode=require|sslmode=verify-full", var.database_url))
    error_message = "Must set sslmode=require or sslmode=verify-full. The connection crosses the public internet."
  }
}

variable "database_username" {
  description = "Neon database role. Not secret on its own, and it is passed as a plain environment variable."
  type        = string
}

variable "database_password" {
  description = "Password for the Neon role. Never commit it: supply it through the environment as TF_VAR_database_password, and it is stored as a Parameter Store SecureString and injected as a secret rather than an environment variable."
  type        = string
  sensitive   = true
}

# ---------------------------------------------------------------------------------------------
# Application configuration
# ---------------------------------------------------------------------------------------------

variable "api_allowed_origins" {
  description = "Comma-separated browser origins permitted to call the API. Empty registers no CORS mapping at all, which is the application's intended default; set it once issue #46 publishes an Expo web preview origin."
  type        = string
  default     = ""
}

variable "google_oauth_client_id" {
  description = "Google OAuth client id. Optional until sign-in is exercised through this environment. When empty, no parameter is created and the service receives no such variable."
  type        = string
  default     = ""
}

variable "google_oauth_client_secret" {
  description = "Google OAuth client secret. Optional, and genuinely absent for a public PKCE client. Supply through TF_VAR_google_oauth_client_secret rather than a file."
  type        = string
  default     = ""
  sensitive   = true
}

variable "google_oauth_redirect_uris" {
  description = "Comma-separated exact-match allowlist of OAuth redirect URIs. Empty permits none, which is how the application fails closed. Use the App Runner URL from `tofu output -raw api_base_url`, which is HTTPS and therefore acceptable to Google."
  type        = string
  default     = ""
}

variable "image_retention_count" {
  description = "How many images ECR keeps. Ten is deliberate for an environment nobody rolls back by more than a few commits: at roughly 400 MB an image, thirty would cost more per month than the compute does."
  type        = number
  default     = 10
}
