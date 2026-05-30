package com.govproc.contract.domain;

import com.govproc.shared.domain.BaseEntity;
import com.govproc.shared.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Contrato decorrente de um processo licitatorio vencido e adjudicado.
 *
 * RESPONSABILIDADE: gestao contratual com vida propria. Nasce na ativacao
 * (POST_BID concluido) e segue seu proprio ciclo (ContractStatus), permitindo
 * evoluir no futuro com empenhos, aditivos, reajustes e medicoes SEM tocar no
 * {@code ProcurementProcess}.
 *
 * Um contrato por processo (UNIQUE em process_id).
 *
 * Padrao monetario: BigDecimal precision=19, scale=4, normalizado na entrada.
 * O saldo (remainingBalance) nasce igual ao valor total do contrato.
 */
@Entity
@Table(name = "contracts")
public class Contract extends BaseEntity {

    private static final int MONETARY_SCALE = 4;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    @Column(name = "process_id", nullable = false, unique = true)
    private UUID processId;

    @Column(name = "contract_number", nullable = false, length = 100)
    private String contractNumber;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "contract_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal contractValue;

    @Column(name = "remaining_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal remainingBalance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContractStatus status;

    @Column(name = "registered_by", nullable = false)
    private UUID registeredBy;

    protected Contract() { }

    public Contract(UUID processId, String contractNumber, LocalDate startDate,
                    LocalDate endDate, BigDecimal contractValue, UUID registeredBy) {
        if (contractNumber == null || contractNumber.isBlank()) {
            throw new BusinessException("Número do contrato é obrigatório");
        }
        if (startDate == null) {
            throw new BusinessException("Data de início do contrato é obrigatória");
        }
        if (contractValue == null || contractValue.signum() <= 0) {
            throw new BusinessException("Valor do contrato deve ser positivo");
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new BusinessException("Data de término não pode ser anterior à data de início");
        }
        this.processId = processId;
        this.contractNumber = contractNumber;
        this.startDate = startDate;
        this.endDate = endDate;
        this.contractValue = contractValue.setScale(MONETARY_SCALE, ROUNDING);
        this.remainingBalance = this.contractValue; // saldo inicial = valor total
        this.registeredBy = registeredBy;
        this.status = ContractStatus.ACTIVE;
    }

    // -------------------------------------------------------------------------
    // Estados terminais — cada um exige contrato ACTIVE.
    // -------------------------------------------------------------------------

    public void close() {
        requireActive("encerrar");
        this.status = ContractStatus.CLOSED;
    }

    public void terminate() {
        requireActive("rescindir");
        this.status = ContractStatus.TERMINATED;
    }

    public void expire() {
        requireActive("marcar como vencido");
        this.status = ContractStatus.EXPIRED;
    }

    // -------------------------------------------------------------------------
    // Membros do agregado — a raiz protege os invariantes de saldo/capacidade.
    // -------------------------------------------------------------------------

    /**
     * Empenho: reserva orcamentaria. Valida saldo e o decrementa.
     * INVARIANTE: nao se empenha alem do saldo disponivel.
     */
    public Commitment registerCommitment(String commitmentNumber, BigDecimal amount,
                                         LocalDate issueDate, String notes, UUID registeredBy) {
        requireActive("registrar empenho");
        if (commitmentNumber == null || commitmentNumber.isBlank()) {
            throw new BusinessException("Número do empenho é obrigatório");
        }
        BigDecimal value = requirePositive(amount, "do empenho");
        if (value.compareTo(this.remainingBalance) > 0) {
            throw new BusinessException(
                    "Empenho de " + value.toPlainString() + " excede o saldo disponível de "
                            + this.remainingBalance.toPlainString());
        }
        this.remainingBalance = this.remainingBalance.subtract(value);
        return new Commitment(this.getId(), commitmentNumber, value, issueDate, notes, registeredBy);
    }

    /**
     * Fatura: registro de execucao. NAO consome saldo (ja reservado no empenho).
     */
    public Invoice registerInvoice(String invoiceNumber, BigDecimal amount,
                                   LocalDate issuedAt, UUID registeredBy) {
        requireActive("registrar fatura");
        if (invoiceNumber == null || invoiceNumber.isBlank()) {
            throw new BusinessException("Número da fatura é obrigatório");
        }
        BigDecimal value = requirePositive(amount, "da fatura");
        return new Invoice(this.getId(), invoiceNumber, value, issuedAt, registeredBy);
    }

    /**
     * Aditivo: altera a capacidade do contrato (valor ou prazo).
     * VALUE_INCREASE soma; VALUE_DECREASE valida e subtrai (de valor e saldo);
     * TERM_* ajustam a vigencia (endDate) sem tocar em valores.
     */
    public Addendum applyAddendum(String addendumNumber, AddendumType type, BigDecimal valueChange,
                                  LocalDate newEndDate, String reason, LocalDate signedAt, UUID registeredBy) {
        requireActive("registrar aditivo");
        if (addendumNumber == null || addendumNumber.isBlank()) {
            throw new BusinessException("Número do aditivo é obrigatório");
        }
        if (type == null) {
            throw new BusinessException("Tipo do aditivo é obrigatório");
        }

        BigDecimal appliedValue = null;
        LocalDate appliedEndDate = null;

        switch (type) {
            case VALUE_INCREASE -> {
                appliedValue = requirePositive(valueChange, "do aditivo de acréscimo");
                this.contractValue = this.contractValue.add(appliedValue);
                this.remainingBalance = this.remainingBalance.add(appliedValue);
            }
            case VALUE_DECREASE -> {
                appliedValue = requirePositive(valueChange, "do aditivo de supressão");
                if (appliedValue.compareTo(this.remainingBalance) > 0) {
                    throw new BusinessException(
                            "Supressão de " + appliedValue.toPlainString()
                                    + " excede o saldo disponível de " + this.remainingBalance.toPlainString());
                }
                this.contractValue = this.contractValue.subtract(appliedValue);
                this.remainingBalance = this.remainingBalance.subtract(appliedValue);
            }
            case TERM_EXTENSION, TERM_REDUCTION -> {
                if (newEndDate == null) {
                    throw new BusinessException("Nova data de término é obrigatória para aditivo de prazo");
                }
                if (newEndDate.isBefore(this.startDate)) {
                    throw new BusinessException("Nova data de término não pode ser anterior ao início do contrato");
                }
                this.endDate = newEndDate;
                appliedEndDate = newEndDate;
            }
        }

        return new Addendum(this.getId(), addendumNumber, type, appliedValue,
                appliedEndDate, reason, signedAt, registeredBy);
    }

    // -------------------------------------------------------------------------

    private void requireActive(String action) {
        if (this.status != ContractStatus.ACTIVE) {
            throw new BusinessException("Contrato deve estar ACTIVE para " + action);
        }
    }

    /** Valida positividade e normaliza para a escala monetaria. */
    private BigDecimal requirePositive(BigDecimal value, String label) {
        if (value == null || value.signum() <= 0) {
            throw new BusinessException("Valor " + label + " deve ser positivo");
        }
        return value.setScale(MONETARY_SCALE, ROUNDING);
    }

    public UUID getProcessId()             { return processId; }
    public String getContractNumber()      { return contractNumber; }
    public LocalDate getStartDate()        { return startDate; }
    public LocalDate getEndDate()          { return endDate; }
    public BigDecimal getContractValue()   { return contractValue; }
    public BigDecimal getRemainingBalance() { return remainingBalance; }
    public ContractStatus getStatus()      { return status; }
    public UUID getRegisteredBy()          { return registeredBy; }
}
