package com.lis.inventory.iam.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignRoleDTO {

    @NotNull(message = "El ID del rol es obligatorio")
    private Long roleId;
}
