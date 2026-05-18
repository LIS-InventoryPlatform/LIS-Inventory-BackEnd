package com.lis.inventory.iam.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {

    private String token;
    private String tokenType;
    private long   expiresIn;   // segundos
    private String email;
    private String fullName;
    private String role;
    private List<String> permissions;
}
