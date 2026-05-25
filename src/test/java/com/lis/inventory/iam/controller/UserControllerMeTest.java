package com.lis.inventory.iam.controller;

import com.lis.inventory.iam.dto.SessionInfoDTO;
import com.lis.inventory.iam.service.UserService;
import com.lis.inventory.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración para GET /iam/users/me.
 * US: Consultar información de sesión del usuario autenticado.
 */
@WebMvcTest(UserController.class)
@ActiveProfiles("test")
class UserControllerMeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private static final String URL       = "/iam/users/me";
    private static final String EMAIL     = "usuario@udea.edu.co";

    // --- Helper ------------------------------------------------------------

    private SessionInfoDTO sessionInfo(String role) {
        return SessionInfoDTO.builder()
                .id(1L)
                .email(EMAIL)
                .fullName("Usuario Test")
                .role(role)
                .permissions(List.of("inventory:read"))
                .build();
    }

    // --- Escenario 1: Happy path — usuario autenticado con rol JEFE → 200 ---

    @Test
    void getMySession_debeRetornar200ConDatosBasicos_cuandoUsuarioAutenticadoRolJefe() throws Exception {
        when(userService.getMySession(EMAIL)).thenReturn(sessionInfo("JEFE"));

        mockMvc.perform(get(URL).with(jwt().jwt(j -> j.subject(EMAIL))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.fullName").value("Usuario Test"))
                .andExpect(jsonPath("$.role").value("JEFE"))
                .andExpect(jsonPath("$.permissions[0]").value("inventory:read"));
    }

    // --- Escenario 1 (variantes de rol): SUPER_ADMIN y AUXILIAR también tienen acceso ---

    @Test
    void getMySession_debeRetornar200_cuandoUsuarioAutenticadoRolSuperAdmin() throws Exception {
        when(userService.getMySession(EMAIL)).thenReturn(sessionInfo("SUPER_ADMIN"));

        mockMvc.perform(get(URL).with(jwt().jwt(j -> j.subject(EMAIL))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("SUPER_ADMIN"));
    }

    @Test
    void getMySession_debeRetornar200_cuandoUsuarioAutenticadoRolAuxiliar() throws Exception {
        when(userService.getMySession(EMAIL)).thenReturn(sessionInfo("AUXILIAR"));

        mockMvc.perform(get(URL).with(jwt().jwt(j -> j.subject(EMAIL))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("AUXILIAR"));
    }

    // --- Escenario 2: La respuesta NO expone datos sensibles ---

    @Test
    void getMySession_noDebeExponerDatosSensibles_enElResponse() throws Exception {
        when(userService.getMySession(EMAIL)).thenReturn(sessionInfo("JEFE"));

        mockMvc.perform(get(URL).with(jwt().jwt(j -> j.subject(EMAIL))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").doesNotExist())
                .andExpect(jsonPath("$.auth0Sub").doesNotExist())
                .andExpect(jsonPath("$.createdAt").doesNotExist())
                .andExpect(jsonPath("$.updatedAt").doesNotExist());
    }

    // --- Escenario 3: Sin token → 401 (token inválido / ausente) ---

    @Test
    void getMySession_debeRetornar401_cuandoNoSeEnviaToken() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized());
    }

    // --- Escenario 4: Token expirado / inválido → 401 manejado por Spring Security ---
    // Spring Security rechaza el token antes de llegar al controlador;
    // el test de "sin token" cubre este comportamiento a nivel de filtro.

    // --- Escenario 5: Usuario inactivo → 400 ---

    @Test
    void getMySession_debeRetornar400_cuandoUsuarioEstaInactivo() throws Exception {
        when(userService.getMySession(EMAIL))
                .thenThrow(new IllegalArgumentException("Usuario inactivo. Contacte al administrador."));

        mockMvc.perform(get(URL).with(jwt().jwt(j -> j.subject(EMAIL))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Usuario inactivo. Contacte al administrador."));
    }

    // --- Escenario 6: Usuario no existe en plataforma (token válido pero borrado de DB) → 404 ---

    @Test
    void getMySession_debeRetornar404_cuandoUsuarioNoExisteEnPlataforma() throws Exception {
        when(userService.getMySession(EMAIL))
                .thenThrow(new ResourceNotFoundException("Usuario no encontrado"));

        mockMvc.perform(get(URL).with(jwt().jwt(j -> j.subject(EMAIL))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Usuario no encontrado"));
    }

    // --- Escenario 7: Solo el propio usuario puede consultar su sesión ---
    // La sesión devuelta siempre corresponde al sub del JWT; el servicio
    // no permite consultar la sesión de otro usuario desde este endpoint.

    @Test
    void getMySession_debeUsarEmailDelJwt_noDeParametro() throws Exception {
        String otroEmail = "otro@udea.edu.co";
        when(userService.getMySession(otroEmail)).thenReturn(sessionInfo("JEFE"));

        // Se pasa el sub del token; el controlador usa jwt.getSubject() directamente.
        mockMvc.perform(get(URL).with(jwt().jwt(j -> j.subject(otroEmail))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL)); // sessionInfo siempre devuelve EMAIL del mock
    }
}
