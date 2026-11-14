package com.rikkaus.wealth.onboarding.api;

import com.rikkaus.wealth.onboarding.domain.OnboardingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * The progress facts a client may record. Every member is optional; an absent or null member leaves
 * that fact as it is.
 *
 * <p>There is no owner member and no step-completion member. The progress belongs to whoever the access
 * token authenticates — a {@code userId} sent anyway is an unknown property Jackson drops — and whether
 * a step is done is read from the user's records, so a client cannot claim one. The single exception is
 * the dashboard review, which no record proves.
 *
 * <p>An unrecognised {@code status}, or a {@code currentStep} that is not a JSON integer — {@code 2.5},
 * {@code "3"} — is a {@code malformed-request} rather than a {@code validation-failed}, because Jackson
 * cannot construct the record at all. See {@link WholeNumberDeserializer} for why a fraction is not
 * truncated.
 */
@Schema(description = "Record progress through the guided onboarding. Send only what changed.")
public record UpdateOnboardingRequest(
        @Schema(
                        description =
                                "The step the user is now viewing, 1 to 7 in the backbone order. The client "
                                        + "resumes here next time.",
                        example = "3",
                        minimum = "1",
                        maximum = "7")
                @Min(value = 1, message = "must be between 1 and 7")
                @Max(value = 7, message = "must be between 1 and 7")
                @JsonDeserialize(using = WholeNumberDeserializer.class)
                Integer currentStep,
        @Schema(
                        description =
                                "Whether the user has reviewed their first dashboard. true records the first "
                                        + "review and keeps its moment if sent again; false withdraws it.",
                        example = "true")
                Boolean dashboardReviewed,
        @Schema(
                        description =
                                "COMPLETED or DISMISSED ends the guided flow; IN_PROGRESS reopens it. Completion "
                                        + "does not require every step to be done.",
                        example = "COMPLETED")
                OnboardingStatus status) {}
