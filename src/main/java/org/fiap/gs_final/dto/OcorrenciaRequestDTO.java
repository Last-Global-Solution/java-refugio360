package org.fiap.gs_final.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;

public record OcorrenciaRequestDTO(
        @NotBlank(message = "A descrição é obrigatória")
        String descricao,

        @NotBlank(message = "O nível de urgência é obrigatório")
        @Pattern(regexp = "(?i)BAIXA|MEDIA|ALTA|CRITICA", message = "Use BAIXA, MEDIA, ALTA ou CRITICA")
        String urgencia,
        @NotNull(message = "A latitude é obrigatória") @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull(message = "A longitude é obrigatória") @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude
) {}
