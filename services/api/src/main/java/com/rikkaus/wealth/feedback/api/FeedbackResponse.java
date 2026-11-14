package com.rikkaus.wealth.feedback.api;

import com.rikkaus.wealth.feedback.domain.FeedbackCategory;
import com.rikkaus.wealth.feedback.domain.FeedbackEntry;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/**
 * One piece of the signed-in user's feedback.
 *
 * <p>No author field: the caller is the author of everything this resource returns.
 */
@Schema(description = "Feedback the signed-in user sent.")
public record FeedbackResponse(
        @Schema(description = "The feedback's identifier.", example = "4b7e2c19-8a3d-4f60-9e15-2c8d7a1f5b36")
                UUID id,
        @Schema(description = "The screen code it was sent from.", example = "catalog/assets") String screen,
        @Schema(description = "BUG, IDEA, QUESTION or OTHER; null when the user did not choose one.", example = "BUG")
                FeedbackCategory category,
        @Schema(
                        description = "The user's own words, as stored.",
                        example =
                                "Màn hình danh mục tài sản không hiển thị tỷ giá cập nhật 12/09/2026 sau khi lưu.")
                String message,
        @Schema(description = "When the server stored it.", example = "2026-11-14T11:52:07.318204Z")
                Instant createdAt) {

    static FeedbackResponse of(FeedbackEntry entry) {
        return new FeedbackResponse(
                entry.getId(),
                entry.getScreen(),
                entry.getCategory(),
                entry.getMessage(),
                entry.getCreatedAt());
    }
}
