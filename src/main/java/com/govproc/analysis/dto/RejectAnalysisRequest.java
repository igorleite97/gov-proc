package com.govproc.analysis.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectAnalysisRequest(
        @NotBlank(message = "Motivo da reprovação é obrigatório")
        String rejectionReason
) {
}
