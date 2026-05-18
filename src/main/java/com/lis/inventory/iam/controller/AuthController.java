package com.lis.inventory.iam.controller;

import com.lis.inventory.iam.dto.AuthRequestDTO;
import com.lis.inventory.iam.dto.AuthResponseDTO;
import com.lis.inventory.iam.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Intercambio de token Auth0 por JWT de la aplicación")
public class AuthController {

    private final AuthService authService;

    /**
     * Recibe el ID token emitido por Auth0 (tras login con Google Workspace)
     * y devuelve un JWT propio firmado con HMAC-SHA256 que incluye el rol y
     * todos los permisos del usuario.
     *
     * <p>El frontend debe enviar este JWT en el header {@code Authorization: Bearer <token>}
     * en todas las llamadas posteriores.</p>
     */
    @PostMapping("/token")
    @Operation(summary = "Intercambiar ID token de Auth0 por JWT de la aplicación")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "JWT emitido exitosamente"),
            @ApiResponse(responseCode = "400", description = "Token inválido o email no permitido"),
            @ApiResponse(responseCode = "403", description = "Usuario inactivo")
    })
    public ResponseEntity<AuthResponseDTO> exchangeToken(
            @Valid @RequestBody AuthRequestDTO request) {
        return ResponseEntity.ok(authService.exchangeToken(request.getIdToken()));
    }
}
