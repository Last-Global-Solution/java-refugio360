package org.fiap.gs_final.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

public record LocalSeguroRequestDTO(
        @NotBlank(message = "O nome é obrigatório")
        String nome,

        @NotBlank(message = "O tipo é obrigatório (ex: ABRIGO, UBS)")
        String tipo,

        @NotBlank(message = "O endereço é obrigatório")
        String endereco,

        @NotNull(message = "A latitude é obrigatória") @DecimalMin("-90.0") @DecimalMax("90.0")
        Double latitude,

        @NotNull(message = "A longitude é obrigatória") @DecimalMin("-180.0") @DecimalMax("180.0")
        Double longitude,

        String descricao
) {}
