/**
 * Search, filter and sort parameters shared by the list routes.
 *
 * <p>What lives here is the part every slice reads the same way: the free-text {@code q} and how it
 * matches, multi-valued enum filters, strict dates, and the closed set of {@code sort} keys. What does
 * not live here is any query: each slice builds its own, with the owner's identifier as the first
 * predicate, in a repository fragment of its own.
 */
package com.rikkaus.wealth.shared.search;
