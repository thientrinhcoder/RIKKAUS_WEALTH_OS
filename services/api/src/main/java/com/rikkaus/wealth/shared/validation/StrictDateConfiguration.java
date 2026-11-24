package com.rikkaus.wealth.shared.validation;

import java.time.LocalDate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.module.SimpleModule;

/**
 * Reads every {@link LocalDate} in a JSON request body by {@link EnteredDates#parse}, and nothing else.
 *
 * <p>Jackson's own date reader is lenient by default, and every leniency was measured to store a date
 * other than the one the user meant: an integer was read as a day count from 1970 ({@code 20000} became
 * {@code 2024-10-04}), an array {@code [2026,9,12]} as its parts, a date-time had its time dropped, and
 * a UTC instant such as {@code 2026-09-12T20:00:00Z} became the UTC date, a day before the Vietnamese
 * date of that moment. Signed and five-digit years went straight to PostgreSQL, which refused one as a
 * 500 and silently shifted another.
 *
 * <p>A refused value is a {@code malformed-request}, as any other value Jackson cannot read, and as the
 * contract already says of a date that is not ISO 8601. An empty string stays {@code null}, as it was,
 * so a cleared optional date is still "not given" and a cleared required one is still {@code must not be
 * null} naming the field.
 */
@Configuration(proxyBeanMethods = false)
class StrictDateConfiguration {

    @Bean
    JacksonModule strictCalendarDates() {
        return new SimpleModule("strict-calendar-dates")
                .addDeserializer(LocalDate.class, new StrictLocalDateDeserializer());
    }

    static final class StrictLocalDateDeserializer extends ValueDeserializer<LocalDate> {

        @Override
        public LocalDate deserialize(JsonParser parser, DeserializationContext context) {
            if (parser.currentToken() != JsonToken.VALUE_STRING) {
                return (LocalDate) context.handleUnexpectedToken(LocalDate.class, parser);
            }
            String text = parser.getString();
            if (text.isEmpty()) {
                return null;
            }
            LocalDate date = EnteredDates.parse(text);
            if (date == null) {
                return (LocalDate)
                        context.handleWeirdStringValue(LocalDate.class, text, EnteredDates.CALENDAR_DATE);
            }
            return date;
        }
    }
}
