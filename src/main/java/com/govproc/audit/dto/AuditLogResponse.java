package com.govproc.audit.dto;

import com.govproc.audit.domain.AuditLog;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        String entityName,
        UUID entityId,
        String fieldName,
        String oldValue,
        String newValue,
        UUID performedBy,
        Instant performedAt
) {

    public static AuditLogResponse from(AuditLog a) {
        return new AuditLogResponse(
                a.getId(),
                a.getEntityName(),
                a.getEntityId(),
                a.getFieldName(),
                a.getOldValue(),
                a.getNewValue(),
                a.getPerformedBy(),
                a.getPerformedAt()
        );
    }
}
