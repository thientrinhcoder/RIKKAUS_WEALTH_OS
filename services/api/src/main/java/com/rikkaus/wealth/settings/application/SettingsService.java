package com.rikkaus.wealth.settings.application;

import com.rikkaus.wealth.settings.domain.UserSettings;
import com.rikkaus.wealth.settings.domain.UserSettingsRepository;
import com.rikkaus.wealth.shared.security.OwnershipGuard;
import com.rikkaus.wealth.shared.validation.EnteredDates;
import com.rikkaus.wealth.shared.validation.FieldErrors;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
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
        LocalDate today = EnteredDates.today(clock);
        if (effectiveDate.isAfter(today)) {
            // A declared date is a statement about when the rate took effect, so a future one is a
            // typing error rather than an intention. Refused with the same extension shape the identity
            // slice uses for a rejected redirect URI, so a client has one error body to parse.
            throw FieldErrors.validationFailed("effectiveDate", EnteredDates.NOT_IN_FUTURE);
        }

        UserSettings current = settingsFor(userId);
        current.declareExchangeRate(usdToVndRate, effectiveDate, clock.instant());
        return settings.save(current);
    }
}
