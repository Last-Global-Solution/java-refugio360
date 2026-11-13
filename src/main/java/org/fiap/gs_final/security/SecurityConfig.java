package org.fiap.gs_final.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.http.HttpServletRequest;
import org.fiap.gs_final.exception.ApiError;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    public static final String LOCAL_ISSUER = "urn:refugio360:api";
    static final String FIREBASE_ISSUER_PREFIX = "https://securetoken.google.com/";
    static final String FIREBASE_JWKS = "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com";
    static final String PRE_REGISTRATION_ROLE = "ROLE_PRE_CADASTRO";

    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean UserDetailsService userDetailsService(org.fiap.gs_final.repository.UsuarioRepository usuarios) {
        return email -> usuarios.findByEmailIgnoreCase(email)
                .map(u -> User.withUsername(u.getEmail()).password(u.getSenha()).roles(u.getPapel()).build())
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
    }

    @Bean DaoAuthenticationProvider authenticationProvider(UserDetailsService users, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(users);
        provider.setPasswordEncoder(encoder);
        return provider;
    }

    @Bean AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean SecretKey jwtSecretKey(@Value("${app.jwt.secret}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) throw new IllegalStateException("app.jwt.secret deve conter ao menos 32 bytes");
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean JwtEncoder jwtEncoder(SecretKey key) { return new NimbusJwtEncoder(new ImmutableSecret<>(key)); }

    @Bean JwtAuthenticationConverter jwtAuthenticationConverter(
            org.fiap.gs_final.repository.UsuarioRepository usuarios) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String issuer = jwt.getClaimAsString(JwtClaimNames.ISS);
            Optional<org.fiap.gs_final.model.Usuario> usuario = LOCAL_ISSUER.equals(issuer)
                    ? usuarios.findByEmailIgnoreCase(jwt.getSubject())
                    : usuarios.findByFirebaseUid(jwt.getSubject());
            if (usuario.isPresent()) {
                return List.<GrantedAuthority>of(new SimpleGrantedAuthority("ROLE_" + usuario.get().getPapel()));
            }
            if (issuer != null && issuer.startsWith(FIREBASE_ISSUER_PREFIX)) {
                return List.<GrantedAuthority>of(new SimpleGrantedAuthority(PRE_REGISTRATION_ROLE));
            }
            return List.of();
        });
        return converter;
    }

    @Bean AuthenticationManagerResolver<HttpServletRequest> authenticationManagerResolver(
            SecretKey key,
            JwtAuthenticationConverter jwtConverter,
            @Value("${app.firebase.project-id:}") String projectId) {
        Map<String, AuthenticationManager> managers = new HashMap<>();
        JwtDecoder localDecoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        ((NimbusJwtDecoder) localDecoder).setJwtValidator(JwtValidators.createDefaultWithIssuer(LOCAL_ISSUER));
        managers.put(LOCAL_ISSUER, jwtManager(localDecoder, jwtConverter));

        if (projectId != null && !projectId.isBlank()) {
            String issuer = FIREBASE_ISSUER_PREFIX + projectId.trim();
            NimbusJwtDecoder firebaseDecoder = NimbusJwtDecoder.withJwkSetUri(FIREBASE_JWKS)
                    .jwsAlgorithm(SignatureAlgorithm.RS256).build();
            OAuth2TokenValidator<Jwt> audience = new JwtClaimValidator<String>(
                    JwtClaimNames.AUD, projectId.trim()::equals);
            firebaseDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                    JwtValidators.createDefaultWithIssuer(issuer), audience));
            managers.put(issuer, jwtManager(firebaseDecoder, jwtConverter));
        }
        return new JwtIssuerAuthenticationManagerResolver(managers::get);
    }

    private static AuthenticationManager jwtManager(JwtDecoder decoder, JwtAuthenticationConverter converter) {
        JwtAuthenticationProvider provider = new JwtAuthenticationProvider(decoder);
        provider.setJwtAuthenticationConverter(converter);
        return new ProviderManager(provider);
    }

    @Bean CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins}") String allowedOrigins) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        cors.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }

    @Bean SecurityFilterChain filterChain(HttpSecurity http,
                                           DaoAuthenticationProvider provider,
                                           AuthenticationManagerResolver<HttpServletRequest> resolver,
                                           JwtAuthenticationConverter jwtConverter,
                                           ObjectMapper mapper) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(provider)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/register", "/api/auth/login", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/h2-console/**").permitAll()
                        .requestMatchers("/api/auth/firebase", "/api/auth/me").hasAnyRole("PRE_CADASTRO", "CIDADAO", "GESTOR")
                        .requestMatchers(HttpMethod.GET, "/api/locais-seguros/**", "/api/pontos-risco/**").permitAll()
                        .requestMatchers("/api/ai/**").hasAnyRole("CIDADAO", "GESTOR")
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setStatus(401);
                            response.setContentType("application/json");
                            mapper.writeValue(response.getOutputStream(), new ApiError(Instant.now(), 401, "Unauthorized", "Autenticação necessária ou token inválido", request.getRequestURI(), Map.of()));
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            response.setStatus(403);
                            response.setContentType("application/json");
                            mapper.writeValue(response.getOutputStream(), new ApiError(Instant.now(), 403, "Forbidden", "Seu perfil não permite esta operação", request.getRequestURI(), Map.of()));
                        }))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .oauth2ResourceServer(oauth -> oauth.authenticationManagerResolver(resolver))
                .build();
    }
}
