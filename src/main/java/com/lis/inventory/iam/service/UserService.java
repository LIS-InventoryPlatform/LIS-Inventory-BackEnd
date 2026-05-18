package com.lis.inventory.iam.service;

import com.lis.inventory.iam.dto.AssignRoleDTO;
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
