# Image registry.
#
# One repository serves every environment, because the promotion model requires that the image
# Testing runs is byte-for-byte the image Development ran. A repository per environment would make
# that impossible to express: promotion would become a copy, and a copy is a new image.
#
# The consequence, stated here rather than discovered later: this repository and the account-wide
# OIDC provider in iam.tf are both singletons, so `tofu apply -var environment=testing` in the same
# account fails on RepositoryAlreadyExistsException and EntityAlreadyExists. Standing Testing up
# beside Development therefore needs these two resources moved behind a data source or a toggle
# first. Only Development is in scope for issue #47, and nothing here pretends otherwise.
#
# The lifecycle rule below would also become shared: thirty images total across both environments,
# not thirty each, which would shorten how far back a Development rollback can reach.

resource "aws_ecr_repository" "api" {
  name = "rikkaus-wealth-api"

  # The property the whole pipeline rests on. A tag is a commit SHA and may never be reassigned;
  # with MUTABLE, a re-run of an old workflow could quietly move a tag under a digest that a
  # deployed service resolves later, and "the same tag" would stop meaning "the same code".
  image_tag_mutability = "IMMUTABLE"

  image_scanning_configuration {
    # Second opinion behind the pipeline's Trivy gate. Trivy blocks a bad image from ever being
    # pushed; this one keeps finding vulnerabilities in images already sitting here, which is how a
    # CVE published after the push gets noticed at all.
    scan_on_push = true
  }

  force_delete = false
}

resource "aws_ecr_lifecycle_policy" "api" {
  repository = aws_ecr_repository.api.name

  # Storage is billed per gigabyte-month and every commit to an integration branch adds an image.
  # Thirty is deep enough that a rollback target from weeks ago is still present.
  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Keep the 30 most recent images"
        selection = {
          tagStatus   = "any"
          countType   = "imageCountMoreThan"
          countNumber = 30
        }
        action = { type = "expire" }
      }
    ]
  })
}
