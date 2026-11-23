/**
 * What deleting a record does to the user's other records, said before it happens.
 *
 * <p>Every slice that owns a deletable record answers {@code GET .../deletion-impact} with a {@link
 * com.rikkaus.wealth.shared.deletion.DeletionImpact} computed from the foreign keys that actually exist,
 * and a slice whose delete would change a record the user manages elsewhere refuses that delete with
 * {@code linked-records} until the client confirms it. The types live here because the four slices may
 * depend on {@code shared} and on nothing else; each slice still runs its own queries.
 */
package com.rikkaus.wealth.shared.deletion;
