package com.rikkaus.wealth.identity.application;

import com.rikkaus.wealth.identity.application.IdentityService.AuthenticatedSession;
import com.rikkaus.wealth.identity.domain.UserAccount;
import com.rikkaus.wealth.identity.domain.UserAccountRepository;
import com.rikkaus.wealth.shared.validation.TextInput;
import java.time.Clock;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Signs a seeded user in without Google, for local development only.
 *
 * <p>Exists so a developer or an agent can exercise the client against real data on a machine that has
 * no Google OAuth client and must not sign in to a real Google account. The bean is registered under the
 * {@code local} profile and nowhere else, so in every deployed environment there is no code path at all
 * that creates a session without Google.
 *
 * <p>The account's identity key is the synthetic subject {@code local-dev|<email>}. It can never collide
 * with a real Google {@code sub}, which is a numeric string, so a development account can never be
 * mistaken for, or attached to, a real one. The session is issued by {@link IdentityService#startSession},
 * the same path Google sign-in uses, so tokens, rotation and revocation behave exactly as in production.
 */
@Service
@Profile("local")
public class LocalDevSignInService {

    static final String SUBJECT_PREFIX = "local-dev|";

    private static final Logger log = LoggerFactory.getLogger(LocalDevSignInService.class);

    private final IdentityService identityService;
    private final UserAccountRepository users;
    private final Clock clock;

    LocalDevSignInService(IdentityService identityService, UserAccountRepository users, Clock clock) {
        this.identityService = identityService;
        this.users = users;
        this.clock = clock;
    }

    /** Signs in as the development account for this email, creating it on first use. */
    @Transactional
    public AuthenticatedSession signIn(String email, String displayName) {
        String normalisedEmail = email.trim().toLowerCase(Locale.ROOT);
        String name = TextInput.optional(displayName, "displayName");
        UserAccount user =
                users.findByGoogleSubject(SUBJECT_PREFIX + normalisedEmail)
                        .map(
                                existing -> {
                                    existing.refreshProfileFromGoogle(normalisedEmail, name, clock.instant());
                                    return users.save(existing);
                                })
                        .orElseGet(
                                () ->
                                        users.save(
                                                UserAccount.createFromGoogle(
                                                        SUBJECT_PREFIX + normalisedEmail,
                                                        normalisedEmail,
                                                        name,
                                                        clock.instant())));
        log.warn("Local development sign-in issued a session without Google");
        return identityService.startSession(user);
    }
}
