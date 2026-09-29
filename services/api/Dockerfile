# Build and runtime image for the Rikkaus Wealth OS API.
#
# The build context is `services/api`, not the repository root, so nothing outside this service can
# be copied into a layer by accident.
#
# Both stages pin an exact Temurin patch, as the architecture document requires of every deployed
# tag. A floating `25-jre` would change the JVM under an unrelated rebuild with nothing in the diff
# to show it.
#
# What this does not give is bit-for-bit reproducibility: a tag, even an exact one, can in
# principle be re-pushed, and only `eclipse-temurin@sha256:...` would rule that out. The property
# the promotion rule actually depends on is unaffected either way, because what is promoted between
# environments is this image's own digest, resolved once at push and never re-resolved.

# ---------------------------------------------------------------------------------------------
# Stage 1 — compile and package
# ---------------------------------------------------------------------------------------------
FROM eclipse-temurin:25.0.4.1_1-jdk-noble AS builder

WORKDIR /workspace

# Dependencies resolve in their own layer, keyed only on the POM and the wrapper. An edit under
# src/ then repackages without re-downloading the Spring Boot dependency tree.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline

COPY src/ src/

# Tests are deliberately not run here. The pipeline runs the whole gate — formatting, unit tests,
# Testcontainers integration tests and coverage — against the checkout before it ever produces an
# image, and repeating them here would require a Docker daemon inside the image build and would
# double the slowest part of every deployment. An image is only ever produced from a commit whose
# gate already passed.
RUN ./mvnw -B -q -DskipTests package

# ---------------------------------------------------------------------------------------------
# Stage 2 — runtime
# ---------------------------------------------------------------------------------------------
FROM eclipse-temurin:25.0.4.1_1-jre-noble AS runtime

# Unprivileged. A root process inside the task is one container escape away from the task role's
# AWS credentials, and nothing this service does needs privilege.
RUN groupadd --system --gid 1001 rikkaus \
    && useradd --system --uid 1001 --gid rikkaus --home /app --shell /usr/sbin/nologin rikkaus

WORKDIR /app
COPY --from=builder --chown=rikkaus:rikkaus /workspace/target/*.jar /app/app.jar
USER rikkaus

# Documentation only; ECS publishes the port from the task definition's portMappings. The
# application listens on 8080 unless SERVER_PORT says otherwise, which is what the infrastructure's
# api_container_port variable sets.
EXPOSE 8080

# MaxRAMPercentage rather than a fixed -Xmx, so the heap follows whatever memory the ECS task
# definition grants and the same image runs unchanged on a VPS with different limits. The JVM's own
# default leaves a large share of a small Fargate task unused.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

# Run through a shell so JAVA_OPTS expands, and `exec` so the JVM becomes PID 1 and receives the
# SIGTERM that ECS sends to drain a task. Without the exec the shell holds PID 1, swallows the
# signal, and every deployment ends in a forced kill after the stop timeout rather than a clean
# shutdown.
ENTRYPOINT ["/bin/sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
