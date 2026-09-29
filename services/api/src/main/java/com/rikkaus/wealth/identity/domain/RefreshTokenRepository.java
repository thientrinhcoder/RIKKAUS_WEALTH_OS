package com.rikkaus.wealth.identity.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/**
 * Refresh-token persistence, declaring only the four operations the slice uses.
 *
 * <p>Extends {@code Repository} rather than {@code JpaRepository} deliberately. The inherited
 * interface would expose {@code deleteAll}, {@code deleteAllInBatch} and the rest on a table whose
 * whole security value depends on rows being revoked rather than removed — a revoked row is what
 * distinguishes a replayed token from one that never existed. Declaring the surface explicitly means
 * a future caller has to add a method, and justify it in review, instead of finding a destructive one
 * already available.
 *
 * <p>It also makes an in-memory fake in a unit test a few lines instead of thirty stubs.
 */
public interface RefreshTokenRepository extends Repository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Every token in a rotation chain, including already-revoked ones.
     *
     * <p>Unfiltered on purpose: replay handling revokes the chain, and {@code revoke} is idempotent,
     * so filtering here would only add a predicate the caller does not need.
     */
    List<RefreshToken> findBySessionId(UUID sessionId);

    RefreshToken save(RefreshToken token);

    List<RefreshToken> saveAll(Iterable<RefreshToken> tokens);
}
