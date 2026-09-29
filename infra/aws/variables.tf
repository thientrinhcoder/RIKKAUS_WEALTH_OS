variable "aws_region" {
  description = "Region for every resource in this stack."
  type        = string
  default     = "ap-southeast-1"
}

variable "environment" {
  description = "Environment this stack instance represents. Part of most resource names, but see the note in registry.tf: a second instance in the same account collides on the shared ECR repository and the account-wide GitHub OIDC provider, and standing Testing up alongside Development needs that resolved first."
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
  description = "Branches whose workflow runs may assume the deploy role. The Product Owner deploys Development from main, so that is the only entry; the architecture's `develop` branch was never created. A pull-request run, or a run from a fork, carries a different subject and cannot match, which is the second half of the guarantee the workflow's own `if` makes."
  type        = list(string)
  default     = ["main"]
}

variable "vpc_cidr" {
  description = "Address space for the VPC."
  type        = string
  default     = "10.20.0.0/16"
}

variable "api_container_port" {
  description = "Port the Spring Boot application listens on inside the task."
  type        = number
  default     = 8080
}

variable "api_cpu" {
  description = "Fargate CPU units for the API task. 512 is a quarter vCPU."
  type        = number
  default     = 512
}

variable "api_memory" {
  description = "Fargate memory (MiB) for the API task. The JVM takes 75% of this as its heap ceiling; see JAVA_OPTS in the Dockerfile."
  type        = number
  default     = 1024
}

variable "api_desired_count" {
  description = "Running tasks. Set to 0 outside a test window to stop paying for compute without destroying anything."
  type        = number
  default     = 1
}

variable "db_instance_class" {
  description = "RDS instance class. db.t4g.micro is the cheapest Graviton option that runs PostgreSQL."
  type        = string
  default     = "db.t4g.micro"
}

variable "db_engine_version" {
  description = "PostgreSQL major version on RDS. Major-only lets AWS pick the current minor. Check what the region actually offers with: aws rds describe-db-engine-versions --engine postgres --query 'DBEngineVersions[].EngineVersion'"
  type        = string
  default     = "17"
}

variable "db_allocated_storage" {
  description = "Allocated storage in GiB."
  type        = number
  default     = 20
}

variable "api_allowed_origins" {
  description = "Comma-separated browser origins permitted to call the API. Empty registers no CORS mapping at all, which is the application's intended default; set it to the Expo web preview origin once issue #46 publishes one."
  type        = string
  default     = ""
}

variable "google_oauth_client_id" {
  description = "Google OAuth client id. Optional: leave empty until issue #42 merges and sign-in is actually wired up. When empty, no parameter is created and the task receives no such variable."
  type        = string
  default     = ""
}

variable "google_oauth_client_secret" {
  description = "Google OAuth client secret. Optional, and genuinely absent for a public PKCE client. Never commit a value; supply it through a tfvars file that git ignores, or set TF_VAR_google_oauth_client_secret in the shell."
  type        = string
  default     = ""
  sensitive   = true
}

variable "google_oauth_redirect_uris" {
  description = "Comma-separated exact-match allowlist of OAuth redirect URIs. Empty permits none, which is how the application fails closed."
  type        = string
  default     = ""
}

variable "cloudfront_price_class" {
  description = "Which edge locations the distribution uses. PriceClass_200 includes Asia, which is where this team and its reviewers are; PriceClass_100 is cheaper but serves Vietnam from North America and Europe, adding a noticeable round trip to every call."
  type        = string
  default     = "PriceClass_200"

  validation {
    condition     = contains(["PriceClass_100", "PriceClass_200", "PriceClass_All"], var.cloudfront_price_class)
    error_message = "Must be one of PriceClass_100, PriceClass_200 or PriceClass_All."
  }
}

variable "log_retention_days" {
  description = "CloudWatch Logs retention for the API log group."
  type        = number
  default     = 14
}
