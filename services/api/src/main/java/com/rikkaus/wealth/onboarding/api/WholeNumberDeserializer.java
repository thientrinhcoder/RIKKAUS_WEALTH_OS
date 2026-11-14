package com.rikkaus.wealth.onboarding.api;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/**
 * Reads an integer member only from a JSON integer.
 *
 * <p>Jackson's default accepts a fraction for an {@code Integer} and truncates it, so {@code
 * "currentStep": 2.5} would be stored as step 2 with a 200 — the client asked for something that does
 * not exist and was told it succeeded. Refusing every token but an integer makes it a {@code
 * malformed-request}, the answer an unrecognised enum constant already gets. Scoped to this request
 * rather than set on the shared mapper, because changing coercion for every slice is not this
 * resource's decision.
 */
final class WholeNumberDeserializer extends ValueDeserializer<Integer> {

    @Override
    public Integer deserialize(JsonParser parser, DeserializationContext context) {
        if (parser.currentToken() == JsonToken.VALUE_NUMBER_INT) {
            return parser.getIntValue();
        }
        return (Integer) context.handleUnexpectedToken(Integer.class, parser);
    }
}
