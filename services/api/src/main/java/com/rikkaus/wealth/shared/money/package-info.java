/**
 * Exact-decimal money, exchange rates and the conversion trail between them.
 *
 * <p>Lives under {@code shared} because every later feature slice needs an amount of money, and the
 * architecture decisions name {@code shared} as the only package a slice may depend on.
 *
 * <p>These types carry no Spring, JPA or Jackson annotation, and that is deliberate. A consumer
 * decides how to store and serialize them; inheriting a column type or a JSON shape from here would
 * settle a decision this package has no standing to make. The first slice that stores money chooses
 * the column shape.
 *
 * <p>Display is not here either. Grouping separators, the dong symbol and relative time are owned by
 * the frontend.
 */
package com.rikkaus.wealth.shared.money;
