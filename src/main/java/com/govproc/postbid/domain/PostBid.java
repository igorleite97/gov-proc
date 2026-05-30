package com.govproc.postbid.domain;

import com.govproc.shared.domain.BaseEntity;
import com.govproc.shared.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Fase pos-disputa de um processo licitatorio: homologacao → adjudicacao → conclusao.
 *
 * RESPONSABILIDADE: encapsular o rito juridico que sucede a vitoria (WINNER) e
 * antecede a ativacao do contrato (CONTRACT_ACTIVE). O {@code ProcurementProcess}
 * permanece simples — sabe apenas que esta em POST_BID; os atos (numeros e datas
 * de homologacao/adjudicacao) vivem aqui.
 *
 * Uma fase pos-disputa por processo (UNIQUE em process_id).
 *
 * Maquina de estados propria (PostBidStatus): cada transicao e um metodo de
 * dominio com validacao interna — mesma filosofia de ProcurementProcess e Dispute.
 */
@Entity
@Table(name = "post_bids")
public class PostBid extends BaseEntity {

    @Column(name = "process_id", nullable = false, unique = true)
    private UUID processId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostBidStatus status;

    @Column(name = "homologation_number", length = 100)
    private String homologationNumber;

    @Column(name = "homologation_date")
    private LocalDate homologationDate;

    @Column(name = "adjudication_number", length = 100)
    private String adjudicationNumber;

    @Column(name = "adjudication_date")
    private LocalDate adjudicationDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "registered_by", nullable = false)
    private UUID registeredBy;

    protected PostBid() { }

    public PostBid(UUID processId, UUID registeredBy) {
        this.processId = processId;
        this.registeredBy = registeredBy;
        this.status = PostBidStatus.PENDING;
    }

    // -------------------------------------------------------------------------
    // Maquina de estados — PENDING → HOMOLOGATED → ADJUDICATED → COMPLETED.
    // -------------------------------------------------------------------------

    public void homologate(String homologationNumber, LocalDate homologationDate, String notes) {
        requireStatus(PostBidStatus.PENDING,
                "Pós-disputa deve estar PENDING para homologar");
        if (homologationNumber == null || homologationNumber.isBlank()) {
            throw new BusinessException("Número da homologação é obrigatório");
        }
        this.homologationNumber = homologationNumber;
        this.homologationDate = homologationDate != null ? homologationDate : LocalDate.now();
        appendNotes(notes);
        this.status = PostBidStatus.HOMOLOGATED;
    }

    public void adjudicate(String adjudicationNumber, LocalDate adjudicationDate, String notes) {
        requireStatus(PostBidStatus.HOMOLOGATED,
                "Pós-disputa deve estar HOMOLOGATED para adjudicar");
        if (adjudicationNumber == null || adjudicationNumber.isBlank()) {
            throw new BusinessException("Número da adjudicação é obrigatório");
        }
        this.adjudicationNumber = adjudicationNumber;
        this.adjudicationDate = adjudicationDate != null ? adjudicationDate : LocalDate.now();
        appendNotes(notes);
        this.status = PostBidStatus.ADJUDICATED;
    }

    public void complete() {
        requireStatus(PostBidStatus.ADJUDICATED,
                "Pós-disputa deve estar ADJUDICATED para ser concluída");
        this.status = PostBidStatus.COMPLETED;
    }

    public boolean isCompleted() {
        return this.status == PostBidStatus.COMPLETED;
    }

    // -------------------------------------------------------------------------

    private void appendNotes(String extra) {
        if (extra == null || extra.isBlank()) {
            return;
        }
        this.notes = (this.notes == null || this.notes.isBlank())
                ? extra
                : this.notes + System.lineSeparator() + extra;
    }

    private void requireStatus(PostBidStatus expected, String message) {
        if (this.status != expected) {
            throw new BusinessException(message);
        }
    }

    // -------------------------------------------------------------------------
    // Getters — sem setters; estado muda apenas pelos metodos de dominio.
    // -------------------------------------------------------------------------

    public UUID getProcessId()             { return processId; }
    public PostBidStatus getStatus()       { return status; }
    public String getHomologationNumber()  { return homologationNumber; }
    public LocalDate getHomologationDate() { return homologationDate; }
    public String getAdjudicationNumber()  { return adjudicationNumber; }
    public LocalDate getAdjudicationDate() { return adjudicationDate; }
    public String getNotes()               { return notes; }
    public UUID getRegisteredBy()          { return registeredBy; }
}
