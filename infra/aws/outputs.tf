# Outputs.
#
# Between them these are every value the Backend workflow needs. The setup section of
# docs/backend-deployment.md pipes each one straight into the GitHub repository variable or secret
# it becomes; nothing here has to be looked up in the console.
#
# No secret is output. The database password and the JWT signing key exist only in Parameter Store
# and in the state file, and printing either would put it in a terminal history and a CI log.

output "api_base_url" {
  description = "Public base URL of the API. Becomes the GitHub repository variable API_BASE_URL, and is the address the Product Owner and QE verify. HTTP, not HTTPS, by the accepted decision for this environment."
  value       = "http://${aws_lb.api.dns_name}"
}

output "health_check_url" {
  description = "The endpoint to open first. Answers {\"status\":\"UP\"} anonymously once a deployment has landed."
  value       = "http://${aws_lb.api.dns_name}/actuator/health"
}

output "meta_url" {
  description = "Reports the build version actually running, which is how a reviewer confirms which commit they are looking at."
  value       = "http://${aws_lb.api.dns_name}/api/v1/meta"
}

output "ecr_repository" {
  description = "Becomes the GitHub repository variable ECR_REPOSITORY."
  value       = aws_ecr_repository.api.name
}

output "ecr_repository_url" {
  description = "Full registry path, for pushing an image by hand during a first-run check."
  value       = aws_ecr_repository.api.repository_url
}

output "ecs_cluster" {
  description = "Becomes the GitHub repository variable ECS_CLUSTER."
  value       = aws_ecs_cluster.main.name
}

output "ecs_service" {
  description = "Becomes the GitHub repository variable ECS_SERVICE."
  value       = aws_ecs_service.api.name
}

output "aws_region" {
  description = "Becomes the GitHub repository variable AWS_REGION."
  value       = var.aws_region
}

output "github_deploy_role_arn" {
  description = "Becomes the GitHub repository secret AWS_DEPLOY_ROLE_ARN. Not secret in the cryptographic sense — it grants nothing without a matching OIDC token — but it names the account, so it is kept out of workflow logs."
  value       = aws_iam_role.github_deploy.arn
}

output "log_group" {
  description = "Where the API's logs land. `aws logs tail <this> --follow` is the fastest way to read a failing start-up."
  value       = aws_cloudwatch_log_group.api.name
}

output "database_endpoint" {
  description = "RDS address. Reachable only from inside the VPC; there is no public route to it."
  value       = aws_db_instance.main.address
}
