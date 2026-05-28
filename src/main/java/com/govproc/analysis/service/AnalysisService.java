package com.govproc.analysis.service;

import com.govproc.analysis.domain.AnalysisDecision;
import com.govproc.analysis.domain.ProcessAnalysis;
import com.govproc.analysis.dto.AnalysisResponse;
import com.govproc.analysis.dto.ApproveAnalysisRequest;
import com.govproc.analysis.dto.RejectAnalysisRequest;
import com.govproc.analysis.dto.StartAnalysisRequest;
import com.govproc.analysis.repository.AnalysisRepository;
import com.govproc.audit.service.AuditService;
import com.govproc.process.domain.ProcurementProcess;
import com.govproc.process.repository.ProcessRepository;
import com.govproc.shared.exception.BusinessException;
import com.govproc.shared.exception.ResourceNotFoundException;
import com.govproc.timeline.domain.ProcessEventType;
import com.govproc.timeline.service.TimelineService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AnalysisService {

    private final ProcessRepository processRepository;
    private final AnalysisRepository analysisRepository;
    private final AuditService auditService;
    private final TimelineService timelineService;

    public AnalysisService(ProcessRepository processRepository,
                           AnalysisRepository analysisRepository,
                           AuditService auditService,
                           TimelineService timelineService) {
        this.processRepository = processRepository;
        this.analysisRepository = analysisRepository;
        this.auditService = auditService;
        this.timelineService = timelineService;
    }

    @Transactional
    public AnalysisResponse startAnalysis(UUID processId, StartAnalysisRequest request, UUID userId) {
        if (analysisRepository.existsByProcessId(processId)) {
            throw new BusinessException("Análise já iniciada para o processo: " + processId);
        }

        ProcurementProcess process = loadProcess(processId);
        String oldStatus = process.getStatus().name();

        process.startAnalysis(); // valida CAPTURED → UNDER_ANALYSIS

        ProcessAnalysis analysis = new ProcessAnalysis(
                processId,
                request.sampleRequired(),
                request.allowsAdhesion(),
                request.proposalGuarantee(),
                request.deliveryDeadline(),
                request.observations(),
                userId
        );

        processRepository.save(process);
        analysisRepository.save(analysis);

        // Auditoria: alteracao critica de status
        auditService.record("ProcurementProcess", processId, "status",
                oldStatus, "UNDER_ANALYSIS", userId);

        // Timeline: evento operacional
        timelineService.record(processId, ProcessEventType.ANALYSIS_STARTED,
                "Análise iniciada", userId);

        return AnalysisResponse.from(analysis);
    }

    @Transactional
    public AnalysisResponse approveAnalysis(UUID processId, ApproveAnalysisRequest request, UUID userId) {
        ProcurementProcess process = loadProcess(processId);
        String oldStatus = process.getStatus().name();

        process.approveAnalysis(); // valida UNDER_ANALYSIS → ANALYSIS_APPROVED

        ProcessAnalysis analysis = loadAnalysis(processId);
        analysis.approve(request.observations());

        processRepository.save(process);
        analysisRepository.save(analysis);

        auditService.record("ProcurementProcess", processId, "status",
                oldStatus, "ANALYSIS_APPROVED", userId);

        timelineService.record(processId, ProcessEventType.ANALYSIS_APPROVED,
                "Análise aprovada", userId);

        return AnalysisResponse.from(analysis);
    }

    @Transactional
    public AnalysisResponse rejectAnalysis(UUID processId, RejectAnalysisRequest request, UUID userId) {
        ProcurementProcess process = loadProcess(processId);
        String oldStatus = process.getStatus().name();

        // processo e analise validam o motivo independentemente (defesa em profundidade)
        process.rejectAnalysis(request.rejectionReason());

        ProcessAnalysis analysis = loadAnalysis(processId);
        analysis.reject(request.rejectionReason());

        processRepository.save(process);
        analysisRepository.save(analysis);

        auditService.record("ProcurementProcess", processId, "status",
                oldStatus, "ANALYSIS_REJECTED", userId);

        timelineService.record(processId, ProcessEventType.ANALYSIS_REJECTED,
                "Análise reprovada: " + request.rejectionReason(), userId);

        return AnalysisResponse.from(analysis);
    }

    @Transactional(readOnly = true)
    public AnalysisResponse getAnalysis(UUID processId) {
        if (!processRepository.existsById(processId)) {
            throw new ResourceNotFoundException("Processo não encontrado: " + processId);
        }
        return AnalysisResponse.from(loadAnalysis(processId));
    }

    private ProcurementProcess loadProcess(UUID processId) {
        return processRepository.findById(processId)
                .orElseThrow(() -> new ResourceNotFoundException("Processo não encontrado: " + processId));
    }

    private ProcessAnalysis loadAnalysis(UUID processId) {
        return analysisRepository.findByProcessId(processId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Análise não encontrada para o processo: " + processId));
    }
}
