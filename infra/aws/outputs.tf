# Outputs.
#
# Between them these are every value the Backend workflow needs. The setup section of
# docs/backend-deployment.md pipes each one straight into the GitHub repository variable or secret
# it becomes; nothing here has to be looked up in the console.
#
# No secret is output. The JWT signing key and the Neon password exist only in Parameter Store and
# in the state file, and printing either would put it in a terminal history and a CI log.

output "api_base_url" {
  description = "Public base URL of the API, over HTTPS on App Runner's own certificate. Becomes the GitHub repository variable API_BASE_URL, and is the address the Product Owner and QE verify."
  value       = "https://${aws_apprunner_service.api.service_url}"
}

output "health_check_url" {
  description = "The endpoint to open first. Answers {\"status\":\"UP\"} anonymously once a deployment has landed."
  value       = "https://${aws_apprunner_service.api.service_url}/actuator/health"
}

output "meta_url" {
  description = "Reports the build version actually running, which is how a reviewer confirms which commit they are looking at."
  value       = "https://${aws_apprunner_service.api.service_url}/api/v1/meta"
}

output "ecr_repository" {
  description = "Becomes the GitHub repository variable ECR_REPOSITORY."
  value       = aws_ecr_repository.api.name
}

output "ecr_repository_url" {
  description = "Full registry path, for pushing an image by hand during a first-run check."
  value       = aws_ecr_repository.api.repository_url
}

output "apprunner_service_arn" {
  description = "Becomes the GitHub repository variable APPRUNNER_SERVICE_ARN. The deploy job updates this service, and the deploy role is scoped to this ARN alone."
  value       = aws_apprunner_service.api.arn
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
  description = "Where the application's own logs land. App Runner creates this itself; `aws logs tail <this> --follow` is the fastest way to read a failing start-up. A second group ending /service carries App Runner's own deployment events."
  value       = "/aws/apprunner/${aws_apprunner_service.api.service_name}/${aws_apprunner_service.api.service_id}/application"
}

output "pause_command" {
  description = "Stops all compute charges without destroying anything. Resume with the same command and resume-service. Printed as an output because it is the single most useful thing to know about running this environment cheaply."
  value       = "aws apprunner pause-service --service-arn ${aws_apprunner_service.api.arn} --region ${var.aws_region}"
}
