package com.rikkaus.wealth.goals.projection.application;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The system reference thresholds that turn a contribution coverage into a status.
 *
 * <p>Coverage is the planned monthly contribution as a percentage of the required one. At or above
 * {@code onTrackMinCoveragePercent} a goal is on track, at or above {@code atRiskMinCoveragePercent}
 * it is at risk, and below that it is behind. The accepted design for issue #87 shows 100 and 80 and
 * names them a <em>system reference threshold</em> rather than a rule of finance, which is why they
 * live in configuration in one place: changing them is a deployment decision, not a code change, and
 * every response states the values it was judged with.
 *
 * <p>There is no default in this record. The values are in {@code application.yml}, and a missing or
 * inconsistent pair stops the application at start-up with the property named, because a status
 * judged against a threshold nobody chose would be a silent product decision.
 */
@ConfigurationProperties("rikkaus.goals.projection")
public record GoalProjectionProperties(BigDecimal onTrackMinCoveragePercent, BigDecimal atRiskMinCoveragePercent) {

    public GoalProjectionProperties {
        if (onTrackMinCoveragePercent == null) {
            throw new IllegalStateException("rikkaus.goals.projection.on-track-min-coverage-percent is not set.");
        }
        if (atRiskMinCoveragePercent == null) {
            throw new IllegalStateException("rikkaus.goals.projection.at-risk-min-coverage-percent is not set.");
        }
        if (atRiskMinCoveragePercent.signum() <= 0) {
            throw new IllegalStateException(
                    "rikkaus.goals.projection.at-risk-min-coverage-percent must be greater than zero.");
        }
        if (atRiskMinCoveragePercent.compareTo(onTrackMinCoveragePercent) >= 0) {
            throw new IllegalStateException(
                    "rikkaus.goals.projection.at-risk-min-coverage-percent must be less than "
                            + "rikkaus.goals.projection.on-track-min-coverage-percent.");
        }
    }
}
