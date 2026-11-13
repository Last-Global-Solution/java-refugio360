package org.fiap.gs_final.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "tb_ocorrencia")
public class Ocorrencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "A descrição do relato é obrigatória")
    private String descricao;

    @NotBlank(message = "O nível de urgência é obrigatório (ex: BAIXA, MEDIA, CRITICA)")
    private String urgencia;

    private String status = "PENDENTE";

    private Double latitude;
    private Double longitude;

    // Construtores
    public Ocorrencia() {}

    public Ocorrencia(String descricao, String urgencia, Double latitude, Double longitude) {
        this.descricao = descricao;
        this.urgencia = urgencia;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getUrgencia() { return urgencia; }
    public void setUrgencia(String urgencia) { this.urgencia = urgencia; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}
