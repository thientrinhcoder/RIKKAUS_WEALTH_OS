package com.rikkaus.wealth.shared.observability;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * {@code %redactedMsg}: the log message with credential- and email-shaped strings masked.
 *
 * <p>Registered in {@code logback-spring.xml} and used in place of {@code %msg} by the readable console
 * pattern of the {@code local} profile. The JSON output applies the same rule through {@link
 * RedactingJsonMembersCustomizer}.
 */
public class RedactedMessageConverter extends ClassicConverter {

    @Override
    public String convert(ILoggingEvent event) {
        return LogRedaction.redact(event.getFormattedMessage());
    }
}
