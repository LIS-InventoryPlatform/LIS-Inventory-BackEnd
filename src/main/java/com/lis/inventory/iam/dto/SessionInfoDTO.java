package com.lis.inventory.iam.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Información de sesión del usuario autenticado.
 * Expone únicamente los datos necesarios para identificar al usuario en la plataforma.
 * No incluye contraseñas, tokens internos ni información sensible.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionInfoDTO {

    private Long         id;
    private String       email;
    private String       fullName;
    private String       role;
    private List<String> permissions;
}
