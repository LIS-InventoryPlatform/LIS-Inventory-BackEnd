package com.lis.inventory.iam.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Usuario de la aplicación autenticado vía Auth0 + Google Workspace (@udea.edu.co).
 * Un usuario tiene exactamente un rol (o ninguno mientras el admin no lo asigne).
 */
@Entity
@Table(name = "app_users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "full_name", length = 255)
    private String fullName;

    /** Subject del token Auth0 ("google-oauth2|xxxxxxxx"). */
    @Column(name = "auth0_sub", unique = true, length = 255)
    private String auth0Sub;

    /** Un usuario → un rol. Sin rol no tiene permisos. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
