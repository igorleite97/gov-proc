package com.govproc.contract.service;

import com.govproc.audit.service.AuditService;
import com.govproc.contract.domain.Addendum;
import com.govproc.contract.domain.Commitment;
import com.govproc.contract.domain.Contract;
import com.govproc.contract.domain.Invoice;
import com.govproc.contract.dto.AddendumResponse;
import com.govproc.contract.dto.ApplyAddendumRequest;
import com.govproc.contract.dto.CommitmentResponse;
import com.govproc.contract.dto.InvoiceResponse;
import com.govproc.contract.dto.RegisterCommitmentRequest;
import com.govproc.contract.dto.RegisterInvoiceRequest;
import com.govproc.contract.repository.AddendumRepository;
import com.govproc.contract.repository.CommitmentRepository;
import com.govproc.contract.repository.ContractRepository;
import com.govproc.contract.repository.InvoiceRepository;
import com.govproc.shared.exception.ResourceNotFoundException;
import com.govproc.timeline.domain.ProcessEventType;
import com.govproc.timeline.service.TimelineService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Operacoes de EXECUCAO do agregado Contract: empenho, fatura e aditivo.
 *
 * Toda mutacao de saldo/capacidade passa pela raiz {@code Contract} (que protege
 * os invariantes). Este service apenas orquestra: carrega a raiz, delega, persiste
 * raiz + filho na mesma transacao, e registra audit/timeline.
 */
@Service
public class ContractExecutionService {

    private final ContractRepository contractRepository;
    private final CommitmentRepository commitmentRepository;
    private final InvoiceRepository invoiceRepository;
    private final AddendumRepository addendumRepository;
    private final AuditService auditService;
    private final TimelineService timelineService;

    public ContractExecutionService(ContractRepository contractRepository,
                                    CommitmentRepository commitmentRepository,
                                    InvoiceRepository invoiceRepository,
                                    AddendumRepository addendumRepository,
                                    AuditService auditService,
                                    TimelineService timelineService) {
        this.contractRepository = contractRepository;
        this.commitmentRepository = commitmentRepository;
        this.invoiceRepository = invoiceRepository;
        this.addendumRepository = addendumRepository;
        this.auditService = auditService;
        this.timelineService = timelineService;
    }

    // ---------------------------------------------------------------- Empenho

    @Transactional
    public CommitmentResponse registerCommitment(UUID processId, RegisterCommitmentRequest request, UUID userId) {
        Contract contract = loadContract(processId);
        BigDecimal oldBalance = contract.getRemainingBalance();

        Commitment commitment = contract.registerCommitment(
                request.commitmentNumber(), request.amount(), request.issueDate(), request.notes(), userId);

        contractRepository.save(contract);
        commitmentRepository.save(commitment);

        auditService.record("Contract", contract.getId(), "remainingBalance",
                oldBalance.toPlainString(), contract.getRemainingBalance().toPlainString(), userId);
        timelineService.record(processId, ProcessEventType.COMMITMENT_REGISTERED,
                "Empenho registrado: " + request.commitmentNumber()
                        + " (" + request.amount().toPlainString() + ")", userId);

        return CommitmentResponse.from(commitment);
    }

    @Transactional(readOnly = true)
    public List<CommitmentResponse> listCommitments(UUID processId) {
        Contract contract = loadContract(processId);
        return commitmentRepository.findByContractIdOrderByIssueDateAsc(contract.getId())
                .stream().map(CommitmentResponse::from).toList();
    }

    // ----------------------------------------------------------------- Fatura

    @Transactional
    public InvoiceResponse registerInvoice(UUID processId, RegisterInvoiceRequest request, UUID userId) {
        Contract contract = loadContract(processId);

        // Fatura NAO altera saldo — apenas registra a execucao.
        Invoice invoice = contract.registerInvoice(
                request.invoiceNumber(), request.amount(), request.issuedAt(), userId);

        invoiceRepository.save(invoice);

        timelineService.record(processId, ProcessEventType.INVOICE_REGISTERED,
                "Fatura registrada: " + request.invoiceNumber()
                        + " (" + request.amount().toPlainString() + ")", userId);

        return InvoiceResponse.from(invoice);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> listInvoices(UUID processId) {
        Contract contract = loadContract(processId);
        return invoiceRepository.findByContractIdOrderByIssuedAtAsc(contract.getId())
                .stream().map(InvoiceResponse::from).toList();
    }

    // ----------------------------------------------------------------- Aditivo

    @Transactional
    public AddendumResponse applyAddendum(UUID processId, ApplyAddendumRequest request, UUID userId) {
        Contract contract = loadContract(processId);
        BigDecimal oldValue = contract.getContractValue();
        BigDecimal oldBalance = contract.getRemainingBalance();

        Addendum addendum = contract.applyAddendum(
                request.addendumNumber(), request.type(), request.valueChange(),
                request.newEndDate(), request.reason(), request.signedAt(), userId);

        contractRepository.save(contract);
        addendumRepository.save(addendum);

        // Audita apenas o que o tipo de aditivo de fato alterou.
        auditIfChanged(contract.getId(), "contractValue", oldValue, contract.getContractValue(), userId);
        auditIfChanged(contract.getId(), "remainingBalance", oldBalance, contract.getRemainingBalance(), userId);
        timelineService.record(processId, ProcessEventType.ADDENDUM_APPLIED,
                "Aditivo aplicado: " + request.addendumNumber() + " [" + request.type() + "]", userId);

        return AddendumResponse.from(addendum);
    }

    @Transactional(readOnly = true)
    public List<AddendumResponse> listAddenda(UUID processId) {
        Contract contract = loadContract(processId);
        return addendumRepository.findByContractIdOrderBySignedAtAsc(contract.getId())
                .stream().map(AddendumResponse::from).toList();
    }

    // -------------------------------------------------------------------------

    private void auditIfChanged(UUID contractId, String field, BigDecimal oldVal, BigDecimal newVal, UUID userId) {
        if (oldVal.compareTo(newVal) != 0) {
            auditService.record("Contract", contractId, field,
                    oldVal.toPlainString(), newVal.toPlainString(), userId);
        }
    }

    private Contract loadContract(UUID processId) {
        return contractRepository.findByProcessId(processId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Contrato não encontrado para o processo: " + processId));
    }
}
