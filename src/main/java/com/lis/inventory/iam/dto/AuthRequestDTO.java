package com.lis.inventory.iam.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthRequestDTO {

    @NotBlank(message = "El ID token de Auth0 es obligatorio")
    private String idToken;
}
