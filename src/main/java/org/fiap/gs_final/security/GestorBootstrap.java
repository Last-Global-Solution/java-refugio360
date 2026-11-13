package org.fiap.gs_final.security;

import org.fiap.gs_final.model.Usuario;
import org.fiap.gs_final.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class GestorBootstrap {
    @Bean CommandLineRunner criarGestorInicial(UsuarioRepository repository, PasswordEncoder encoder,
            @Value("${app.bootstrap-gestor.email:gestor@refugio360.local}") String email,
            @Value("${app.bootstrap-gestor.password:gestor12345}") String password) {
        return args -> {
            if (!repository.existsByEmailIgnoreCase(email)) repository.save(new Usuario(email.toLowerCase(), encoder.encode(password), "GESTOR"));
        };
    }
}
