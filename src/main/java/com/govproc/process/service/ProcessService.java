package com.govproc.process.service;

import com.govproc.process.domain.ProcurementProcess;
import com.govproc.process.domain.ProcessStatus;
import com.govproc.process.dto.CaptureProcessRequest;
import com.govproc.process.dto.ProcessResponse;
import com.govproc.process.repository.ProcessRepository;
import com.govproc.shared.exception.BusinessException;
import com.govproc.shared.exception.ResourceNotFoundException;
import com.govproc.timeline.domain.ProcessEventType;
import com.govproc.timeline.service.TimelineService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProcessService {

    private final ProcessRepository processRepository;
    private final TimelineService timelineService;

    public ProcessService(ProcessRepository processRepository, TimelineService timelineService) {
        this.processRepository = processRepository;
        this.timelineService = timelineService;
    }

    @Transactional
    public ProcessResponse captureProcess(CaptureProcessRequest request, UUID createdBy) {
        if (processRepository.existsByProcessNumberAndUasg(request.processNumber(), request.uasg())) {
            throw new BusinessException(
                    "Processo já cadastrado: " + request.processNumber() + " / UASG " + request.uasg());
        }

        ProcurementProcess process = new ProcurementProcess(
                request.processNumber(),
                request.uasg(),
                request.agency(),
                request.portal(),
                request.objectDescription(),
                request.publicationDate(),
                request.openingDate(),
                request.disputeDate(),
                request.priority(),
                request.risk(),
                createdBy
        );
        processRepository.save(process);

        // Timeline nasce junto do processo — rastreabilidade desde o primeiro instante
        timelineService.record(
                process.getId(),
                ProcessEventType.PROCESS_CAPTURED,
                "Processo capturado no sistema",
                createdBy
        );

        return ProcessResponse.from(process);
    }

    @Transactional(readOnly = true)
    public List<ProcessResponse> findAll(ProcessStatus status) {
        List<ProcurementProcess> processes = status != null
                ? processRepository.findByStatus(status)
                : processRepository.findAllByOrderByCreatedAtDesc();

        return processes.stream()
                .map(ProcessResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProcessResponse findById(UUID id) {
        ProcurementProcess process = processRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Processo não encontrado: " + id));
        return ProcessResponse.from(process);
    }
}
