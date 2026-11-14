package com.rikkaus.wealth.shared.observability;

import java.io.IOException;
import org.springframework.boot.logging.StackTracePrinter;

/**
 * The {@code stack_trace} member of every JSON log line: class names and frames, no message.
 *
 * <p>Named by {@code logging.structured.json.stacktrace.printer} in {@code application.yml}. Without
 * it Spring Boot prints the standard trace, whose first line is the exception message — exactly where
 * a parse error quotes the request body and a constraint violation quotes the row.
 */
public class MessageFreeStackTracePrinter implements StackTracePrinter {

    @Override
    public void printStackTrace(Throwable throwable, Appendable out) throws IOException {
        LogRedaction.appendMessageFree(throwable, out);
    }
}
