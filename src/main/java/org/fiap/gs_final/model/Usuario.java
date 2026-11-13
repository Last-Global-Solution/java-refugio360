package org.fiap.gs_final.model;

import jakarta.persistence.*;

@Entity
@Table(name = "tb_usuario", uniqueConstraints = {
        @UniqueConstraint(columnNames = "email"),
        @UniqueConstraint(columnNames = "firebase_uid")
})
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true) private String email;
    @Column(nullable = false) private String senha;
    @Column(nullable = false) private String papel;
    @Column(name = "firebase_uid") private String firebaseUid;
    protected Usuario() {}
    public Usuario(String email, String senha, String papel) { this.email = email; this.senha = senha; this.papel = papel; }
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getSenha() { return senha; }
    public String getPapel() { return papel; }
    public String getFirebaseUid() { return firebaseUid; }
    public void setFirebaseUid(String firebaseUid) { this.firebaseUid = firebaseUid; }
}
