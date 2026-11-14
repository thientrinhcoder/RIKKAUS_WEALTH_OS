package com.rikkaus.wealth.shared.observability;

import ch.qos.logback.classic.pattern.ThrowableHandlingConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * {@code %safeEx}: the event's exception as class names and stack frames, with no message.
 *
 * <p>Registered in {@code logback-spring.xml} and used by the {@code local} console pattern in place of
 * Spring Boot's {@code %wEx}. Extending {@link ThrowableHandlingConverter} is load-bearing: Logback
 * appends its own message-bearing {@code %ex} to any pattern that has no throwable converter, so a
 * plain converter here would print every exception twice, once with its message.
 */
public class MessageFreeThrowableConverter extends ThrowableHandlingConverter {

    @Override
    public String convert(ILoggingEvent event) {
        IThrowableProxy proxy = event.getThrowableProxy();
        if (proxy == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        if (proxy instanceof ThrowableProxy live) {
            try {
                LogRedaction.appendMessageFree(live.getThrowable(), out);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            // A deserialised event carries no live throwable; its class name is all that is safe.
            out.append(proxy.getClassName()).append(System.lineSeparator());
        }
        return out.toString();
    }
}
