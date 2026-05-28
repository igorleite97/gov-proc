package com.govproc.process.dto;

import com.govproc.process.domain.PriorityLevel;
import com.govproc.process.domain.ProcurementProcess;
import com.govproc.process.domain.ProcessStatus;
import com.govproc.process.domain.RiskLevel;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ProcessResponse(
        UUID id,
        String processNumber,
        String uasg,
        String agency,
        String portal,
        String objectDescription,
        ProcessStatus status,
        LocalDate publicationDate,
        LocalDate openingDate,
        LocalDate disputeDate,
        PriorityLevel priority,
        RiskLevel risk,
        UUID createdBy,
        Instant createdAt
) {

    /**
     * Mapeamento direto e explicito: sem mapper class, sem reflexao.
     * Se o contrato do ProcessResponse mudar, o compilador aponta aqui.
     */
    public static ProcessResponse from(ProcurementProcess p) {
        return new ProcessResponse(
                p.getId(),
                p.getProcessNumber(),
                p.getUasg(),
                p.getAgency(),
                p.getPortal(),
                p.getObjectDescription(),
                p.getStatus(),
                p.getPublicationDate(),
                p.getOpeningDate(),
                p.getDisputeDate(),
                p.getPriority(),
                p.getRisk(),
                p.getCreatedBy(),
                p.getCreatedAt()
        );
    }
}
