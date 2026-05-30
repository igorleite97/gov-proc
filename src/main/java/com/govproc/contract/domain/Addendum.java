package com.govproc.contract.domain;

import com.govproc.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Termo aditivo — altera a capacidade do contrato (valor ou prazo).
 *
 * Membro do agregado Contract: nasce via {@code Contract.applyAddendum(...)},
 * que aplica o efeito na raiz (contractValue/remainingBalance ou endDate) com
 * as validacoes pertinentes. O aditivo em si e o registro imutavel do ato.
 *
 * Para tipos VALUE_*, {@code valueChange} carrega o montante; {@code newEndDate} e nulo.
 * Para tipos TERM_*, {@code newEndDate} carrega a nova vigencia; {@code valueChange} e nulo.
 */
@Entity
@Table(name = "addenda")
public class Addendum extends BaseEntity {

    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    @Column(name = "addendum_number", nullable = false, length = 100)
    private String addendumNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AddendumType type;

    @Column(name = "value_change", precision = 19, scale = 4)
    private BigDecimal valueChange;

    @Column(name = "new_end_date")
    private LocalDate newEndDate;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "signed_at")
    private LocalDate signedAt;

    @Column(name = "registered_by", nullable = false)
    private UUID registeredBy;

    protected Addendum() { }

    Addendum(UUID contractId, String addendumNumber, AddendumType type,
             BigDecimal valueChange, LocalDate newEndDate, String reason,
             LocalDate signedAt, UUID registeredBy) {
        this.contractId = contractId;
        this.addendumNumber = addendumNumber;
        this.type = type;
        this.valueChange = valueChange;
        this.newEndDate = newEndDate;
        this.reason = reason;
        this.signedAt = signedAt != null ? signedAt : LocalDate.now();
        this.registeredBy = registeredBy;
    }

    public UUID getContractId()      { return contractId; }
    public String getAddendumNumber() { return addendumNumber; }
    public AddendumType getType()    { return type; }
    public BigDecimal getValueChange() { return valueChange; }
    public LocalDate getNewEndDate() { return newEndDate; }
    public String getReason()        { return reason; }
    public LocalDate getSignedAt()   { return signedAt; }
    public UUID getRegisteredBy()    { return registeredBy; }
}
