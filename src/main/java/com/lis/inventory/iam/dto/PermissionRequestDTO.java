package com.lis.inventory.iam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PermissionRequestDTO {

    @NotBlank(message = "El nombre del permiso es obligatorio")
    @Size(max = 100)
    private String name;

    @Size(max = 500)
    private String description;
}
