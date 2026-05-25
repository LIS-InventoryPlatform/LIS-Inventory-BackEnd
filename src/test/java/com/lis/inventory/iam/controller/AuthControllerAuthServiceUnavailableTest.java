package com.lis.inventory.iam.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lis.inventory.iam.dto.AuthResponseDTO;
import com.lis.inventory.iam.service.AuthService;
import com.lis.inventory.shared.exception.AuthServiceUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración para POST /auth/token.
 * US: Manejo controlado de errores de comunicación con LIS-Authentication-Service.
 */
@WebMvcTest(AuthController.class)
@ActiveProfiles("test")
class AuthControllerAuthServiceUnavailableTest {

    /**
     * Cadena de seguridad con prioridad máxima que permite todas las peticiones.
     * Necesaria porque /auth/token es un endpoint público y @WebMvcTest con
     * oauth2ResourceServer bloquea peticiones sin token incluso en rutas permitAll().
     */
    @TestConfiguration
    static class PermitAllSecurityConfig {
        @Bean
        @Order(1)
        SecurityFilterChain testChain(HttpSecurity http) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(a -> a.anyRequest().permitAll());
            return http.build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    private static final String URL = "/auth/token";

    // --- Helper ------------------------------------------------------------

    private String requestBody(String token) throws Exception {
        return objectMapper.writeValueAsString(Map.of("idToken", token));
    }

    private AuthResponseDTO buildAuthResponse() {
        return AuthResponseDTO.builder()
                .token("app.jwt.token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .email("usuario@udea.edu.co")
                .fullName("Usuario Test")
                .role("JEFE")
                .permissions(List.of("inventory:read"))
                .build();
    }

    // --- Escenario: Auth0 no disponible → 503 con mensaje controlado ---

    @Test
    void exchangeToken_debeRetornar503_cuandoAuth0NoEstaDisponible() throws Exception {
        when(authService.exchangeToken(anyString()))
                .thenThrow(new AuthServiceUnavailableException(
                        "El servicio de autenticación no está disponible. Intente más tarde.",
                        new java.net.ConnectException("Connection refused")));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("fake.token.unavailable")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message")
                        .value("El servicio de autenticación no está disponible. Intente más tarde."))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // --- Escenario: timeout → 503 ---

    @Test
    void exchangeToken_debeRetornar503_cuandoOcurreTimeoutEnServicioDeAutenticacion() throws Exception {
        when(authService.exchangeToken(anyString()))
                .thenThrow(new AuthServiceUnavailableException(
                        "El servicio de autenticación no está disponible. Intente más tarde.",
                        new java.net.SocketTimeoutException("Read timed out")));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("fake.token.timeout")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message")
                        .value("El servicio de autenticación no está disponible. Intente más tarde."));
    }

    // --- Escenario: respuesta inválida del servicio → 503 ---

    @Test
    void exchangeToken_debeRetornar503_cuandoServicioRetornaRespuestaInvalida() throws Exception {
        when(authService.exchangeToken(anyString()))
                .thenThrow(new AuthServiceUnavailableException(
                        "El servicio de autenticación no está disponible. Intente más tarde.",
                        new RuntimeException("Malformed JWKS response")));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("fake.token.invalid.response")))
                .andExpect(status().isServiceUnavailable());
    }

    // --- Escenario: el response 503 NO expone detalles técnicos al usuario ---

    @Test
    void exchangeToken_responseDeError503_noDebeExponerDetallesTecnicos() throws Exception {
        when(authService.exchangeToken(anyString()))
                .thenThrow(new AuthServiceUnavailableException(
                        "El servicio de autenticación no está disponible. Intente más tarde.",
                        new java.net.ConnectException("Connection refused: internal-host:443")));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("fake.token")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(
                        "El servicio de autenticación no está disponible. Intente más tarde."))
                .andExpect(jsonPath("$.cause").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

    // --- Escenario: 503 no genera token de aplicación en el body ---

    @Test
    void exchangeToken_responseDeError503_noDebeContenerTokenDeAplicacion() throws Exception {
        when(authService.exchangeToken(anyString()))
                .thenThrow(new AuthServiceUnavailableException(
                        "El servicio de autenticación no está disponible. Intente más tarde.",
                        new RuntimeException("Auth0 down")));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("fake.token")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.tokenType").doesNotExist());
    }

    // --- Escenario: token inválido / expirado → 400 (error del usuario, no del servicio) ---

    @Test
    void exchangeToken_debeRetornar400_cuandoTokenEsInvalidoOExpirado() throws Exception {
        when(authService.exchangeToken(anyString()))
                .thenThrow(new IllegalArgumentException("Token de autenticación inválido o expirado."));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("invalid.jwt.token")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Token de autenticación inválido o expirado."));
    }

    // --- Escenario: happy path (servicio disponible) → 200 con token ---

    @Test
    void exchangeToken_debeRetornar200ConToken_cuandoServicioDisponibleYTokenValido() throws Exception {
        when(authService.exchangeToken(anyString())).thenReturn(buildAuthResponse());

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("valid.auth0.token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("app.jwt.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.email").value("usuario@udea.edu.co"));
    }

    // --- Escenario: request sin idToken → 400 validación ---

    @Test
    void exchangeToken_debeRetornar400_cuandoIdTokenEsBlank() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("idToken", ""))))
                .andExpect(status().isBadRequest());
    }
}
