package com.govproc.contract.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Rescisao antecipada do contrato. O motivo e obrigatorio e fica registrado
 * na timeline/auditoria (rastreabilidade), sem inflar a entidade Contract.
 */
public record TerminateContractRequest(

        @NotBlank(message = "Motivo da rescisão é obrigatório")
        String reason
) {
}
