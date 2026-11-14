package com.rikkaus.wealth.feedback.domain;

/**
 * What kind of feedback the user says they are sending.
 *
 * <p>Optional on every entry: a missing category is stored as {@code null}, meaning "the user did not
 * say", which is different from {@link #OTHER}. The four values are checked again by
 * {@code feedback_entries_category_is_known} in {@code V12__feedback_entries.sql}.
 */
public enum FeedbackCategory {
    BUG,
    IDEA,
    QUESTION,
    OTHER
}
