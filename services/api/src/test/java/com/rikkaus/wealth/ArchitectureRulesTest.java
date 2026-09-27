package com.rikkaus.wealth;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
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
}
