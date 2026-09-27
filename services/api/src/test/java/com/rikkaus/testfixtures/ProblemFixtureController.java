package com.rikkaus.testfixtures;

import com.rikkaus.wealth.shared.error.ApiException;
import com.rikkaus.wealth.shared.error.ProblemType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Raises each class of error so the Problem Details contract can be asserted. Test-only.
 *
 * <p><strong>The unusual package is deliberate — do not "tidy" it into {@code
 * com.rikkaus.wealth}.</strong> {@code RikkausWealthApplication} is a {@code @SpringBootApplication}
 * in {@code com.rikkaus.wealth}, so its component scan covers {@code com.rikkaus.wealth.**} across the
 * whole classpath, including {@code target/test-classes}. A {@code @RestController} there would be a
 * live bean in every {@code @SpringBootTest} context — including the one that generates the published
 * OpenAPI contract — and with the contract scoped to {@code /api/v1/**} these routes would be
 * serialized into the committed document, publishing endpoints that do not exist in production. The
 * drift assertion could not catch it either, because the fixture would appear on both sides of the
 * comparison.
 *
 * <p>Two things prevent that: this package is outside the scan root, and the routes are mapped off
 * {@code /api/v1}. Register it explicitly with {@code @Import} on the tests that need it.
 */
@RestController
@RequestMapping("/test-fixtures")
public class ProblemFixtureController {

    /** Bean validation failure, for the per-field {@code errors} member. */
    @PostMapping("/validated")
    String validated(@Valid @RequestBody FixtureRequest request) {
        return request.name();
    }

    /** A domain error raised the way a real feature slice would raise it. */
    @GetMapping("/domain")
    String domain() {
        throw new ApiException(ProblemType.NOT_FOUND, "Fixture not found");
    }

    /** An unexpected failure, to prove the last-resort handler leaks nothing from the message. */
    @GetMapping("/boom")
    String boom() {
        throw new IllegalStateException("internal detail that must not leak");
    }

    /** Type conversion failure, one of the statuses where Spring's own detail echoes the input. */
    @GetMapping("/typed/{id}")
    FixtureResponse typed(@PathVariable int id) {
        return new FixtureResponse(id);
    }

    public record FixtureRequest(@NotBlank(message = "must not be blank") String name) {}

    /**
     * Returned as an object rather than a String on purpose. {@code StringHttpMessageConverter}
     * advertises {@code * / *}, so a String-returning endpoint happily serves an {@code Accept:
     * application/pdf} request with HTTP 200 and never produces the 406 this fixture needs.
     */
    public record FixtureResponse(int id) {}
}
