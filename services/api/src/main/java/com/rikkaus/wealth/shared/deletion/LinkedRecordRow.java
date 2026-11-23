package com.rikkaus.wealth.shared.deletion;

import java.util.UUID;

/**
 * The projection a slice's native query selects into: {@code id}, {@code label}, {@code category}.
 *
 * <p>An interface because that is the projection Spring Data maps a native query's column aliases
 * onto; the slice's query aliases its own columns to these three names.
 */
public interface LinkedRecordRow {

    UUID getId();

    String getLabel();

    String getCategory();
}
