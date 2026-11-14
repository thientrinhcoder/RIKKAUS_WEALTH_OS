package com.rikkaus.wealth.onboarding.api;

import com.rikkaus.wealth.onboarding.application.OnboardingView;
import com.rikkaus.wealth.onboarding.domain.OnboardingStatus;
import com.rikkaus.wealth.onboarding.domain.OnboardingStep;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * The guided-onboarding checklist as the API describes it.
 *
 * <p>Every {@code done} is read from the user's own records at the time of the request, so it can never
 * disagree with them. Nothing here is a figure or a name: the response says that an asset exists, never
 * which one or what it is worth.
 */
@Schema(description = "The signed-in user's guided-onboarding checklist and where they are in it.")
public record OnboardingResponse(
        @Schema(description = "Where the user stands in the flow as a whole.", example = "IN_PROGRESS")
                OnboardingStatus status,
        @Schema(description = "The step the user last viewed, 1 to 7. 1 until the client records one.", example = "3")
                int currentStep,
        @ArraySchema(
                        arraySchema =
                                @Schema(description = "All seven steps, always in backbone order."),
                        minItems = 7,
                        maxItems = 7)
                List<Step> steps,
        @Schema(
                        description = "When the user first reviewed their dashboard. Null until the client records it.",
                        example = "2026-11-14T12:05:41.208Z")
                Instant dashboardReviewedAt,
        @Schema(description = "When the user completed the flow. Null unless status is COMPLETED.")
                Instant completedAt,
        @Schema(description = "When the user dismissed the flow. Null unless status is DISMISSED.")
                Instant dismissedAt) {

    /** One checklist step. */
    @Schema(name = "OnboardingStepState", description = "One checklist step and whether it is done now.")
    public record Step(
            @Schema(description = "The step's position, 1 to 7.", example = "1") int step,
            @Schema(description = "Which step this is. The client maps it to its copy.", example = "EXCHANGE_RATE")
                    OnboardingStep key,
            @Schema(
                            description =
                                    "Whether the user's records satisfy this step right now. Deleting the "
                                            + "last such record makes it false again.",
                            example = "true")
                    boolean done) {}

    static OnboardingResponse of(OnboardingView view) {
        return new OnboardingResponse(
                view.status(),
                view.currentStep(),
                view.steps().stream()
                        .map(state -> new Step(state.step().number(), state.step(), state.done()))
                        .toList(),
                view.dashboardReviewedAt(),
                view.completedAt(),
                view.dismissedAt());
    }
}
