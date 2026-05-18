package com.lis.inventory.iam.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class Auth0TokenValidator {

    private static final String ALLOWED_DOMAIN = "@udea.edu.co";

    private final NimbusJwtDecoder decoder;

    public Auth0TokenValidator(
            @Value("${auth0.jwks-uri}") String jwksUri,
            @Value("${auth0.issuer-uri}") String issuerUri,
            @Value("${auth0.audience}") String audience) {

        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(issuerUri);

        OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<List<String>>(
                "aud", aud -> aud != null && aud.contains(audience));

        this.decoder = NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
        this.decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator));
    }

    public Jwt validateAndDecode(String idToken) {
        try {
            return decoder.decode(idToken);
        } catch (JwtException ex) {
            throw new IllegalArgumentException("Token Auth0 inválido o expirado: " + ex.getMessage());
        }
    }

    public String extractAndValidateEmail(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email == null || !email.endsWith(ALLOWED_DOMAIN)) {
            throw new IllegalArgumentException("Solo se permiten correos " + ALLOWED_DOMAIN);
        }
        return email;
    }

    public String extractName(Jwt jwt) {
        return jwt.getClaimAsString("name");
    }

    public String extractSub(Jwt jwt) {
        return jwt.getSubject();
    }
}
