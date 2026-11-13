package org.fiap.gs_final.repository;

import org.fiap.gs_final.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmailIgnoreCase(String email);
    Optional<Usuario> findByFirebaseUid(String firebaseUid);
    boolean existsByEmailIgnoreCase(String email);
}
