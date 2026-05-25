package com.lis.inventory.iam.service;

import com.lis.inventory.shared.exception.AuthServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class Auth0TokenValidator {

    private static final String ALLOWED_DOMAIN = "@udea.edu.co";
    private static final String ORIGIN         = "Auth0TokenValidator.validateAndDecode";

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

    /**
     * Valida y decodifica el ID token emitido por Auth0.
     *
     * <p>Distingue entre:</p>
     * <ul>
     *   <li>Token inválido / expirado → {@link IllegalArgumentException} (400)</li>
     *   <li>Servicio de autenticación no disponible → {@link AuthServiceUnavailableException} (503)</li>
     * </ul>
     * Todos los errores quedan registrados con tipo y origen para trazabilidad.
     * Nunca se registran credenciales ni el contenido del token.
     */
    public Jwt validateAndDecode(String idToken) {
        try {
            return decoder.decode(idToken);
        } catch (JwtException ex) {
            if (isRemoteServiceError(ex)) {
                log.error("[{}] Fallo de comunicación con el servicio de autenticación. "
                        + "tipo={} | causa={}",
                        ORIGIN, ex.getClass().getSimpleName(), rootCauseMessage(ex));
                throw new AuthServiceUnavailableException(
                        "El servicio de autenticación no está disponible. Intente más tarde.", ex);
            }
            log.warn("[{}] Token de autenticación rechazado. tipo={} | detalle={}",
                    ORIGIN, ex.getClass().getSimpleName(), ex.getMessage());
            throw new IllegalArgumentException("Token de autenticación inválido o expirado.");
        } catch (Exception ex) {
            log.error("[{}] Error inesperado al contactar el servicio de autenticación. "
                    + "tipo={} | causa={}",
                    ORIGIN, ex.getClass().getSimpleName(), rootCauseMessage(ex));
            throw new AuthServiceUnavailableException(
                    "El servicio de autenticación no está disponible. Intente más tarde.", ex);
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

    // -----------------------------------------------------------------------

    /**
     * Recorre la cadena de causas buscando errores de red o de acceso remoto
     * al endpoint JWKS de Auth0 (timeout, conexión rechazada, host inalcanzable).
     */
    private boolean isRemoteServiceError(JwtException ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof IOException) return true;
            // Nimbus-JOSE: RemoteKeySourceException al no poder obtener el JWK Set
            if (current.getClass().getName().contains("RemoteKeySourceException")) return true;
            current = current.getCause();
        }
        return false;
    }

    /** Devuelve el mensaje de la causa raíz para el log técnico. */
    private String rootCauseMessage(Throwable ex) {
        Throwable current = ex;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage();
    }
}
