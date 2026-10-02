# Identities: two for the service, one for the pipeline.
#
# The pipeline's identity is federated, not a stored key. GitHub mints a short-lived OIDC token for
# a workflow run, AWS trusts it only when its subject matches this repository on a named branch, and
# no access key exists in the repository to leak or rotate.

data "aws_caller_identity" "current" {}
data "aws_partition" "current" {}

# ---------------------------------------------------------------------------------------------
# App Runner roles
# ---------------------------------------------------------------------------------------------

# Used by App Runner's build side to pull the image. Note the principal: `build.apprunner`, not
# `tasks.apprunner`. Getting these two the wrong way round is the usual cause of a service that
# cannot pull its image while appearing to have the right permissions.
data "aws_iam_policy_document" "apprunner_build_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["build.apprunner.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "apprunner_ecr_access" {
  name               = "${local.name}-apprunner-ecr"
  description        = "Lets App Runner pull the API image from ECR"
  assume_role_policy = data.aws_iam_policy_document.apprunner_build_assume.json
}

resource "aws_iam_role_policy_attachment" "apprunner_ecr_access" {
  role       = aws_iam_role.apprunner_ecr_access.name
  policy_arn = "arn:${data.aws_partition.current.partition}:iam::aws:policy/service-role/AWSAppRunnerServicePolicyForECRAccess"
}

# Assumed by the running instance. Its only job is reading this environment's parameters: the
# application talks to Neon and to Google and to no AWS API at all.
data "aws_iam_policy_document" "apprunner_tasks_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["tasks.apprunner.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "apprunner_instance" {
  name               = "${local.name}-apprunner-instance"
  description        = "Runtime identity of the API; reads its own secrets and nothing else"
  assume_role_policy = data.aws_iam_policy_document.apprunner_tasks_assume.json
}

data "aws_iam_policy_document" "apprunner_instance" {
  # Scoped to this environment's prefix, so a Development instance cannot read a Testing parameter.
  statement {
    sid       = "ReadEnvironmentParameters"
    actions   = ["ssm:GetParameters", "ssm:GetParameter"]
    resources = ["arn:${data.aws_partition.current.partition}:ssm:${var.aws_region}:${data.aws_caller_identity.current.account_id}:parameter${local.parameter_prefix}/*"]
  }

  # SecureStrings created without an explicit key are encrypted under the account's AWS-managed SSM
  # key, whose ARN is not a stable input here, so the resource is `*` and two conditions narrow it:
  # the grant works only through SSM, and only for this environment's own parameters.
  statement {
    sid       = "DecryptThroughSsm"
    actions   = ["kms:Decrypt"]
    resources = ["*"]

    condition {
      test     = "StringEquals"
      variable = "kms:ViaService"
      values   = ["ssm.${var.aws_region}.amazonaws.com"]
    }

    condition {
      test     = "StringLike"
      variable = "kms:EncryptionContext:PARAMETER_ARN"
      values   = ["arn:${data.aws_partition.current.partition}:ssm:${var.aws_region}:${data.aws_caller_identity.current.account_id}:parameter${local.parameter_prefix}/*"]
    }
  }
}

resource "aws_iam_role_policy" "apprunner_instance" {
  name   = "read-runtime-parameters"
  role   = aws_iam_role.apprunner_instance.id
  policy = data.aws_iam_policy_document.apprunner_instance.json
}

# ---------------------------------------------------------------------------------------------
# GitHub Actions deploy role
# ---------------------------------------------------------------------------------------------

# One OIDC provider per account. If the account already has one for GitHub, this apply fails with
# EntityAlreadyExists; import it rather than creating a second. The runbook gives the command.
resource "aws_iam_openid_connect_provider" "github" {
  url             = "https://token.actions.githubusercontent.com"
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = ["6938fd4d98bab03faadb97b34396831e3780aea1"]
}

data "aws_iam_policy_document" "github_assume" {
  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.github.arn]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }

    # Two subject forms are needed because GitHub changes the claim depending on the job. A job with
    # no `environment:` presents `repo:owner/name:ref:refs/heads/<branch>`, which is what the publish
    # job sends. A job declaring an environment — as the deploy job does, so its URL shows in the
    # GitHub UI — presents `repo:owner/name:environment:<name>` with no branch in it at all. Listing
    # only the ref form is how a pipeline publishes an image and then fails to deploy it with an
    # error that reads like a mis-set secret. What keeps the environment form on an allowed branch is
    # the workflow's `needs: publish`, since publish is admitted only on a listed branch.
    condition {
      test     = "StringLike"
      variable = "token.actions.githubusercontent.com:sub"
      values = concat(
        [for branch in var.github_deploy_branches : "repo:${var.github_repository}:ref:refs/heads/${branch}"],
        ["repo:${var.github_repository}:environment:${var.environment}"],
      )
    }
  }
}

resource "aws_iam_role" "github_deploy" {
  name                 = "${local.name}-github-deploy"
  description          = "Assumed by the Backend workflow to push an image and deploy the App Runner service"
  assume_role_policy   = data.aws_iam_policy_document.github_assume.json
  max_session_duration = 3600
}

data "aws_iam_policy_document" "github_deploy" {
  # The OIDC token is account-wide, so this narrows to the one repository the pipeline pushes to.
  statement {
    sid = "PushToThisRepository"
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:BatchGetImage",
      "ecr:CompleteLayerUpload",
      "ecr:DescribeImages",
      "ecr:GetDownloadUrlForLayer",
      "ecr:InitiateLayerUpload",
      "ecr:PutImage",
      "ecr:UploadLayerPart",
    ]
    resources = [aws_ecr_repository.api.arn]
  }

  # Not resource-scopable: the token `docker login` needs is an account-level call.
  statement {
    sid       = "AuthenticateToEcr"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }

  # Deploy, read status, and nothing else. Notably absent: CreateService and DeleteService. The
  # pipeline changes which image a service runs; it does not get to create or destroy one, which
  # stays with whoever runs `tofu apply`.
  statement {
    sid = "DeployThisServiceOnly"
    actions = [
      "apprunner:UpdateService",
      "apprunner:DescribeService",
      "apprunner:ListOperations",
    ]
    resources = [aws_apprunner_service.api.arn]
  }

  # UpdateService may pass the two roles the service already uses, and no others, so the permission
  # cannot be turned into running the service as a more privileged identity.
  statement {
    sid       = "PassOnlyThisServicesRoles"
    actions   = ["iam:PassRole"]
    resources = [aws_iam_role.apprunner_ecr_access.arn, aws_iam_role.apprunner_instance.arn]
    condition {
      test     = "StringEquals"
      variable = "iam:PassedToService"
      values   = ["build.apprunner.amazonaws.com", "tasks.apprunner.amazonaws.com"]
    }
  }
}

resource "aws_iam_role_policy" "github_deploy" {
  name   = "deploy-api"
  role   = aws_iam_role.github_deploy.id
  policy = data.aws_iam_policy_document.github_deploy.json
}
