package com.lis.inventory.iam.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleResponseDTO {
    private Long id;
    private String name;
    private String description;
    private List<String> permissions;
    private LocalDateTime createdAt;
}
