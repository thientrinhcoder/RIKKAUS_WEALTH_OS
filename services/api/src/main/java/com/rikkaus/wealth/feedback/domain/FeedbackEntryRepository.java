package com.rikkaus.wealth.feedback.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/**
 * Feedback persistence, declaring only the two operations the slice uses.
 *
 * <p>There is no {@code findAll} and no {@code findById}: the only legitimate read is one author's own
 * feedback, so a method that could return anybody else's is not declared at all. The order is part of
 * the method name, newest first with the identifier breaking a tie, so two entries stored in the same
 * microsecond do not swap places between requests.
 */
public interface FeedbackEntryRepository extends Repository<FeedbackEntry, UUID> {

    List<FeedbackEntry> findByUserIdOrderByCreatedAtDescIdDesc(UUID userId);

    FeedbackEntry save(FeedbackEntry entry);
}
