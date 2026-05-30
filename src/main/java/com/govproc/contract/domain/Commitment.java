package com.govproc.contract.domain;

import com.govproc.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Empenho — reserva orcamentaria sobre um contrato.
 *
 * Membro do agregado Contract: nasce exclusivamente via
 * {@code Contract.registerCommitment(...)}, que valida o saldo e o decrementa.
 * Referencia o contrato por {@code contractId} (UUID) — sem @ManyToOne/cascade,
 * coerente com o restante do projeto.
 */
@Entity
@Table(name = "commitments")
public class Commitment extends BaseEntity {

    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    @Column(name = "commitment_number", nullable = false, length = 100)
    private String commitmentNumber;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "registered_by", nullable = false)
    private UUID registeredBy;

    protected Commitment() { }

    Commitment(UUID contractId, String commitmentNumber, BigDecimal amount,
               LocalDate issueDate, String notes, UUID registeredBy) {
        this.contractId = contractId;
        this.commitmentNumber = commitmentNumber;
        this.amount = amount;
        this.issueDate = issueDate != null ? issueDate : LocalDate.now();
        this.notes = notes;
        this.registeredBy = registeredBy;
    }

    public UUID getContractId()        { return contractId; }
    public String getCommitmentNumber() { return commitmentNumber; }
    public BigDecimal getAmount()      { return amount; }
    public LocalDate getIssueDate()    { return issueDate; }
    public String getNotes()           { return notes; }
    public UUID getRegisteredBy()      { return registeredBy; }
}
