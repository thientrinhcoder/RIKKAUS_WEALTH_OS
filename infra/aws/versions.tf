terraform {
  required_version = ">= 1.8.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.66"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.9"
    }
  }

  # State is local by default so a first `tofu apply` needs nothing to exist beforehand. Move it to
  # an S3 backend with locking before a second person runs an apply: two local states diverging is
  # how one operator silently destroys the other's service. Recorded as a known limitation in
  # docs/backend-deployment.md.
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "rikkaus-wealth-os"
      Environment = var.environment
      ManagedBy   = "opentofu"
      Issue       = "47"
    }
  }
}
