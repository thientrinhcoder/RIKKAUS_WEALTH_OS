package com.rikkaus.wealth.identity.google;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Calls Google's token endpoint to redeem an authorization code.
 *
 * <p>Every failure — a non-2xx status, an unreachable host, a 200 with no {@code id_token} — becomes
 * the same checked exception, and the caller renders one 401. Distinguishing them on the wire would
 * tell a caller which half of a stolen code pair was wrong, and would also leak Google's own error text
 * into a response body that is supposed to be safe to show a user.
 */
@Component
class HttpGoogleAuthorizationCodeExchange implements GoogleAuthorizationCodeExchange {

    private static final Logger log =
            LoggerFactory.getLogger(HttpGoogleAuthorizationCodeExchange.class);

    private final RestClient restClient;
    private final GoogleOidcProperties properties;

    HttpGoogleAuthorizationCodeExchange(RestClient.Builder restClientBuilder, GoogleOidcProperties properties) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
    }

    @Override
    public String exchangeForIdentityToken(
            String authorizationCode, String codeVerifier, String redirectUri)
            throws GoogleExchangeFailedException {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", authorizationCode);
        form.add("code_verifier", codeVerifier);
        form.add("redirect_uri", redirectUri);
        form.add("client_id", properties.clientId());
        if (properties.hasClientSecret()) {
            form.add("client_secret", properties.clientSecret());
        }

        Map<String, Object> body;
        try {
            body =
                    restClient
                            .post()
                            .uri(properties.tokenUri())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .body(form)
                            .retrieve()
                            .body(new org.springframework.core.ParameterizedTypeReference<>() {});
        } catch (RestClientException e) {
            // Logged without the form: it carries the authorization code and, when configured, the
            // client secret, neither of which may reach a log.
            log.warn("Google token exchange failed: {}", e.getClass().getSimpleName());
            throw new GoogleExchangeFailedException("Google rejected the authorization code", e);
        }

        if (body == null || !(body.get("id_token") instanceof String idToken) || idToken.isBlank()) {
            throw new GoogleExchangeFailedException("Google returned no identity token");
        }
        return idToken;
    }
}
