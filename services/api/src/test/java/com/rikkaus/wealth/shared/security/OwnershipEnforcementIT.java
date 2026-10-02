package com.rikkaus.wealth.shared.security;

import com.rikkaus.testfixtures.OwnedRecordFixtureController;
import com.rikkaus.wealth.identity.application.IdentityService.AuthenticatedSession;
import com.rikkaus.wealth.support.OwnershipContract;
import java.util.UUID;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;

/**
 * Proves the ownership contract against a real user-owned resource over HTTP.
 *
 * <p>Deliberately thin: every assertion lives in {@link OwnershipContract}, and this class supplies only the
 * two hooks. That is the point — it is also the worked example of how little a later slice has to write to
 * inherit the whole rule set. If this file grows assertions of its own, they belonged in the contract.
 */
@Import(OwnedRecordFixtureController.class)
class OwnershipEnforcementIT extends OwnershipContract {

    private static final String BASE = "/test-fixtures/owned";

    @Override
    protected String createRecordOwnedBy(AuthenticatedSession owner) {
        String id =
                objectMapper
                        .readTree(
                                restTemplate
                                        .exchange(
                                                BASE,
                                                HttpMethod.POST,
                                                jsonAs(owner, "{\"label\":\"Căn hộ Thảo Điền\"}"),
                                                String.class)
                                        .getBody())
                        .get("id")
                        .asString();
        return BASE + "/" + id;
    }

    @Override
    protected String pathOfARecordThatDoesNotExist() {
        // A well-formed UUID that was never created. A malformed one would bind-fail into a 400 and prove
        // nothing about whether a denial and a miss look alike.
        return BASE + "/" + UUID.randomUUID();
    }
}
