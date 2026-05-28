package com.govproc.audit.controller;

import com.govproc.audit.dto.AuditLogResponse;
import com.govproc.audit.service.AuditService;
import com.govproc.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Audit", description = "Trilha de auditoria de alterações críticas")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/processes/{id}/audit")
    @Operation(summary = "Trilha de auditoria do processo",
               description = "Alterações críticas registradas: mudanças de status, preços, decisões.")
    public ApiResponse<List<AuditLogResponse>> getProcessAudit(@PathVariable UUID id) {
        return ApiResponse.ok(auditService.findByEntity("ProcurementProcess", id));
    }
}
