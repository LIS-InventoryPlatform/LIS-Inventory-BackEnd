package com.lis.inventory.iam.service;

import com.lis.inventory.iam.dto.SessionInfoDTO;
import com.lis.inventory.iam.entity.AppUser;
import com.lis.inventory.iam.entity.Permission;
import com.lis.inventory.iam.entity.Role;
import com.lis.inventory.iam.repository.RoleRepository;
import com.lis.inventory.iam.repository.UserRepository;
import com.lis.inventory.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios para UserService.getMySession().
 * US: Consultar información de sesión del usuario autenticado.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceGetMySessionTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private UserService userService;

    private static final String EMAIL = "usuario@udea.edu.co";

    // --- Helpers -----------------------------------------------------------

    private Permission buildPermission(String name) {
        return Permission.builder().name(name).build();
    }

    private Role buildRole(String name, String... perms) {
        Set<Permission> permissions = new java.util.HashSet<>();
        for (String perm : perms) {
            permissions.add(buildPermission(perm));
        }
        return Role.builder().id(1L).name(name).permissions(permissions).build();
    }

    private AppUser activeUserWith(Role role) {
        return AppUser.builder()
                .id(1L)
                .email(EMAIL)
                .fullName("Usuario Test")
                .active(true)
                .role(role)
                .build();
    }

    // --- Escenario 1: Happy path — usuario activo con rol retorna SessionInfoDTO ---

    @Test
    void getMySession_debeRetornarSessionInfo_cuandoUsuarioActivoConRolJefe() {
        Role role = buildRole("JEFE", "inventory:read", "inventory:write");
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(activeUserWith(role)));

        SessionInfoDTO result = userService.getMySession(EMAIL);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo(EMAIL);
        assertThat(result.getFullName()).isEqualTo("Usuario Test");
        assertThat(result.getRole()).isEqualTo("JEFE");
        assertThat(result.getPermissions()).containsExactlyInAnyOrder("inventory:read", "inventory:write");
        verify(userRepository).findByEmail(EMAIL);
    }

    @Test
    void getMySession_debeRetornarSessionInfo_cuandoUsuarioActivoConRolSuperAdmin() {
        Role role = buildRole("SUPER_ADMIN", "users:read", "users:write", "users:delete");
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(activeUserWith(role)));

        SessionInfoDTO result = userService.getMySession(EMAIL);

        assertThat(result.getRole()).isEqualTo("SUPER_ADMIN");
        assertThat(result.getPermissions()).containsExactlyInAnyOrder("users:read", "users:write", "users:delete");
    }

    @Test
    void getMySession_debeRetornarSessionInfo_cuandoUsuarioActivoConRolAuxiliar() {
        Role role = buildRole("AUXILIAR", "inventory:read");
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(activeUserWith(role)));

        SessionInfoDTO result = userService.getMySession(EMAIL);

        assertThat(result.getRole()).isEqualTo("AUXILIAR");
        assertThat(result.getPermissions()).containsExactly("inventory:read");
    }

    // --- Escenario: usuario activo sin rol asignado ---

    @Test
    void getMySession_debeRetornarPermisosVacios_cuandoUsuarioNoTieneRol() {
        AppUser userSinRol = AppUser.builder()
                .id(2L).email(EMAIL).fullName("Sin Rol").active(true).role(null).build();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(userSinRol));

        SessionInfoDTO result = userService.getMySession(EMAIL);

        assertThat(result.getRole()).isNull();
        assertThat(result.getPermissions()).isEmpty();
    }

    // --- Escenario 2: usuario no existe en plataforma → ResourceNotFoundException ---

    @Test
    void getMySession_debeLanzarResourceNotFoundException_cuandoUsuarioNoExiste() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getMySession(EMAIL))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Usuario no encontrado");
    }

    // --- Escenario 3: usuario inactivo → IllegalArgumentException ---

    @Test
    void getMySession_debeLanzarIllegalArgumentException_cuandoUsuarioEstaInactivo() {
        AppUser userInactivo = AppUser.builder()
                .id(3L).email(EMAIL).fullName("Inactivo").active(false).build();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(userInactivo));

        assertThatThrownBy(() -> userService.getMySession(EMAIL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Usuario inactivo. Contacte al administrador.");
    }
}
