package com.govproc.audit.service;

import com.govproc.audit.domain.AuditLog;
import com.govproc.audit.dto.AuditLogResponse;
import com.govproc.audit.repository.AuditRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AuditService {

    private final AuditRepository auditRepository;

    public AuditService(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Transactional
    public void record(String entityName, UUID entityId, String fieldName,
                       String oldValue, String newValue, UUID performedBy) {
        auditRepository.save(
                new AuditLog(entityName, entityId, fieldName, oldValue, newValue, performedBy));
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> findByEntity(String entityName, UUID entityId) {
        return auditRepository
                .findByEntityNameAndEntityIdOrderByPerformedAtDesc(entityName, entityId)
                .stream()
                .map(AuditLogResponse::from)
                .toList();
    }
}
