package com.rikkaus.wealth.onboarding.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Onboarding persistence, declaring only the operations the slice uses.
 *
 * <p>Narrow for the same reason as {@code UserSettingsRepository}: a lookup is always by the owner,
 * which is also the primary key, and nothing lists or deletes progress.
 */
public interface OnboardingProgressRepository extends Repository<OnboardingProgress, UUID> {

    Optional<OnboardingProgress> findById(UUID userId);

    OnboardingProgress save(OnboardingProgress progress);

    /**
     * Creates the owner's row with the default progress unless it already exists.
     *
     * <p>An upsert rather than {@code save} on a new entity, because two first writes from the same
     * user — a double tap, two tabs — would otherwise both see no row, both insert, and the loser would
     * reach the client as a 500 from the primary key. {@code ON CONFLICT DO NOTHING} makes the second
     * insert wait for the first and then do nothing, so every caller can read the row afterwards.
     */
    @Modifying
    @Query(
            value =
                    "INSERT INTO wealth.onboarding_progress (user_id, current_step, created_at, updated_at) "
                            + "VALUES (:userId, 1, :now, :now) ON CONFLICT (user_id) DO NOTHING",
            nativeQuery = true)
    int insertIfAbsent(@Param("userId") UUID userId, @Param("now") Instant now);

    /**
     * Which record-backed checklist steps the owner's records satisfy right now.
     *
     * <p>A native query against the tables of four other slices, because the slice rule in {@code
     * ArchitectureRulesTest} forbids {@code onboarding} from importing any of them — the approach {@code
     * cashflow} already takes to check a linked asset. Each clause is an {@code EXISTS} over the owner
     * column every one of those tables indexes through its foreign key, and none selects a value: the
     * answer is six booleans, so no amount, name or note ever enters this slice.
     *
     * <p>The rate step asks for a declared rate rather than a settings row, because reading the rate
     * settings creates the row with every rate column null, and that is the "rate needed" state, not a
     * declaration.
     *
     * <p>Cash and bank accounts are the {@code CASH} and {@code BANK_ACCOUNT} categories; every other
     * category, a term deposit included, counts as a major asset. A recurring cash-flow entry counts
     * whether it is income or expense and whether or not it has an end date.
     *
     * <p>The aliases are quoted so PostgreSQL keeps their case and they match the getters of {@link
     * RecordedSteps}.
     */
    @Query(
            value =
                    """
                    SELECT
                      EXISTS (SELECT 1 FROM wealth.user_settings s
                              WHERE s.user_id = :ownerId AND s.usd_to_vnd_rate IS NOT NULL)
                        AS "exchangeRateDeclared",
                      EXISTS (SELECT 1 FROM wealth.assets a
                              WHERE a.user_id = :ownerId AND a.category IN ('CASH', 'BANK_ACCOUNT'))
                        AS "cashOrBankAccountAdded",
                      EXISTS (SELECT 1 FROM wealth.assets a
                              WHERE a.user_id = :ownerId AND a.category NOT IN ('CASH', 'BANK_ACCOUNT'))
                        AS "majorAssetAdded",
                      EXISTS (SELECT 1 FROM wealth.liabilities l WHERE l.user_id = :ownerId)
                        AS "liabilityAdded",
                      EXISTS (SELECT 1 FROM wealth.cash_flow_entries c
                              WHERE c.user_id = :ownerId AND c.timing = 'RECURRING')
                        AS "recurringCashFlowAdded",
                      EXISTS (SELECT 1 FROM wealth.goals g WHERE g.user_id = :ownerId)
                        AS "goalAdded"
                    """,
            nativeQuery = true)
    RecordedSteps recordedStepsOf(@Param("ownerId") UUID ownerId);
}
