package com.govproc.timeline.domain;

import com.govproc.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Registro imutavel de um evento operacional em um processo.
 *
 * Nao possui relacionamento JPA com {@code ProcurementProcess} nem com {@code User}
 * de proposito: eventos de timeline sao logs — referenciam entidades por UUID
 * sem carregar grafos de objetos. Simples e performatico.
 *
 * Uma vez persistido, este registro nao deve ser alterado.
 */
@Entity
@Table(name = "process_timeline_events")
public class ProcessTimelineEvent extends BaseEntity {

    @Column(name = "process_id", nullable = false)
    private UUID processId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 80)
    private ProcessEventType eventType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "performed_by", nullable = false)
    private UUID performedBy;

    protected ProcessTimelineEvent() { }

    public ProcessTimelineEvent(UUID processId, ProcessEventType eventType,
                                String description, UUID performedBy) {
        this.processId = processId;
        this.eventType = eventType;
        this.description = description;
        this.performedBy = performedBy;
    }

    public UUID getProcessId()           { return processId; }
    public ProcessEventType getEventType() { return eventType; }
    public String getDescription()       { return description; }
    public UUID getPerformedBy()         { return performedBy; }
}
