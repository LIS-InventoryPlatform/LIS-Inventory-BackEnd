package com.lis.inventory.iam.service;

import com.lis.inventory.iam.dto.PermissionRequestDTO;
import com.lis.inventory.iam.dto.RoleRequestDTO;
import com.lis.inventory.iam.dto.RoleResponseDTO;
import com.lis.inventory.iam.entity.Permission;
import com.lis.inventory.iam.entity.Role;
import com.lis.inventory.iam.repository.PermissionRepository;
import com.lis.inventory.iam.repository.RoleRepository;
import com.lis.inventory.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public List<RoleResponseDTO> findAll() {
        return roleRepository.findAll().stream().map(this::toDTO).toList();
    }

    public RoleResponseDTO findById(Long id) {
        return toDTO(getOrThrow(id));
    }

    @Transactional
    public RoleResponseDTO create(RoleRequestDTO dto) {
        if (roleRepository.findByName(dto.getName().toUpperCase()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un rol con el nombre: " + dto.getName());
        }
        Role role = Role.builder()
                .name(dto.getName().toUpperCase())
                .description(dto.getDescription())
                .build();
        return toDTO(roleRepository.save(role));
    }

    @Transactional
    public RoleResponseDTO update(Long id, RoleRequestDTO dto) {
        Role role = getOrThrow(id);
        String newName = dto.getName().toUpperCase();
        if (!role.getName().equals(newName) && roleRepository.findByName(newName).isPresent()) {
            throw new IllegalArgumentException("Ya existe un rol con el nombre: " + newName);
        }
        role.setName(newName);
        role.setDescription(dto.getDescription());
        return toDTO(roleRepository.save(role));
    }

    @Transactional
    public RoleResponseDTO setPermissions(Long roleId, List<Long> permissionIds) {
        Role role = getOrThrow(roleId);
        Set<Permission> permissions = permissionIds.stream()
                .map(pid -> permissionRepository.findById(pid)
                        .orElseThrow(() -> new ResourceNotFoundException("Permiso", pid)))
                .collect(Collectors.toSet());
        role.getPermissions().clear();
        role.getPermissions().addAll(permissions);
        return toDTO(roleRepository.save(role));
    }

    @Transactional
    public void delete(Long id) {
        if (!roleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Rol", id);
        }
        roleRepository.deleteById(id);
    }

    // ---------------------------------------------------------------

    private Role getOrThrow(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rol", id));
    }

    private RoleResponseDTO toDTO(Role r) {
        return RoleResponseDTO.builder()
                .id(r.getId())
                .name(r.getName())
                .description(r.getDescription())
                .permissions(r.getPermissions().stream()
                        .map(Permission::getName).sorted().toList())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
