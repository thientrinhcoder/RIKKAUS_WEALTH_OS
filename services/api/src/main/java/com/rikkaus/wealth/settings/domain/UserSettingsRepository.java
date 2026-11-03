package com.rikkaus.wealth.settings.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/**
 * Settings persistence, declaring only the operations the slice uses.
 *
 * <p>Narrow for the same reason as {@code UserAccountRepository}: MVP 1A has no feature that deletes a
 * user's settings and none that lists everybody's, so neither method should be reachable. A lookup is
 * always by the owner, which is also the primary key.
 */
public interface UserSettingsRepository extends Repository<UserSettings, UUID> {

    Optional<UserSettings> findById(UUID userId);

    UserSettings save(UserSettings settings);
}
