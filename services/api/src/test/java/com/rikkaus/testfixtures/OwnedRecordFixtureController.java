package com.rikkaus.testfixtures;

import com.rikkaus.wealth.shared.security.CurrentUser;
import com.rikkaus.wealth.shared.security.OwnershipGuard;
import com.rikkaus.wealth.shared.security.UserOwned;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A user-owned resource, so ownership enforcement can be proved end to end. Test-only.
 *
 * <p>MVP 0 has no domain resource yet — assets, liabilities, cash flow and goals are later work — so there
 * is nothing real to guard. This stands in for the first one, and is written exactly the way a real slice
 * should write it: read the viewer from {@link CurrentUser}, look the record up by its own identifier, and
 * hand both to {@link OwnershipGuard}. It doubles as the worked example a later slice can copy.
 *
 * <p><strong>The unusual package is deliberate — do not move it under {@code com.rikkaus.wealth}.</strong>
 * The application's component scan covers that package across the whole classpath, compiled test classes
 * included, so a {@code @RestController} there becomes a live bean in every {@code @SpringBootTest}
 * context — including the one that generates the published OpenAPI contract, which would then advertise
 * routes that do not exist in production. Being outside the scan root and mapped off {@code /api/v1} are
 * the two things that prevent it. Register it with {@code @Import} on the tests that need it.
 */
@RestController
@RequestMapping("/test-fixtures/owned")
public class OwnedRecordFixtureController {

    /** In-memory, because what is under test is the ownership rule, not persistence. */
    private final Map<UUID, OwnedFixture> records = new ConcurrentHashMap<>();

    /** Creates a record owned by the caller and returns it, including its generated identifier. */
    @PostMapping
    OwnedFixture createForCurrentUser(@RequestBody LabelRequest request) {
        OwnedFixture record = new OwnedFixture(UUID.randomUUID(), CurrentUser.require(), request.label());
        records.put(record.id(), record);
        return record;
    }

    @GetMapping("/{recordId}")
    OwnedFixture read(@PathVariable UUID recordId) {
        // The shape every real slice should copy: the viewer comes from the verified token, never from the
        // request, and the guard decides. There is no branch here that could return someone else's record.
        return OwnershipGuard.requireOwned(
                Optional.ofNullable(records.get(recordId)), CurrentUser.require());
    }

    @PutMapping("/{recordId}")
    OwnedFixture update(@PathVariable UUID recordId, @RequestBody LabelRequest request) {
        // A write goes through the identical check. Guarding reads and forgetting writes is the usual way
        // this gets half-done, so the contract test exercises both.
        OwnedFixture existing =
                OwnershipGuard.requireOwned(
                        Optional.ofNullable(records.get(recordId)), CurrentUser.require());
        OwnedFixture updated = new OwnedFixture(existing.id(), existing.ownerId(), request.label());
        records.put(updated.id(), updated);
        return updated;
    }

    public record OwnedFixture(UUID id, UUID ownerId, String label) implements UserOwned {}

    public record LabelRequest(String label) {}
}
