package com.rikkaus.wealth.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.rikkaus.wealth.shared.error.ApiException;
import com.rikkaus.wealth.shared.error.ProblemType;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The non-disclosure rule: absent and someone-else's must be indistinguishable. */
class OwnershipGuardTest {

    private static final UUID VIEWER = UUID.randomUUID();
    private static final UUID SOMEONE_ELSE = UUID.randomUUID();

    private record OwnedRecord(UUID ownerId, String name) implements UserOwned {}

    @Test
    void theOwnerGetsTheirRecord() {
        OwnedRecord record = new OwnedRecord(VIEWER, "Căn hộ Thảo Điền");

        assertThat(OwnershipGuard.requireOwned(Optional.of(record), VIEWER)).isSameAs(record);
    }

    @Test
    void anAbsentRecordIsNotFound() {
        ApiException thrown = rejectionFor(Optional.empty());

        assertThat(thrown.getProblemType()).isEqualTo(ProblemType.NOT_FOUND);
    }

    @Test
    void someoneElsesRecordIsAlsoNotFound() {
        ApiException thrown =
                rejectionFor(Optional.of(new OwnedRecord(SOMEONE_ELSE, "Căn hộ Thảo Điền")));

        // Not forbidden. A 403 would confirm the record exists and turn a list of identifiers into an
        // enumeration oracle.
        assertThat(thrown.getProblemType()).isEqualTo(ProblemType.NOT_FOUND);
    }

    @Test
    void theTwoRejectionsAreIndistinguishable() {
        // The property that actually protects the user, asserted directly rather than inferred from two
        // separate status-code checks: same type, same detail, same extension members.
        ApiException absent = rejectionFor(Optional.empty());
        ApiException someoneElses =
                rejectionFor(Optional.of(new OwnedRecord(SOMEONE_ELSE, "Căn hộ Thảo Điền")));

        assertThat(someoneElses.getProblemType()).isEqualTo(absent.getProblemType());
        assertThat(someoneElses.getMessage()).isEqualTo(absent.getMessage());
        assertThat(someoneElses.getExtensions()).isEqualTo(absent.getExtensions());
    }

    @Test
    void theDetailNeverMentionsOwnershipOrAnotherUser() {
        ApiException thrown =
                rejectionFor(Optional.of(new OwnedRecord(SOMEONE_ELSE, "Căn hộ Thảo Điền")));

        assertThat(thrown.getMessage())
                .isEqualTo(ProblemType.NOT_FOUND.safeDetail())
                .doesNotContainIgnoringCase("owner")
                .doesNotContainIgnoringCase("permission")
                .doesNotContain(SOMEONE_ELSE.toString())
                .doesNotContain("Căn hộ Thảo Điền");
    }

    @Test
    void aNullRecordIsNotFoundRatherThanAServerError() {
        ApiException thrown =
                catchThrowableOfType(
                        ApiException.class, () -> OwnershipGuard.requireOwned((OwnedRecord) null, VIEWER));

        // A repository that returns null must not turn a correct 404 into a 500.
        assertThat(thrown.getProblemType()).isEqualTo(ProblemType.NOT_FOUND);
    }

    @Test
    void theRecordInHandOverloadAppliesTheSameRule() {
        OwnedRecord mine = new OwnedRecord(VIEWER, "Sổ tiết kiệm");

        assertThat(OwnershipGuard.requireOwned(mine, VIEWER)).isSameAs(mine);
        assertThat(
                        catchThrowableOfType(
                                        ApiException.class,
                                        () ->
                                                OwnershipGuard.requireOwned(
                                                        new OwnedRecord(SOMEONE_ELSE, "Sổ tiết kiệm"), VIEWER))
                                .getProblemType())
                .isEqualTo(ProblemType.NOT_FOUND);
    }

    @Test
    void theBooleanFormAgreesWithTheThrowingForm() {
        assertThat(OwnershipGuard.isOwnedBy(new OwnedRecord(VIEWER, "A"), VIEWER)).isTrue();
        assertThat(OwnershipGuard.isOwnedBy(new OwnedRecord(SOMEONE_ELSE, "A"), VIEWER)).isFalse();
        assertThat(OwnershipGuard.isOwnedBy(null, VIEWER)).isFalse();
    }

    private static ApiException rejectionFor(Optional<OwnedRecord> candidate) {
        return catchThrowableOfType(
                ApiException.class, () -> OwnershipGuard.requireOwned(candidate, VIEWER));
    }
}
