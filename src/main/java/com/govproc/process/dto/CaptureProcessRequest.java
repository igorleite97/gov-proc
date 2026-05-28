package com.govproc.process.dto;

import com.govproc.process.domain.PriorityLevel;
import com.govproc.process.domain.RiskLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CaptureProcessRequest(
        @NotBlank(message = "Número do processo é obrigatório")
        @Size(max = 100)
        String processNumber,

        @NotBlank(message = "UASG é obrigatória")
        @Size(max = 20)
        String uasg,

        @NotBlank(message = "Órgão é obrigatório")
        @Size(max = 255)
        String agency,

        @NotBlank(message = "Portal é obrigatório")
        @Size(max = 100)
        String portal,

        @NotBlank(message = "Objeto da licitação é obrigatório")
        String objectDescription,

        LocalDate publicationDate,
        LocalDate openingDate,
        LocalDate disputeDate,

        @NotNull(message = "Prioridade é obrigatória")
        PriorityLevel priority,

        @NotNull(message = "Nível de risco é obrigatório")
        RiskLevel risk
) {
}
