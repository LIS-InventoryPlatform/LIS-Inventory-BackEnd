package com.lis.inventory.iam.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionResponseDTO {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
}
