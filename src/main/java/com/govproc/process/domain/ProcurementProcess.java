package com.govproc.process.domain;

import com.govproc.shared.domain.BaseEntity;
import com.govproc.shared.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Entidade central do GovProc. Representa um processo licitatorio completo,
 * desde a captacao ate o contrato.
 *
 * O campo {@code status} e PRIVADO e sem setter publico. Toda mudanca de estado
 * ocorre exclusivamente por metodos de dominio ({@code startAnalysis()},
 * {@code approveAnalysis()}, etc.), que validam a transicao antes de executar.
 * Isso garante que nenhum status invalido possa existir no sistema.
 */
@Entity
@Table(
        name = "procurement_processes",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_process_number_uasg",
                columnNames = {"process_number", "uasg"}
        )
)
public class ProcurementProcess extends BaseEntity {

    @Column(name = "process_number", nullable = false, length = 100)
    private String processNumber;

    @Column(nullable = false, length = 20)
    private String uasg;

    @Column(nullable = false, length = 255)
    private String agency;

    @Column(nullable = false, length = 100)
    private String portal;

    @Column(name = "object_description", nullable = false, columnDefinition = "TEXT")
    private String objectDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ProcessStatus status;

    @Column(name = "publication_date")
    private LocalDate publicationDate;

    @Column(name = "opening_date")
    private LocalDate openingDate;

    @Column(name = "dispute_date")
    private LocalDate disputeDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PriorityLevel priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RiskLevel risk;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    protected ProcurementProcess() { }

    public ProcurementProcess(String processNumber, String uasg, String agency, String portal,
                               String objectDescription, LocalDate publicationDate,
                               LocalDate openingDate, LocalDate disputeDate,
                               PriorityLevel priority, RiskLevel risk, UUID createdBy) {
        this.processNumber = processNumber;
        this.uasg = uasg;
        this.agency = agency;
        this.portal = portal;
        this.objectDescription = objectDescription;
        this.publicationDate = publicationDate;
        this.openingDate = openingDate;
        this.disputeDate = disputeDate;
        this.priority = priority;
        this.risk = risk;
        this.createdBy = createdBy;
        this.status = ProcessStatus.CAPTURED; // todo processo nasce como CAPTURED
    }

    // -------------------------------------------------------------------------
    // Maquina de estados — cada metodo encapsula a regra de transicao valida.
    // -------------------------------------------------------------------------

    public void startAnalysis() {
        requireStatus(ProcessStatus.CAPTURED,
                "Processo deve estar CAPTURED para iniciar análise");
        this.status = ProcessStatus.UNDER_ANALYSIS;
    }

    public void approveAnalysis() {
        requireStatus(ProcessStatus.UNDER_ANALYSIS,
                "Processo deve estar UNDER_ANALYSIS para ser aprovado");
        this.status = ProcessStatus.ANALYSIS_APPROVED;
    }

    public void rejectAnalysis(String reason) {
        requireStatus(ProcessStatus.UNDER_ANALYSIS,
                "Processo deve estar UNDER_ANALYSIS para ser reprovado");
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("Motivo da reprovação é obrigatório");
        }
        this.status = ProcessStatus.ANALYSIS_REJECTED;
    }

    public void startQuotation() {
        requireStatus(ProcessStatus.ANALYSIS_APPROVED,
                "Processo deve estar ANALYSIS_APPROVED para iniciar cotação");
        this.status = ProcessStatus.IN_QUOTATION;
    }

    public void markAsQuoted() {
        requireStatus(ProcessStatus.IN_QUOTATION,
                "Processo deve estar IN_QUOTATION para ser marcado como cotado");
        this.status = ProcessStatus.QUOTED;
    }

    public void startDispute() {
        requireStatus(ProcessStatus.QUOTED,
                "Processo deve estar QUOTED para iniciar disputa");
        this.status = ProcessStatus.IN_DISPUTE;
    }

    public void markAsWinner() {
        requireStatus(ProcessStatus.IN_DISPUTE,
                "Processo deve estar IN_DISPUTE para ser marcado como vencedor");
        this.status = ProcessStatus.WINNER;
    }

    public void markAsLoser() {
        requireStatus(ProcessStatus.IN_DISPUTE,
                "Processo deve estar IN_DISPUTE para ser marcado como perdedor");
        this.status = ProcessStatus.LOSER;
    }

    public void activateContract() {
        requireStatus(ProcessStatus.WINNER,
                "Processo deve estar WINNER para ativar contrato");
        this.status = ProcessStatus.CONTRACT_ACTIVE;
    }

    public void close() {
        requireStatus(ProcessStatus.CONTRACT_ACTIVE,
                "Processo deve estar CONTRACT_ACTIVE para ser encerrado");
        this.status = ProcessStatus.CLOSED;
    }

    // -------------------------------------------------------------------------
    // Getters — sem setters; estado muda apenas pelos metodos acima.
    // -------------------------------------------------------------------------

    public String getProcessNumber()     { return processNumber; }
    public String getUasg()              { return uasg; }
    public String getAgency()            { return agency; }
    public String getPortal()            { return portal; }
    public String getObjectDescription() { return objectDescription; }
    public ProcessStatus getStatus()     { return status; }
    public LocalDate getPublicationDate() { return publicationDate; }
    public LocalDate getOpeningDate()    { return openingDate; }
    public LocalDate getDisputeDate()    { return disputeDate; }
    public PriorityLevel getPriority()   { return priority; }
    public RiskLevel getRisk()           { return risk; }
    public UUID getCreatedBy()           { return createdBy; }

    // -------------------------------------------------------------------------
    // Guardiao privado da maquina de estados.
    // -------------------------------------------------------------------------

    private void requireStatus(ProcessStatus expected, String message) {
        if (this.status != expected) {
            throw new BusinessException(message);
        }
    }
}
