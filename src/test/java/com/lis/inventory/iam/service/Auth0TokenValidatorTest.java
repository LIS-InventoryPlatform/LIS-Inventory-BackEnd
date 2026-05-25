package com.lis.inventory.iam.service;

import com.lis.inventory.shared.exception.AuthServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios para Auth0TokenValidator.
 * US: Manejo controlado de errores de comunicación con LIS-Authentication-Service.
 */
@ExtendWith(MockitoExtension.class)
class Auth0TokenValidatorTest {

    private Auth0TokenValidator validator;
    private NimbusJwtDecoder     mockDecoder;

    private static final String FAKE_TOKEN = "header.payload.signature";

    @BeforeEach
    void setUp() {
        // El constructor solo configura objetos locales; no realiza llamadas de red.
        validator = new Auth0TokenValidator(
                "https://test.auth0.local/.well-known/jwks.json",
                "https://test.auth0.local/",
                "https://test-api.local"
        );
        mockDecoder = mock(NimbusJwtDecoder.class);
        ReflectionTestUtils.setField(validator, "decoder", mockDecoder);
    }

    // --- Escenario: servicio Auth0 no disponible (conexión rechazada) → 503 ---

    @Test
    void validateAndDecode_debeLanzarAuthServiceUnavailable_cuandoAuth0NoDisponibleConectException() {
        JwtException jwtEx = new JwtException("JWKS unreachable",
                new ConnectException("Connection refused: test.auth0.local"));
        when(mockDecoder.decode(FAKE_TOKEN)).thenThrow(jwtEx);

        assertThatThrownBy(() -> validator.validateAndDecode(FAKE_TOKEN))
                .isInstanceOf(AuthServiceUnavailableException.class)
                .hasMessage("El servicio de autenticación no está disponible. Intente más tarde.");
    }

    // --- Escenario: timeout al invocar el servicio de autenticación → 503 ---

    @Test
    void validateAndDecode_debeLanzarAuthServiceUnavailable_cuandoOcurreTimeout() {
        JwtException jwtEx = new JwtException("Read timeout",
                new SocketTimeoutException("Read timed out"));
        when(mockDecoder.decode(FAKE_TOKEN)).thenThrow(jwtEx);

        assertThatThrownBy(() -> validator.validateAndDecode(FAKE_TOKEN))
                .isInstanceOf(AuthServiceUnavailableException.class)
                .hasMessage("El servicio de autenticación no está disponible. Intente más tarde.");
    }

    // --- Escenario: IOException genérica en la cadena de causas → 503 ---

    @Test
    void validateAndDecode_debeLanzarAuthServiceUnavailable_cuandoIOExceptionEnCadenaDeCausas() {
        JwtException jwtEx = new JwtException("Remote error",
                new RuntimeException("Wrapper", new java.io.IOException("Network failure")));
        when(mockDecoder.decode(FAKE_TOKEN)).thenThrow(jwtEx);

        assertThatThrownBy(() -> validator.validateAndDecode(FAKE_TOKEN))
                .isInstanceOf(AuthServiceUnavailableException.class);
    }

    // --- Escenario: excepción inesperada (no JwtException) → 503 ---

    @Test
    void validateAndDecode_debeLanzarAuthServiceUnavailable_cuandoExcepcionInesperada() {
        when(mockDecoder.decode(FAKE_TOKEN)).thenThrow(new RuntimeException("Unexpected failure"));

        assertThatThrownBy(() -> validator.validateAndDecode(FAKE_TOKEN))
                .isInstanceOf(AuthServiceUnavailableException.class)
                .hasMessage("El servicio de autenticación no está disponible. Intente más tarde.");
    }

    // --- Escenario: token inválido / expirado (sin error de red) → 400, mensaje seguro ---

    @Test
    void validateAndDecode_debeLanzarIllegalArgument_cuandoTokenInvalido() {
        when(mockDecoder.decode(FAKE_TOKEN)).thenThrow(new JwtException("Signature verification failed"));

        assertThatThrownBy(() -> validator.validateAndDecode(FAKE_TOKEN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Token de autenticación inválido o expirado.");
    }

    @Test
    void validateAndDecode_mensajeDeErrorNoDebeExponerDetallesTecnicos_cuandoTokenInvalido() {
        when(mockDecoder.decode(FAKE_TOKEN))
                .thenThrow(new JwtException("JWT expired at 2020-01-01T00:00:00Z"));

        assertThatThrownBy(() -> validator.validateAndDecode(FAKE_TOKEN))
                .isInstanceOf(IllegalArgumentException.class)
                .satisfies(ex -> assertThat(ex.getMessage())
                        .doesNotContain("JWT")
                        .doesNotContain("2020")
                        .doesNotContain("expired at"));
    }

    // --- Escenario: decode exitoso → retorna Jwt ---

    @Test
    void validateAndDecode_debeRetornarJwt_cuandoTokenEsValido() {
        Jwt jwtMock = Jwt.withTokenValue(FAKE_TOKEN)
                .header("alg", "RS256")
                .claim("sub", "google-oauth2|12345")
                .claim("email", "usuario@udea.edu.co")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(mockDecoder.decode(FAKE_TOKEN)).thenReturn(jwtMock);

        Jwt result = validator.validateAndDecode(FAKE_TOKEN);

        assertThat(result).isNotNull();
        assertThat(result.getClaimAsString("email")).isEqualTo("usuario@udea.edu.co");
    }

    // --- Tests de extractAndValidateEmail ---

    @Test
    void extractAndValidateEmail_debeRetornarEmail_cuandoDominioEsUdea() {
        Jwt jwt = buildJwt("usuario@udea.edu.co");

        String email = validator.extractAndValidateEmail(jwt);

        assertThat(email).isEqualTo("usuario@udea.edu.co");
    }

    @Test
    void extractAndValidateEmail_debeLanzarIllegalArgument_cuandoDominioNoEsUdea() {
        Jwt jwt = buildJwt("usuario@gmail.com");

        assertThatThrownBy(() -> validator.extractAndValidateEmail(jwt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@udea.edu.co");
    }

    @Test
    void extractAndValidateEmail_debeLanzarIllegalArgument_cuandoEmailEsNull() {
        Jwt jwt = Jwt.withTokenValue(FAKE_TOKEN)
                .header("alg", "RS256")
                .claim("sub", "sub123")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        assertThatThrownBy(() -> validator.extractAndValidateEmail(jwt))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- Helper ---

    private Jwt buildJwt(String email) {
        return Jwt.withTokenValue(FAKE_TOKEN)
                .header("alg", "RS256")
                .claims(c -> c.putAll(Map.of(
                        "sub",   "sub123",
                        "email", email,
                        "name",  "Usuario Test"
                )))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }
}
