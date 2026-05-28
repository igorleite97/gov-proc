package com.govproc.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Registro imutavel de uma alteracao critica no sistema.
 *
 * Difere de {@link com.govproc.timeline.domain.ProcessTimelineEvent} em proposito:
 *  - AuditLog: O QUE mudou tecnicamente (campo, valor anterior, valor novo)
 *  - ProcessTimelineEvent: O QUE aconteceu operacionalmente (evento semantico)
 *
 * Nao estende BaseEntity intencionalmente: AuditLog e um log puro,
 * com semantica propria (performedAt), sem campo updatedAt.
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "entity_name", nullable = false, length = 100)
    private String entityName;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "field_name", nullable = false, length = 100)
    private String fieldName;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "performed_by", nullable = false)
    private UUID performedBy;

    @Column(name = "performed_at", updatable = false, nullable = false)
    private Instant performedAt;

    protected AuditLog() { }

    public AuditLog(String entityName, UUID entityId, String fieldName,
                    String oldValue, String newValue, UUID performedBy) {
        this.entityName = entityName;
        this.entityId = entityId;
        this.fieldName = fieldName;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.performedBy = performedBy;
        this.performedAt = Instant.now();
    }

    public UUID getId()             { return id; }
    public String getEntityName()   { return entityName; }
    public UUID getEntityId()       { return entityId; }
    public String getFieldName()    { return fieldName; }
    public String getOldValue()     { return oldValue; }
    public String getNewValue()     { return newValue; }
    public UUID getPerformedBy()    { return performedBy; }
    public Instant getPerformedAt() { return performedAt; }
}
