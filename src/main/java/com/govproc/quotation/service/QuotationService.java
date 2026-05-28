package com.govproc.quotation.service;

import com.govproc.audit.service.AuditService;
import com.govproc.process.domain.ProcurementProcess;
import com.govproc.process.domain.ProcessStatus;
import com.govproc.process.dto.ProcessResponse;
import com.govproc.process.repository.ProcessRepository;
import com.govproc.quotation.domain.Quotation;
import com.govproc.quotation.dto.AddQuotationRequest;
import com.govproc.quotation.dto.QuotationResponse;
import com.govproc.quotation.repository.QuotationRepository;
import com.govproc.shared.exception.BusinessException;
import com.govproc.shared.exception.ResourceNotFoundException;
import com.govproc.supplier.repository.SupplierRepository;
import com.govproc.timeline.domain.ProcessEventType;
import com.govproc.timeline.service.TimelineService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class QuotationService {

    private final ProcessRepository processRepository;
    private final QuotationRepository quotationRepository;
    private final SupplierRepository supplierRepository;
    private final AuditService auditService;
    private final TimelineService timelineService;

    public QuotationService(ProcessRepository processRepository,
                            QuotationRepository quotationRepository,
                            SupplierRepository supplierRepository,
                            AuditService auditService,
                            TimelineService timelineService) {
        this.processRepository = processRepository;
        this.quotationRepository = quotationRepository;
        this.supplierRepository = supplierRepository;
        this.auditService = auditService;
        this.timelineService = timelineService;
    }

    @Transactional
    public ProcessResponse startQuotation(UUID processId, UUID userId) {
        ProcurementProcess process = loadProcess(processId);
        String oldStatus = process.getStatus().name();

        process.startQuotation(); // valida ANALYSIS_APPROVED → IN_QUOTATION

        processRepository.save(process);
        auditService.record("ProcurementProcess", processId, "status", oldStatus, "IN_QUOTATION", userId);
        timelineService.record(processId, ProcessEventType.QUOTATION_STARTED, "Fase de cotação iniciada", userId);

        return ProcessResponse.from(process);
    }

    @Transactional
    public QuotationResponse addQuotation(UUID processId, AddQuotationRequest request, UUID userId) {
        ProcurementProcess process = loadProcess(processId);
        if (process.getStatus() != ProcessStatus.IN_QUOTATION) {
            throw new BusinessException("Processo deve estar IN_QUOTATION para adicionar cotação");
        }
        if (!supplierRepository.existsById(request.supplierId())) {
            throw new ResourceNotFoundException("Fornecedor não encontrado: " + request.supplierId());
        }

        Quotation quotation = new Quotation(
                processId,
                request.supplierId(),
                request.productDescription(),
                request.unitCost(),
                request.quantity(),
                request.shippingCost(),
                request.manufacturer(),
                request.brand(),
                request.deliveryDays(),
                request.observations(),
                userId
        );
        quotationRepository.save(quotation);

        timelineService.record(processId, ProcessEventType.QUOTATION_ADDED,
                "Cotação adicionada: " + request.productDescription(), userId);

        return QuotationResponse.from(quotation);
    }

    @Transactional
    public QuotationResponse selectQuotation(UUID processId, UUID quotationId, UUID userId) {
        ProcurementProcess process = loadProcess(processId);
        if (process.getStatus() != ProcessStatus.IN_QUOTATION) {
            throw new BusinessException("Processo deve estar IN_QUOTATION para selecionar cotação");
        }

        Quotation quotation = quotationRepository.findByIdAndProcessId(quotationId, processId)
                .orElseThrow(() -> new ResourceNotFoundException("Cotação não encontrada: " + quotationId));

        // Atomic: deseleciona todas, seleciona a escolhida
        quotationRepository.clearSelectedByProcess(processId);
        quotation.select();
        quotationRepository.save(quotation);

        auditService.record("Quotation", quotationId, "selected", "false", "true", userId);
        timelineService.record(processId, ProcessEventType.QUOTATION_SELECTED,
                "Cotação selecionada: " + quotation.getProductDescription(), userId);

        return QuotationResponse.from(quotation);
    }

    @Transactional
    public ProcessResponse markAsQuoted(UUID processId, UUID userId) {
        if (!quotationRepository.existsByProcessIdAndSelectedTrue(processId)) {
            throw new BusinessException("Selecione ao menos uma cotação antes de marcar o processo como cotado");
        }

        ProcurementProcess process = loadProcess(processId);
        String oldStatus = process.getStatus().name();

        process.markAsQuoted(); // valida IN_QUOTATION → QUOTED

        processRepository.save(process);
        auditService.record("ProcurementProcess", processId, "status", oldStatus, "QUOTED", userId);
        timelineService.record(processId, ProcessEventType.PROCESS_QUOTED, "Processo cotado", userId);

        return ProcessResponse.from(process);
    }

    @Transactional(readOnly = true)
    public List<QuotationResponse> findByProcess(UUID processId) {
        if (!processRepository.existsById(processId)) {
            throw new ResourceNotFoundException("Processo não encontrado: " + processId);
        }
        return quotationRepository.findByProcessIdOrderByTotalCostAsc(processId)
                .stream()
                .map(QuotationResponse::from)
                .toList();
    }

    private ProcurementProcess loadProcess(UUID processId) {
        return processRepository.findById(processId)
                .orElseThrow(() -> new ResourceNotFoundException("Processo não encontrado: " + processId));
    }
}
