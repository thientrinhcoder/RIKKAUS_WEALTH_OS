package com.rikkaus.wealth.feedback.application;

import com.rikkaus.wealth.feedback.domain.FeedbackCategory;
import com.rikkaus.wealth.feedback.domain.FeedbackEntry;
import com.rikkaus.wealth.feedback.domain.FeedbackEntryRepository;
import com.rikkaus.wealth.shared.error.ApiException;
import com.rikkaus.wealth.shared.error.ProblemType;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The feedback slice's application service: send and list, nothing else.
 *
 * <p>Every method takes the author as its first argument, and every caller gets that value from {@code
 * CurrentUser}. There is no method that can be asked about another user's feedback.
 *
 * <p>Nothing here logs. The message is the user's own words and may well name an apartment or an
 * amount; the request log line written by {@code RequestLoggingFilter} records that a feedback was
 * sent, and that is all any log may say about it.
 */
@Service
public class FeedbackService {

    private final FeedbackEntryRepository entries;
    private final Clock clock;

    FeedbackService(FeedbackEntryRepository entries, Clock clock) {
        this.entries = entries;
        this.clock = clock;
    }

    /** The author's feedback, newest first. Unpaginated: a pilot user sends a handful. */
    @Transactional(readOnly = true)
    public List<FeedbackEntry> listFor(UUID authorId) {
        return entries.findByUserIdOrderByCreatedAtDescIdDesc(authorId);
    }

    /**
     * Stores one piece of feedback.
     *
     * <p>The screen code and the length rules are bean-validation annotations on the request. The two
     * rules annotations cannot express are judged here: the null character, which PostgreSQL cannot
     * store in a {@code text} column and would otherwise surface as a 500, and blankness in the
     * Unicode sense, which {@code @NotBlank} does not cover — a message of one non-breaking space, as
     * a Vietnamese input method can produce, passes it.
     *
     * <p>The timestamp is truncated to microseconds, the precision of {@code TIMESTAMPTZ}, so the
     * create response says exactly what every later {@code GET} will say.
     */
    @Transactional
    public FeedbackEntry send(UUID authorId, String screen, FeedbackCategory category, String message) {
        if (message.indexOf('\u0000') >= 0) {
            throw invalidMessage("must not contain a null character");
        }
        String stripped = stripped(message);
        if (stripped.isEmpty()) {
            throw invalidMessage("must not be blank");
        }
        return entries.save(
                FeedbackEntry.create(
                        authorId,
                        screen,
                        category,
                        stripped,
                        clock.instant().truncatedTo(ChronoUnit.MICROS)));
    }

    private static ApiException invalidMessage(String reason) {
        return new ApiException(
                ProblemType.VALIDATION_FAILED,
                ProblemType.VALIDATION_FAILED.safeDetail(),
                Map.of("errors", List.of(Map.of("field", "message", "message", reason))));
    }

    /**
     * The message with surrounding blank space removed, where blank is Java whitespace plus every
     * Unicode space separator — the same rule the assets and cash-flow slices apply to their text.
     */
    private static String stripped(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && isBlankToAReader(value.codePointAt(start))) {
            start += Character.charCount(value.codePointAt(start));
        }
        while (end > start && isBlankToAReader(value.codePointBefore(end))) {
            end -= Character.charCount(value.codePointBefore(end));
        }
        return value.substring(start, end);
    }

    private static boolean isBlankToAReader(int codePoint) {
        return Character.isWhitespace(codePoint)
                || Character.getType(codePoint) == Character.SPACE_SEPARATOR;
    }
}
