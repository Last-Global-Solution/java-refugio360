package org.fiap.gs_final.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.Parameter;
import org.fiap.gs_final.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService service;

    public AuthController(AuthService service) { this.service = service; }

    /** Login local mantido para desenvolvimento e uso do Swagger. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthService.AuthSession registrarLocal(@Valid @RequestBody RegisterRequest request) {
        return service.registrarLocal(request.email(), request.senha());
    }

    @PostMapping("/login")
    public AuthService.AuthSession loginLocal(@Valid @RequestBody LoginRequest request) {
        return service.loginLocal(request.email(), request.senha());
    }

    /** O front chama após autenticar no Firebase. O ID token vai no header Authorization. */
    @PostMapping("/firebase")
    public AuthService.UserProfile registrarFirebase(@Parameter(hidden = true) @AuthenticationPrincipal Jwt firebaseToken) {
        return service.registrarFirebase(firebaseToken);
    }

    /** Retorna o perfil local já vinculado ao usuário autenticado do Firebase. */
    @GetMapping("/me")
    public AuthService.UserProfile sessao(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        if ("urn:refugio360:api".equals(jwt.getClaimAsString("iss"))) {
            return service.sessaoLocal(jwt);
        }
        return service.sessaoFirebase(jwt);
    }

    public record RegisterRequest(@NotBlank @Email String email,
                                 @NotBlank @Size(min = 8, max = 72) String senha) {}
    public record LoginRequest(@NotBlank @Email String email, @NotBlank String senha) {}
}
