package com.lis.inventory.iam.service;

import com.lis.inventory.iam.dto.RegisterUserRequestDTO;
import com.lis.inventory.iam.dto.UserResponseDTO;
import com.lis.inventory.iam.entity.AppUser;
import com.lis.inventory.iam.entity.Permission;
import com.lis.inventory.iam.entity.Role;
import com.lis.inventory.iam.repository.RoleRepository;
import com.lis.inventory.iam.repository.UserRepository;
import com.lis.inventory.shared.exception.ResourceAlreadyExistsException;
import com.lis.inventory.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios para UserService.registerUser().
 * US: Registrar un usuario en el sistema local.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceRegisterUserTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private UserService userService;

    private static final String EMAIL = "nuevo@udea.edu.co";

    // --- Helpers -----------------------------------------------------------

    private RegisterUserRequestDTO request(Long roleId) {
        return RegisterUserRequestDTO.builder()
                .email(EMAIL)
                .fullName("Usuario Nuevo")
                .roleId(roleId)
                .build();
    }

    private Role role(String name) {
        return Role.builder()
                .id(1L)
                .name(name)
                .permissions(Set.of(
                        Permission.builder().name("users:read").build(),
                        Permission.builder().name("users:write").build()))
                .build();
    }

    // --- Escenario 1: Happy path — usuario institucional con rol inicial ---

    @Test
    void registerUser_debeCrearUsuarioActivo_cuandoDatosValidosConRol() {
        Role role = role("JEFE");
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> {
            AppUser user = invocation.getArgument(0);
            user.setId(10L);
            return user;
        });

        UserResponseDTO result = userService.registerUser(request(1L));

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getEmail()).isEqualTo(EMAIL);
        assertThat(result.getFullName()).isEqualTo("Usuario Nuevo");
        assertThat(result.getActive()).isTrue();
        assertThat(result.getRole()).isEqualTo("JEFE");
        assertThat(result.getPermissions()).containsExactlyInAnyOrder("users:read", "users:write");

        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getAuth0Sub()).isNull();
    }

    @Test
    void registerUser_debeCrearUsuarioActivoSinRol_cuandoRoleIdEsNull() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> {
            AppUser user = invocation.getArgument(0);
            user.setId(11L);
            return user;
        });

        UserResponseDTO result = userService.registerUser(request(null));

        assertThat(result.getRole()).isNull();
        assertThat(result.getPermissions()).isEmpty();
        assertThat(result.getActive()).isTrue();
        verify(roleRepository, never()).findById(any());
    }

    // --- Escenario 2: Error o validación — correo inválido o duplicado ---

    @Test
    void registerUser_debeLanzarIllegalArgumentException_cuandoCorreoNoEsInstitucional() {
        RegisterUserRequestDTO dto = RegisterUserRequestDTO.builder()
                .email("nuevo@gmail.com")
                .fullName("Usuario Nuevo")
                .roleId(1L)
                .build();

        assertThatThrownBy(() -> userService.registerUser(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El correo debe pertenecer al dominio @udea.edu.co");

        verify(userRepository, never()).existsByEmail(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_debeLanzarResourceAlreadyExistsException_cuandoCorreoYaExiste() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUser(request(1L)))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessage("Ya existe un usuario registrado con el correo: " + EMAIL);

        verify(roleRepository, never()).findById(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_debeLanzarResourceNotFoundException_cuandoRolNoExiste() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.registerUser(request(99L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Rol con id 99 no encontrado");

        verify(userRepository, never()).save(any());
    }
}
