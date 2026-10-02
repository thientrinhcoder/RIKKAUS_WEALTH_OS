package com.rikkaus.wealth.identity.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/**
 * User persistence, declaring only the operations the slice uses.
 *
 * <p>Narrow for the same reason as {@link RefreshTokenRepository}: MVP 0 has no account-deletion
 * feature, so no deletion method should be reachable, and a lookup is always by the Google subject or
 * the primary key rather than by listing every user.
 */
public interface UserAccountRepository extends Repository<UserAccount, UUID> {

    Optional<UserAccount> findByGoogleSubject(String googleSubject);

    Optional<UserAccount> findById(UUID id);

    UserAccount save(UserAccount user);
}
