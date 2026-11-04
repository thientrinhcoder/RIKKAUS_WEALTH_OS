/**
 * The project's time conventions: what is stored, in which zone, and which of the two kinds of time
 * fact a given value is.
 *
 * <p>The distinction this package exists to keep visible is that a business date and a system
 * timestamp are not the same thing. "Giá trị tại ngày 12/09/2026" is a date-only fact that does not
 * move with a timezone, and belongs in a {@code LocalDate}. "Cập nhật lúc 09:30, 13/09/2026 (GMT+7)"
 * is a moment, stored in UTC as an {@code Instant} and rendered in the reader's zone. Collapsing
 * either into the other is how a valuation appears to shift a day for a user near midnight.
 *
 * <p>There is deliberately no wrapper type around {@code LocalDate} or {@code Instant}. The JDK types
 * already carry the distinction; a wrapper would add a conversion at every boundary and no guarantee.
 * What this package adds is the two zones and the clock-driven accessors, so no caller reaches for
 * the server's default zone.
 */
package com.rikkaus.wealth.shared.time;
