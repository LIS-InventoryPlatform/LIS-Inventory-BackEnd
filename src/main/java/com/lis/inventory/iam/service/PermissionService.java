package com.lis.inventory.iam.service;

import com.lis.inventory.iam.dto.PermissionRequestDTO;
import com.lis.inventory.iam.dto.PermissionResponseDTO;
import com.lis.inventory.iam.entity.Permission;
import com.lis.inventory.iam.repository.PermissionRepository;
import com.lis.inventory.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PermissionService {

    private final PermissionRepository permissionRepository;

    public List<PermissionResponseDTO> findAll() {
        return permissionRepository.findAll().stream().map(this::toDTO).toList();
    }

    public PermissionResponseDTO findById(Long id) {
        return toDTO(getOrThrow(id));
    }

    @Transactional
    public PermissionResponseDTO create(PermissionRequestDTO dto) {
        if (permissionRepository.findByName(dto.getName()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un permiso con el nombre: " + dto.getName());
        }
        Permission permission = Permission.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .build();
        return toDTO(permissionRepository.save(permission));
    }

    @Transactional
    public PermissionResponseDTO update(Long id, PermissionRequestDTO dto) {
        Permission permission = getOrThrow(id);
        if (!permission.getName().equals(dto.getName())
                && permissionRepository.findByName(dto.getName()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un permiso con el nombre: " + dto.getName());
        }
        permission.setName(dto.getName());
        permission.setDescription(dto.getDescription());
        return toDTO(permissionRepository.save(permission));
    }

    @Transactional
    public void delete(Long id) {
        if (!permissionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Permiso", id);
        }
        permissionRepository.deleteById(id);
    }

    // ---------------------------------------------------------------

    private Permission getOrThrow(Long id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permiso", id));
    }

    private PermissionResponseDTO toDTO(Permission p) {
        return PermissionResponseDTO.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
