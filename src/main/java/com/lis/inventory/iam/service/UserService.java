package com.lis.inventory.iam.service;

import com.lis.inventory.iam.dto.AssignRoleDTO;
import com.lis.inventory.iam.dto.SessionInfoDTO;
import com.lis.inventory.iam.dto.UserResponseDTO;
import com.lis.inventory.iam.entity.AppUser;
import com.lis.inventory.iam.entity.Role;
import com.lis.inventory.iam.repository.RoleRepository;
import com.lis.inventory.iam.repository.UserRepository;
import com.lis.inventory.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public List<UserResponseDTO> findAll() {
        return userRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    public UserResponseDTO findById(Long id) {
        return toDTO(getOrThrow(id));
    }

    @Transactional
    public UserResponseDTO assignRole(Long userId, AssignRoleDTO dto) {
        AppUser user = getOrThrow(userId);
        Role role = roleRepository.findById(dto.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Rol", dto.getRoleId()));
        user.setRole(role);
        return toDTO(userRepository.save(user));
    }

    @Transactional
    public UserResponseDTO toggleActive(Long userId) {
        AppUser user = getOrThrow(userId);
        user.setActive(!user.getActive());
        return toDTO(userRepository.save(user));
    }

    @Transactional
    public void delete(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Usuario", userId);
        }
        userRepository.deleteById(userId);
    }

    // ---------------------------------------------------------------

    /**
     * Retorna la información de sesión del usuario autenticado.
     * Verifica que el usuario exista en la plataforma y esté activo.
     *
     * @param email email extraído del claim {@code sub} del JWT propio.
     * @throws ResourceNotFoundException si el usuario no existe.
     * @throws IllegalArgumentException  si el usuario está inactivo.
     */
    public SessionInfoDTO getMySession(String email) {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (Boolean.FALSE.equals(user.getActive())) {
            throw new IllegalArgumentException("Usuario inactivo. Contacte al administrador.");
        }

        List<String> permissions = (user.getRole() != null)
                ? user.getRole().getPermissions().stream()
                        .map(p -> p.getName()).sorted().toList()
                : List.of();

        return SessionInfoDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole() != null ? user.getRole().getName() : null)
                .permissions(permissions)
                .build();
    }

    private AppUser getOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
    }

    private UserResponseDTO toDTO(AppUser u) {
        List<String> permissions = (u.getRole() != null)
                ? u.getRole().getPermissions().stream()
                        .map(p -> p.getName()).sorted().toList()
                : List.of();

        return UserResponseDTO.builder()
                .id(u.getId())
                .email(u.getEmail())
                .fullName(u.getFullName())
                .active(u.getActive())
                .role(u.getRole() != null ? u.getRole().getName() : null)
                .permissions(permissions)
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .build();
    }
}
