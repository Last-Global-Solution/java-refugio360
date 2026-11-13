package org.fiap.gs_final.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "tb_ponto_risco")
public class PontoRisco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O título da área de risco é obrigatório")
    private String titulo;

    @NotBlank(message = "O nível de perigo é obrigatório (ex: ALTO, MODERADO, BAIXO)")
    private String nivelPerigo;

    @NotNull(message = "A latitude é obrigatória")
    private Double latitude;

    @NotNull(message = "A longitude é obrigatória")
    private Double longitude;

    // Construtores
    public PontoRisco() {}

    public PontoRisco(String titulo, String nivelPerigo, Double latitude, Double longitude) {
        this.titulo = titulo;
        this.nivelPerigo = nivelPerigo;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getNivelPerigo() { return nivelPerigo; }
    public void setNivelPerigo(String nivelPerigo) { this.nivelPerigo = nivelPerigo; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}