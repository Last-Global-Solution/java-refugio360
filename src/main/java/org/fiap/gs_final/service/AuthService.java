package org.fiap.gs_final.service;

import org.fiap.gs_final.model.Usuario;
import org.fiap.gs_final.repository.UsuarioRepository;
import org.fiap.gs_final.security.SecurityConfig;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtEncoder jwtEncoder) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;
    }

    @Transactional
    public AuthSession registrarLocal(String email, String senha) {
        String normalizado = email.trim().toLowerCase();
        if (usuarios.existsByEmailIgnoreCase(normalizado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado");
        }
        return emitir(usuarios.save(new Usuario(normalizado, passwordEncoder.encode(senha), "CIDADAO")));
    }

    @Transactional(readOnly = true)
    public AuthSession loginLocal(String email, String senha) {
        String normalizado = email.trim().toLowerCase();
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(normalizado, senha));
        Usuario usuario = usuarios.findByEmailIgnoreCase(normalizado)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas"));
        return emitir(usuario);
    }

    /** Vincula o UID do Firebase a um cadastro local, ou cria um cidadão na primeira entrada. */
    @Transactional
    public UserProfile registrarFirebase(Jwt firebaseToken) {
        String uid = firebaseToken.getSubject();
        String email = firebaseToken.getClaimAsString("email");
        if (uid == null || uid.isBlank() || email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "O ID token precisa conter UID e e-mail");
        }
        email = email.trim().toLowerCase();
        Usuario usuario = usuarios.findByFirebaseUid(uid).orElse(null);
        if (usuario == null) {
            usuario = usuarios.findByEmailIgnoreCase(email).orElse(null);
            if (usuario != null && usuario.getFirebaseUid() != null && !usuario.getFirebaseUid().equals(uid)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Este e-mail já está vinculado a outra conta Firebase");
            }
            if (usuario != null && "GESTOR".equals(usuario.getPapel())
                    && !Boolean.TRUE.equals(firebaseToken.getClaim("email_verified"))) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Verifique o e-mail no Firebase antes de vincular a conta de gestor");
            }
            if (usuario == null) {
                // A senha aleatória existe somente para satisfazer o cadastro local; autenticação real é do Firebase.
                usuario = new Usuario(email, passwordEncoder.encode(UUID.randomUUID().toString()), "CIDADAO");
            }
            usuario.setFirebaseUid(uid);
            usuario = usuarios.save(usuario);
        }
        return perfil(usuario);
    }

    @Transactional(readOnly = true)
    public UserProfile sessaoFirebase(Jwt firebaseToken) {
        String uid = firebaseToken.getSubject();
        return usuarios.findByFirebaseUid(uid)
                .map(this::perfil)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Conta ainda não vinculada; chame POST /api/auth/firebase"));
    }

    @Transactional(readOnly = true)
    public UserProfile sessaoLocal(Jwt jwt) {
        return usuarios.findByEmailIgnoreCase(jwt.getSubject())
                .map(this::perfil)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão local inválida"));
    }

    private UserProfile perfil(Usuario usuario) {
        return new UserProfile(usuario.getEmail(), usuario.getPapel(), usuario.getFirebaseUid());
    }

    private AuthSession emitir(Usuario usuario) {
        Instant now = Instant.now();
        Instant expires = now.plusSeconds(3600);
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(SecurityConfig.LOCAL_ISSUER)
                .issuedAt(now).expiresAt(expires).subject(usuario.getEmail())
                .claim("roles", List.of(usuario.getPapel())).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new AuthSession("Bearer", token, expires, usuario.getEmail(), usuario.getPapel(), usuario.getFirebaseUid());
    }

    public record AuthSession(String tokenType, String accessToken, Instant expiresAt,
                              String email, String papel, String firebaseUid) {}
    public record UserProfile(String email, String papel, String firebaseUid) {}
}
