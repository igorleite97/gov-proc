package com.govproc.process.controller;

import com.govproc.auth.domain.User;
import com.govproc.process.domain.ProcessStatus;
import com.govproc.process.dto.CaptureProcessRequest;
import com.govproc.process.dto.ProcessResponse;
import com.govproc.process.service.ProcessService;
import com.govproc.shared.web.ApiResponse;
import com.govproc.timeline.dto.TimelineEventResponse;
import com.govproc.timeline.service.TimelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/processes")
@Tag(name = "Processes", description = "Gestão de processos licitatórios")
public class ProcessController {

    private final ProcessService processService;
    private final TimelineService timelineService;

    public ProcessController(ProcessService processService, TimelineService timelineService) {
        this.processService = processService;
        this.timelineService = timelineService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Capturar processo",
               description = "Registra uma oportunidade licitatória. Status inicial: CAPTURED.")
    public ApiResponse<ProcessResponse> capture(
            @Valid @RequestBody CaptureProcessRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.ok(
                processService.captureProcess(request, currentUser.getId()),
                "Processo capturado com sucesso"
        );
    }

    @GetMapping
    @Operation(summary = "Listar processos",
               description = "Retorna todos os processos. Filtre por status com ?status=CAPTURED")
    public ApiResponse<List<ProcessResponse>> findAll(
            @RequestParam(required = false) ProcessStatus status) {
        return ApiResponse.ok(processService.findAll(status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar processo por ID")
    public ApiResponse<ProcessResponse> findById(@PathVariable UUID id) {
        return ApiResponse.ok(processService.findById(id));
    }

    @GetMapping("/{id}/timeline")
    @Operation(summary = "Timeline do processo",
               description = "Histórico completo de eventos do processo em ordem cronológica")
    public ApiResponse<List<TimelineEventResponse>> getTimeline(@PathVariable UUID id) {
        processService.findById(id); // verifica existencia — lanca 404 se nao encontrar
        return ApiResponse.ok(timelineService.findByProcessId(id));
    }
}
