package com.rikkaus.wealth.shared.security;

import java.util.UUID;

/**
 * A record that belongs to exactly one user.
 *
 * <p>Implemented by every user-owned entity — assets, liabilities, cash-flow entries, goals — so
 * {@link OwnershipGuard} can check any of them without knowing what they are. MVP 0 has one owner per
 * record and no household, shared access or role, so a single owner identifier is the whole model.
 */
public interface UserOwned {

    UUID ownerId();
}
