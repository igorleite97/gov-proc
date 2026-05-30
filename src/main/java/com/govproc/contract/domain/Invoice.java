package com.govproc.contract.domain;

import com.govproc.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Fatura — registro de execucao/liquidacao do contrato.
 *
 * IMPORTANTE: NAO consome remainingBalance. O saldo ja foi reservado no empenho.
 * A fatura responde "o fornecedor entregou?", nao "quanto ainda posso comprometer?".
 * Por isso e um registro simples, sem efeito sobre o saldo do contrato.
 */
@Entity
@Table(name = "invoices")
public class Invoice extends BaseEntity {

    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    @Column(name = "invoice_number", nullable = false, length = 100)
    private String invoiceNumber;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "issued_at", nullable = false)
    private LocalDate issuedAt;

    @Column(name = "registered_by", nullable = false)
    private UUID registeredBy;

    protected Invoice() { }

    Invoice(UUID contractId, String invoiceNumber, BigDecimal amount,
            LocalDate issuedAt, UUID registeredBy) {
        this.contractId = contractId;
        this.invoiceNumber = invoiceNumber;
        this.amount = amount;
        this.issuedAt = issuedAt != null ? issuedAt : LocalDate.now();
        this.registeredBy = registeredBy;
    }

    public UUID getContractId()     { return contractId; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public BigDecimal getAmount()   { return amount; }
    public LocalDate getIssuedAt()  { return issuedAt; }
    public UUID getRegisteredBy()   { return registeredBy; }
}
