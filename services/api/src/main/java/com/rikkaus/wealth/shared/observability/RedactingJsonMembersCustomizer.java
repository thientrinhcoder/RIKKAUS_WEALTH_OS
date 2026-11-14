package com.rikkaus.wealth.shared.observability;

import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.logging.structured.StructuredLoggingJsonMembersCustomizer;

/**
 * Masks credential- and email-shaped strings in every string member of a JSON log line.
 *
 * <p>Named by {@code logging.structured.json.customizer} in {@code application.yml}. Applied to every
 * string value rather than to {@code message} alone, because MDC entries and key-value pairs become
 * top-level members too, and a future {@code MDC.put} of the wrong value must not be the one path that
 * escapes the rule.
 */
public class RedactingJsonMembersCustomizer implements StructuredLoggingJsonMembersCustomizer<Object> {

    @Override
    public void customize(JsonWriter.Members<Object> members) {
        members.applyingValueProcessor(JsonWriter.ValueProcessor.of(String.class, LogRedaction::redact));
    }
}
