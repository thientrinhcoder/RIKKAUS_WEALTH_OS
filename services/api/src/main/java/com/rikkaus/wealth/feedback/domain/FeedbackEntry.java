package com.rikkaus.wealth.feedback.domain;

import com.rikkaus.wealth.shared.security.UserOwned;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * One piece of feedback a user sent from a screen of the app.
 *
 * <p>Immutable once written: there is no route to edit or delete feedback, so there is no method that
 * could. The owner is a bare {@link UUID} rather than an association, because the slice rule in
 * {@code ArchitectureRulesTest} forbids {@code feedback} from depending on {@code identity}; the
 * relationship is the foreign key in {@code V12__feedback_entries.sql}.
 *
 * <p>The message is product data that the author reads back. It must never be passed to a logger,
 * which is why this class deliberately has no {@code toString}.
 */
@Entity
@Table(name = "feedback_entries", schema = "wealth")
public class FeedbackEntry implements UserOwned {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(nullable = false, updatable = false)
    private String screen;

    @Enumerated(EnumType.STRING)
    @Column(updatable = false)
    private FeedbackCategory category;

    @Column(nullable = false, updatable = false)
    private String message;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Required by JPA. Not for application use. */
    protected FeedbackEntry() {}

    private FeedbackEntry(
            UUID id, UUID userId, String screen, FeedbackCategory category, String message, Instant now) {
        this.id = id;
        this.userId = userId;
        this.screen = screen;
        this.category = category;
        this.message = message;
        this.createdAt = now;
    }

    /** New feedback from the given author. The identifier is assigned here, never by the client. */
    public static FeedbackEntry create(
            UUID authorId, String screen, FeedbackCategory category, String message, Instant now) {
        return new FeedbackEntry(UUID.randomUUID(), authorId, screen, category, message, now);
    }

    @Override
    public UUID ownerId() {
        return userId;
    }

    public UUID getId() {
        return id;
    }

    public String getScreen() {
        return screen;
    }

    public FeedbackCategory getCategory() {
        return category;
    }

    public String getMessage() {
        return message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
