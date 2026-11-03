package com.rikkaus.wealth.settings.application;

import com.rikkaus.wealth.settings.domain.UserSettings;
import com.rikkaus.wealth.settings.domain.UserSettingsRepository;
import com.rikkaus.wealth.shared.error.ApiException;
import com.rikkaus.wealth.shared.error.ProblemType;
import com.rikkaus.wealth.shared.security.OwnershipGuard;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The settings slice's application service: read the reporting currency and the declared exchange
 * rate, and record a new declaration.
 *
 * <p>Every method takes the owner as its first argument and every caller gets that value from {@code
 * CurrentUser}, never from the request. The service has no method that can be asked about a user other
 * than the one named, which is what makes the resource impossible to address as somebody else.
 */
@Service
public class SettingsService {

    /**
     * The zone the declared date is judged in.
     *
     * <p>The {@link Clock} bean is UTC, and a pilot user in Vietnam is seven hours ahead of it. Judging
     * "is this date in the future" in UTC would refuse a user who declares today's rate at nine in the
     * evening, because UTC still calls it yesterday and their date therefore looks like tomorrow. The
     * declared date is a Vietnamese civil date, so it is compared against one.
     */
    private static final ZoneId DECLARED_DATE_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final UserSettingsRepository settings;
    private final Clock clock;

    SettingsService(UserSettingsRepository settings, Clock clock) {
        this.settings = settings;
        this.clock = clock;
    }

    /**
     * The viewer's settings, creating the default row on first read.
     *
     * <p>Created here rather than during sign-in because the identity slice must not know this slice
     * exists — the ArchUnit slice rule forbids the dependency, and inverting it would mean every new
     * feature slice edits the sign-in path to seed its own row.
     *
     * <p>That makes a {@code GET} a write on exactly one request per user. It is idempotent: the
     * primary key is the owner, so a concurrent duplicate read cannot insert twice.
     */
    @Transactional
    public UserSettings settingsFor(UUID userId) {
        return settings
                .findById(userId)
                .map(existing -> OwnershipGuard.requireOwned(existing, userId))
                .orElseGet(() -> settings.save(UserSettings.defaultsFor(userId, clock.instant())));
    }

    /**
     * Records the USD/VND rate the viewer declares, and the date they say it took effect.
     *
     * <p>The rate's shape — present, positive, within the column's scale — is refused at the API
     * boundary by bean validation. What cannot be expressed as an annotation is the date comparison,
     * because it depends on the clock, so it lives here.
     */
    @Transactional
    public UserSettings declareExchangeRate(
            UUID userId, BigDecimal usdToVndRate, LocalDate effectiveDate) {
        LocalDate today = LocalDate.now(clock.withZone(DECLARED_DATE_ZONE));
        if (effectiveDate.isAfter(today)) {
            // A declared date is a statement about when the rate took effect, so a future one is a
            // typing error rather than an intention. Refused with the same extension shape the identity
            // slice uses for a rejected redirect URI, so a client has one error body to parse.
            throw new ApiException(
                    ProblemType.VALIDATION_FAILED,
                    ProblemType.VALIDATION_FAILED.safeDetail(),
                    Map.of(
                            "errors",
                            List.of(
                                    Map.of("field", "effectiveDate", "message", "must not be in the future"))));
        }

        UserSettings current = settingsFor(userId);
        current.declareExchangeRate(usdToVndRate, effectiveDate, clock.instant());
        return settings.save(current);
    }
}
