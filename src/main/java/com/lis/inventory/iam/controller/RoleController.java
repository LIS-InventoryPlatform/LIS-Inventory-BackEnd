package com.lis.inventory.iam.controller;

import com.lis.inventory.iam.dto.RoleRequestDTO;
import com.lis.inventory.iam.dto.RoleResponseDTO;
import com.lis.inventory.iam.service.RoleService;
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
@RequestMapping("/iam/roles")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('roles:manage')")
@Tag(name = "IAM – Roles", description = "Gestión de roles")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @Operation(summary = "Listar todos los roles")
    public ResponseEntity<List<RoleResponseDTO>> findAll() {
        return ResponseEntity.ok(roleService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener rol por ID")
    public ResponseEntity<RoleResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(roleService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Crear un nuevo rol")
    @ApiResponse(responseCode = "201", description = "Rol creado")
    public ResponseEntity<RoleResponseDTO> create(@Valid @RequestBody RoleRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.create(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar nombre o descripción de un rol")
    public ResponseEntity<RoleResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody RoleRequestDTO dto) {
        return ResponseEntity.ok(roleService.update(id, dto));
    }

    @PutMapping("/{id}/permissions")
    @Operation(summary = "Reemplazar el conjunto de permisos de un rol")
    public ResponseEntity<RoleResponseDTO> setPermissions(
            @PathVariable Long id,
            @RequestBody List<Long> permissionIds) {
        return ResponseEntity.ok(roleService.setPermissions(id, permissionIds));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un rol")
    @ApiResponse(responseCode = "204", description = "Rol eliminado")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
