package com.lis.inventory.iam.service;

import com.lis.inventory.iam.dto.AuthResponseDTO;
import com.lis.inventory.iam.entity.AppUser;
import com.lis.inventory.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final Auth0TokenValidator auth0TokenValidator;
    private final UserRepository      userRepository;
    private final JwtService          jwtService;

    /**
     * Flujo de intercambio de token:
     * 1. Valida el ID token de Auth0 (firma RS256 + expiración).
     * 2. Verifica que el email sea @udea.edu.co.
     * 3. Crea el usuario en DB si es su primer acceso.
     * 4. Rechaza si el usuario está inactivo.
     * 5. Emite un JWT propio con rol + permisos del usuario.
     */
    @Transactional
    public AuthResponseDTO exchangeToken(String idToken) {

        // 1 & 2 – validar token Auth0 y email
        Jwt  jwt   = auth0TokenValidator.validateAndDecode(idToken);
        String email = auth0TokenValidator.extractAndValidateEmail(jwt);
        String name  = auth0TokenValidator.extractName(jwt);
        String sub   = auth0TokenValidator.extractSub(jwt);

        // 3 – buscar o crear usuario
        AppUser user = userRepository.findByEmail(email)
                .orElseGet(() -> createUser(email, name, sub));

        // 4 – verificar que esté activo
        if (Boolean.FALSE.equals(user.getActive())) {
            throw new IllegalArgumentException("Usuario inactivo. Contacte al administrador.");
        }

        // Actualizar auth0Sub si todavía no está guardado
        if (user.getAuth0Sub() == null) {
            user.setAuth0Sub(sub);
            userRepository.save(user);
        }

        // 5 – emitir JWT propio
        String appToken = jwtService.generateToken(user);

        List<String> permissions = (user.getRole() != null)
                ? user.getRole().getPermissions().stream()
                        .map(p -> p.getName())
                        .sorted()
                        .toList()
                : List.of();

        return AuthResponseDTO.builder()
                .token(appToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationSeconds())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole() != null ? user.getRole().getName() : null)
                .permissions(permissions)
                .build();
    }

    private AppUser createUser(String email, String name, String sub) {
        AppUser newUser = AppUser.builder()
                .email(email)
                .fullName(name)
                .auth0Sub(sub)
                .active(true)
                .build();
        return userRepository.save(newUser);
    }
}
