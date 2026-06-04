package com.lis.inventory.iam.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lis.inventory.iam.dto.RegisterUserRequestDTO;
import com.lis.inventory.iam.dto.UserResponseDTO;
import com.lis.inventory.iam.service.UserService;
import com.lis.inventory.shared.exception.ResourceAlreadyExistsException;
import com.lis.inventory.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración para POST /iam/users.
 * US: Registrar un usuario en el sistema local.
 */
@WebMvcTest(UserController.class)
@ActiveProfiles("test")
class UserControllerRegisterUserTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {

        @Bean
        @Order(1)
        SecurityFilterChain testChain(HttpSecurity http) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(a -> a.anyRequest().authenticated())
                    .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {
                    }));
            return http.build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static final String URL = "/iam/users";
    private static final String EMAIL = "nuevo@udea.edu.co";

    // --- Helpers -----------------------------------------------------------

    private RegisterUserRequestDTO request(String email) {
        return RegisterUserRequestDTO.builder()
                .email(email)
                .fullName("Usuario Nuevo")
                .roleId(1L)
                .build();
    }

    private UserResponseDTO response(String role) {
        return UserResponseDTO.builder()
                .id(10L)
                .email(EMAIL)
                .fullName("Usuario Nuevo")
                .active(true)
                .role(role)
                .permissions(List.of("users:read", "users:write"))
                .build();
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor userWithWritePermission() {
        return jwt().authorities(new SimpleGrantedAuthority("users:write"));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor userWithoutWritePermission() {
        return jwt().authorities(new SimpleGrantedAuthority("users:read"));
    }

    // --- Escenario 1: Happy path — usuario registrado por perfiles autorizados ---

    @Test
    void registerUser_debeRetornar201_cuandoUsuarioAutenticadoRolSuperAdminTienePermiso() throws Exception {
        when(userService.registerUser(any(RegisterUserRequestDTO.class))).thenReturn(response("SUPER_ADMIN"));

        mockMvc.perform(post(URL)
                        .with(userWithWritePermission())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(EMAIL))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.fullName").value("Usuario Nuevo"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.role").value("SUPER_ADMIN"))
                .andExpect(jsonPath("$.permissions[0]").value("users:read"));
    }

    @Test
    void registerUser_debeRetornar201_cuandoUsuarioAutenticadoRolJefeTienePermiso() throws Exception {
        when(userService.registerUser(any(RegisterUserRequestDTO.class))).thenReturn(response("JEFE"));

        mockMvc.perform(post(URL)
                        .with(userWithWritePermission())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(EMAIL))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("JEFE"));
    }

    @Test
    void registerUser_debeRetornar201_cuandoUsuarioAutenticadoRolAuxiliarTienePermiso() throws Exception {
        when(userService.registerUser(any(RegisterUserRequestDTO.class))).thenReturn(response("AUXILIAR"));

        mockMvc.perform(post(URL)
                        .with(userWithWritePermission())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(EMAIL))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("AUXILIAR"));
    }

    // --- Escenario 2: Error o validación — datos inválidos o usuario duplicado ---

    @Test
    void registerUser_debeRetornar400_cuandoCorreoTieneFormatoInvalido() throws Exception {
        mockMvc.perform(post(URL)
                        .with(userWithWritePermission())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request("correo-invalido"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());

        verifyNoInteractions(userService);
    }

    @Test
    void registerUser_debeRetornar409_cuandoCorreoYaExiste() throws Exception {
        when(userService.registerUser(any(RegisterUserRequestDTO.class)))
                .thenThrow(new ResourceAlreadyExistsException(
                        "Ya existe un usuario registrado con el correo: " + EMAIL));

        mockMvc.perform(post(URL)
                        .with(userWithWritePermission())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(EMAIL))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(
                        "Ya existe un usuario registrado con el correo: " + EMAIL));
    }

    @Test
    void registerUser_debeRetornar403_cuandoUsuarioNoTienePermisoUsersWrite() throws Exception {
        mockMvc.perform(post(URL)
                        .with(userWithoutWritePermission())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(EMAIL))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    // --- Escenario 3: Edge case — rol inexistente o token ausente ---

    @Test
    void registerUser_debeRetornar404_cuandoRolNoExiste() throws Exception {
        when(userService.registerUser(any(RegisterUserRequestDTO.class)))
                .thenThrow(new ResourceNotFoundException("Rol", 99L));

        mockMvc.perform(post(URL)
                        .with(userWithWritePermission())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(EMAIL))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Rol con id 99 no encontrado"));
    }

    @Test
    void registerUser_debeRetornar401_cuandoNoSeEnviaToken() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(EMAIL))))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }
}
