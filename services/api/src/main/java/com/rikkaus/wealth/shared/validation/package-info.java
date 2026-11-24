/**
 * The input rules every slice applies the same way, so one kind of mistake is judged by one
 * definition and answered with one sentence wherever it is made.
 *
 * <p>Three things live here. {@link com.rikkaus.wealth.shared.validation.TextInput} cleans free text
 * and refuses what PostgreSQL cannot store. {@link com.rikkaus.wealth.shared.validation.EnteredDates}
 * is the Vietnamese civil "today" a user-entered date is judged against, and the one grammar a date
 * must have to be read at all. {@link com.rikkaus.wealth.shared.validation.FieldErrors} builds the
 * {@code validation-failed} body with its {@code errors} array. Rules that belong to one resource — an
 * asset's rate snapshot, a liability's term order — stay in that resource's slice.
 *
 * <p>Every message is English, short, names no value the caller sent, and is a constant here or on the
 * annotation, so a client can switch on {@code field} and never has to parse a message.
 */
package com.rikkaus.wealth.shared.validation;
