package com.lis.inventory.iam.controller;

import com.lis.inventory.iam.dto.PermissionRequestDTO;
import com.lis.inventory.iam.dto.PermissionResponseDTO;
import com.lis.inventory.iam.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/iam/permissions")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('roles:manage')")
@Tag(name = "IAM – Permisos", description = "Gestión de permisos")
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    @Operation(summary = "Listar todos los permisos")
    public ResponseEntity<List<PermissionResponseDTO>> findAll() {
        return ResponseEntity.ok(permissionService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener permiso por ID")
    public ResponseEntity<PermissionResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(permissionService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Crear un nuevo permiso")
    @ApiResponse(responseCode = "201", description = "Permiso creado")
    public ResponseEntity<PermissionResponseDTO> create(@Valid @RequestBody PermissionRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(permissionService.create(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un permiso")
    public ResponseEntity<PermissionResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody PermissionRequestDTO dto) {
        return ResponseEntity.ok(permissionService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un permiso")
    @ApiResponse(responseCode = "204", description = "Permiso eliminado")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        permissionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
