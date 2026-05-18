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
public class UserResponseDTO {
    private Long id;
    private String email;
    private String fullName;
    private Boolean active;
    private String role;
    private List<String> permissions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
