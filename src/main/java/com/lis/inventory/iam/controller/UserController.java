package com.lis.inventory.iam.controller;

import com.lis.inventory.iam.dto.AssignRoleDTO;
import com.lis.inventory.iam.dto.UserResponseDTO;
import com.lis.inventory.iam.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/iam/users")
@RequiredArgsConstructor
@Tag(name = "IAM – Usuarios", description = "Gestión de usuarios del sistema")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('users:read')")
    @Operation(summary = "Listar todos los usuarios")
    @ApiResponse(responseCode = "200", description = "Lista obtenida exitosamente")
    public ResponseEntity<List<UserResponseDTO>> findAll() {
        return ResponseEntity.ok(userService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('users:read')")
    @Operation(summary = "Obtener usuario por ID")
    public ResponseEntity<UserResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasAuthority('users:write')")
    @Operation(summary = "Asignar rol a un usuario")
    public ResponseEntity<UserResponseDTO> assignRole(
            @PathVariable Long id,
            @Valid @RequestBody AssignRoleDTO dto) {
        return ResponseEntity.ok(userService.assignRole(id, dto));
    }

    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("hasAuthority('users:write')")
    @Operation(summary = "Activar o desactivar usuario")
    public ResponseEntity<UserResponseDTO> toggleActive(@PathVariable Long id) {
        return ResponseEntity.ok(userService.toggleActive(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('users:delete')")
    @Operation(summary = "Eliminar usuario")
    @ApiResponse(responseCode = "204", description = "Usuario eliminado")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
