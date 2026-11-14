package com.rikkaus.wealth.feedback.api;

import com.rikkaus.wealth.feedback.domain.FeedbackCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Feedback as the user sends it.
 *
 * <p>There is no author field and no timestamp: the author is whoever the access token authenticates
 * and the time is the server's. A client that sends {@code userId}, {@code id} or {@code createdAt}
 * anyway has them ignored, like any other unknown member.
 *
 * <p>The screen code is narrow on purpose. Lowercase ASCII segments joined by {@code -} or {@code /}
 * cannot carry a sentence, an amount written as {@code 4.200.000.000} or a query string with a token
 * in it, so the column is safe to group by and safe to show in an operator's report. The same pattern
 * is the {@code feedback_entries_screen_is_a_code} check in {@code V12__feedback_entries.sql}.
 */
@Schema(description = "Feedback sent from one screen of the app.")
public record FeedbackRequest(
        @Schema(
                        description =
                                "The screen the feedback was sent from, as a route-like code: lowercase ASCII "
                                        + "letters and digits in segments joined by '-' or '/', at most 64 "
                                        + "characters.",
                        example = "catalog/assets",
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        maxLength = 64,
                        pattern = FeedbackRequest.SCREEN_PATTERN)
                @NotBlank(message = "must not be blank")
                @Size(max = 64, message = "must be at most 64 characters")
                @Pattern(
                        regexp = FeedbackRequest.SCREEN_PATTERN,
                        message = "must be a lowercase screen code such as dashboard or catalog/assets")
                String screen,
        @Schema(
                        description = "What kind of feedback this is. Omit when the user did not choose one.",
                        example = "BUG")
                FeedbackCategory category,
        @Schema(
                        description =
                                "The user's own words. Surrounding blank space is removed; must not be blank, "
                                        + "at most 2000 characters, and must not contain U+0000.",
                        example =
                                "Màn hình danh mục tài sản không hiển thị tỷ giá cập nhật 12/09/2026 sau khi lưu.",
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        maxLength = 2000)
                @NotBlank(message = "must not be blank")
                @Size(max = 2000, message = "must be at most 2000 characters")
                String message) {

    static final String SCREEN_PATTERN = "^[a-z0-9]+(?:[-/][a-z0-9]+)*$";
}
