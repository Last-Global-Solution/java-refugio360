package org.fiap.gs_final.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;

public record PontoRiscoRequestDTO(
        @NotBlank(message = "O título é obrigatório") String titulo,
        @NotBlank(message = "O nível de perigo é obrigatório")
        @Pattern(regexp = "(?i)BAIXO|MODERADO|ALTO|CRITICO", message = "Use BAIXO, MODERADO, ALTO ou CRITICO") String nivelPerigo,
        @NotNull @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0") Double latitude,
        @NotNull @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") Double longitude
) {}
