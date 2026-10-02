package com.rikkaus.wealth;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.lang.reflect.Parameter;
import java.util.regex.Pattern;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Enforces the architecture rules the decision record states and nothing previously checked.
 *
 * <p>Reads bytecode only, so this is a {@code *Test} and needs no database. Two of the three rules
 * match nothing while only {@code shared} exists, which is why they declare {@code
 * allowEmptyShould(true)}: they exist to fail the first time a later feature slice violates them.
 */
@AnalyzeClasses(packages = "com.rikkaus.wealth", importOptions = DoNotIncludeTests.class)
class ArchitectureRulesTest {

    /**
     * Monetary vocabulary. A name heuristic is weaker than a type, and it is deliberately narrow: a
     * blanket ban on every {@code double} would trip on the first legitimate non-monetary ratio and
     * invite a suppression that would take the other rules in this class down with it. When the
     * money primitives land, replace this with a rule requiring those types.
     */
    private static final String MONETARY_FIELD_NAME =
            "(?i).*(amount|balance|price|value|rate|total|cost).*";

    @ArchTest
    static final ArchRule featureSlicesDoNotDependOnEachOther =
            slices()
                    .matching("com.rikkaus.wealth.(*)..")
                    .namingSlices("$1")
                    .should()
                    .notDependOnEachOther()
                    // shared/ is the sanctioned cross-cutting package every slice may use.
                    .ignoreDependency(alwaysTrue(), resideInAPackage("com.rikkaus.wealth.shared.."))
                    .because(
                            "a feature must collaborate through another feature's application "
                                    + "interface, never reach into its internals")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule controllersLiveInAnApiPackage =
            classes()
                    .that()
                    .areAnnotatedWith(RestController.class)
                    .should()
                    .resideInAPackage("..api..")
                    .because("HTTP adapters belong in a slice's api package, not beside its domain")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule monetaryFieldsUseExactDecimalTypes =
            noFields()
                    .that()
                    .haveNameMatching(MONETARY_FIELD_NAME)
                    .should()
                    .haveRawType(double.class)
                    .orShould()
                    .haveRawType(Double.class)
                    .orShould()
                    .haveRawType(float.class)
                    .orShould()
                    .haveRawType(Float.class)
                    .because(
                            "money and exchange rates must use an exact decimal type such as "
                                    + "BigDecimal; binary floating point cannot represent decimal "
                                    + "fractions and silently loses cents")
                    .allowEmptyShould(true);

    /**
     * The decision record requires that ownership is enforced server-side and that a client-supplied
     * {@code userId} is never trusted ({@code ARCHITECTURE_TECHNOLOGY_DECISIONS.md:140}). This turns that
     * sentence into a build failure.
     *
     * <p>It bans a user identifier arriving as a {@code @PathVariable} or {@code @RequestParam} on a
     * controller method, which is the concrete shape the mistake takes: {@code GET /assets?userId=...} reads
     * as ordinary code and returns another person's records. The viewer's identity has exactly one
     * legitimate source, {@code CurrentUser}, which reads it from the verified access token.
     *
     * <p>What it deliberately does not claim: no static rule can force a slice to call {@code
     * OwnershipGuard} at all. The acceptance report names that as a residual gap. This catches the one
     * variant of the mistake that has no other detector.
     */
    @ArchTest
    static final ArchRule controllersNeverAcceptAUserIdentifierFromTheClient =
            methods()
                    .that()
                    .areDeclaredInClassesThat()
                    .areAnnotatedWith(RestController.class)
                    .should(notBindAUserIdentifierFromTheRequest())
                    .allowEmptyShould(true);

    /** Matches a parameter name or an explicit binding name that denotes a user identifier. */
    private static final Pattern USER_IDENTIFIER =
            Pattern.compile("(?i)^(user|owner|account)_?id$");

    private static ArchCondition<JavaMethod> notBindAUserIdentifierFromTheRequest() {
        return new ArchCondition<>("not bind a user identifier from the request") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                for (Parameter parameter : method.reflect().getParameters()) {
                    String boundName = requestBoundNameOf(parameter);
                    if (boundName != null && USER_IDENTIFIER.matcher(boundName).matches()) {
                        events.add(
                                SimpleConditionEvent.violated(
                                        method,
                                        "%s binds '%s' from the request; read the viewer from CurrentUser instead"
                                                .formatted(method.getFullName(), boundName)));
                    }
                }
            }
        };
    }

    /**
     * The name a parameter is bound to, or {@code null} if it is not bound from the request at all.
     *
     * <p>Checks the annotation's explicit name first and falls back to the compiled parameter name. The
     * fallback is what catches the common form {@code @PathVariable UUID userId}, which carries no explicit
     * name; it works because the Spring Boot parent POM compiles with {@code -parameters}. If that ever
     * changes this rule silently weakens, which is why the explicit-name check is kept as well rather than
     * relying on one of the two.
     */
    private static String requestBoundNameOf(Parameter parameter) {
        PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);
        if (pathVariable != null) {
            return !pathVariable.name().isEmpty()
                    ? pathVariable.name()
                    : !pathVariable.value().isEmpty() ? pathVariable.value() : parameter.getName();
        }
        RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
        if (requestParam != null) {
            return !requestParam.name().isEmpty()
                    ? requestParam.name()
                    : !requestParam.value().isEmpty() ? requestParam.value() : parameter.getName();
        }
        return null;
    }
}
