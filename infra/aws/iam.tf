# Identities: two for the running task, one for the pipeline.
#
# The pipeline's identity is federated, not a stored key. GitHub mints a short-lived OIDC token for
# a workflow run, AWS trusts it only when its subject matches this repository on a named branch,
# and no access key exists in the repository to leak or rotate.

data "aws_caller_identity" "current" {}
data "aws_partition" "current" {}

# ---------------------------------------------------------------------------------------------
# Task roles
# ---------------------------------------------------------------------------------------------

data "aws_iam_policy_document" "ecs_tasks_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

# Used by the ECS agent, not by the application: it pulls the image and resolves the SecureString
# parameters before the container starts.
resource "aws_iam_role" "task_execution" {
  name               = "${local.name}-task-execution"
  assume_role_policy = data.aws_iam_policy_document.ecs_tasks_assume.json
}

resource "aws_iam_role_policy_attachment" "task_execution_managed" {
  role       = aws_iam_role.task_execution.name
  policy_arn = "arn:${data.aws_partition.current.partition}:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

data "aws_iam_policy_document" "task_execution_secrets" {
  # The managed policy above covers ECR and CloudWatch but grants nothing on Parameter Store, so
  # without this statement every task fails to start with a ResourceInitializationError. Scoped to
  # this environment's prefix: a Development task cannot read a Testing parameter.
  statement {
    sid       = "ReadEnvironmentParameters"
    actions   = ["ssm:GetParameters"]
    resources = ["arn:${data.aws_partition.current.partition}:ssm:${var.aws_region}:${data.aws_caller_identity.current.account_id}:parameter${local.parameter_prefix}/*"]
  }

  # SecureStrings created without an explicit key are encrypted under the account's AWS-managed SSM
  # key, and decrypting them requires this, conditioned so the grant only works through SSM.
  statement {
    sid       = "DecryptThroughSsm"
    actions   = ["kms:Decrypt"]
    resources = ["*"]

    # `*` is unavoidable here: SecureStrings created without an explicit key use the account's
    # AWS-managed SSM key, whose ARN is not a stable input to this configuration. The two
    # conditions are what narrow it. ViaService alone would still permit decrypting any SSM
    # parameter in the region, so the encryption context pins it to this environment's own prefix,
    # and the grant stays narrow even if the GetParameters statement above is later widened.
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

resource "aws_iam_role_policy" "task_execution_secrets" {
  name   = "read-runtime-parameters"
  role   = aws_iam_role.task_execution.id
  policy = data.aws_iam_policy_document.task_execution_secrets.json
}

# Assumed by the application code itself. It is deliberately empty: this service talks to
# PostgreSQL and to Google, and to no AWS API at all. The role exists so that the day something
# does need an AWS permission, it is granted here rather than by widening the execution role, which
# would hand it to the agent's image-pull path as well.
resource "aws_iam_role" "task" {
  name               = "${local.name}-task"
  assume_role_policy = data.aws_iam_policy_document.ecs_tasks_assume.json
}

# ---------------------------------------------------------------------------------------------
# GitHub Actions deploy role
# ---------------------------------------------------------------------------------------------

# One OIDC provider per account. If the account already has one for GitHub — another repository
# may have created it — this apply fails with EntityAlreadyExists; import it rather than creating a
# second. docs/backend-deployment.md gives the exact import command.
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

    # The allowlist, and the reason a pull request cannot deploy even if the workflow's own `if`
    # were removed. A pull-request run's subject is `repo:owner/name:pull_request`, and a run from
    # a fork names the fork, so neither form below can match. This is the control that does not
    # depend on workflow YAML staying correct.
    #
    # Two forms are needed because GitHub changes the subject claim depending on the job. A job
    # with no `environment:` gets `repo:owner/name:ref:refs/heads/<branch>`, which is what the
    # `publish` job presents. A job that declares an environment — as `deploy` does, so the
    # deployment URL shows in the GitHub UI — gets `repo:owner/name:environment:<name>` instead,
    # and its branch does not appear in the claim at all. Listing only the ref form would let the
    # image be published and then fail the deployment with an error that reads like a mis-set
    # secret.
    #
    # The environment form carries no branch, so it is the workflow's `needs: publish` that keeps
    # it behind the branch check rather than the trust policy: `deploy` cannot run at all unless
    # `publish` succeeded, and `publish` is admitted only on a listed branch.
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
  description          = "Assumed by the Backend workflow to push an image and roll the ECS service"
  assume_role_policy   = data.aws_iam_policy_document.github_assume.json
  max_session_duration = 3600
}

data "aws_iam_policy_document" "github_deploy" {
  # The token is account-wide, so this is narrowed to the one repository the pipeline pushes to.
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

  # Not resource-scopable: the token that `docker login` needs is an account-level call.
  statement {
    sid       = "AuthenticateToEcr"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }

  # RegisterTaskDefinition takes no resource condition, which is an IAM limitation rather than a
  # choice. The blast radius is bounded by the PassRole statement below: a registration naming any
  # role other than this environment's two is refused, so the permission cannot be used to run a
  # task as a more privileged identity.
  statement {
    sid       = "RegisterTaskDefinitions"
    actions   = ["ecs:RegisterTaskDefinition", "ecs:DescribeTaskDefinition"]
    resources = ["*"]
  }

  # Lets the deploy job find the revision this configuration last registered, which is the one it
  # derives each deployment from. Read-only, and one parameter.
  statement {
    sid       = "ReadTheBaseTaskDefinitionPointer"
    actions   = ["ssm:GetParameter"]
    resources = [aws_ssm_parameter.base_task_definition.arn]
  }

  statement {
    sid       = "RollThisServiceOnly"
    actions   = ["ecs:DescribeServices", "ecs:UpdateService"]
    resources = [aws_ecs_service.api.arn]
  }

  statement {
    sid       = "PassOnlyThisEnvironmentsTaskRoles"
    actions   = ["iam:PassRole"]
    resources = [aws_iam_role.task_execution.arn, aws_iam_role.task.arn]
    condition {
      test     = "StringEquals"
      variable = "iam:PassedToService"
      values   = ["ecs-tasks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role_policy" "github_deploy" {
  name   = "deploy-api"
  role   = aws_iam_role.github_deploy.id
  policy = data.aws_iam_policy_document.github_deploy.json
}
