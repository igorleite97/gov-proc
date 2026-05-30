package com.govproc.postbid.controller;

import com.govproc.auth.domain.User;
import com.govproc.postbid.dto.AdjudicateRequest;
import com.govproc.postbid.dto.HomologateRequest;
import com.govproc.postbid.dto.PostBidResponse;
import com.govproc.postbid.service.PostBidService;
import com.govproc.shared.web.ApiResponse;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/processes/{processId}/post-bid")
@Tag(name = "PostBid", description = "Fase pós-disputa — homologação, adjudicação e conclusão")
public class PostBidController {

    private final PostBidService postBidService;

    public PostBidController(PostBidService postBidService) {
        this.postBidService = postBidService;
    }

    @PostMapping("/start")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Iniciar fase pós-disputa",
               description = "Transição WINNER → POST_BID. Cria o registro pós-disputa (PENDING).")
    public ApiResponse<PostBidResponse> start(
            @PathVariable UUID processId,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.ok(
                postBidService.startPostBid(processId, currentUser.getId()),
                "Fase pós-disputa iniciada"
        );
    }

    @PostMapping("/homologate")
    @Operation(summary = "Homologar",
               description = "Transição PENDING → HOMOLOGATED. Registra número e data da homologação.")
    public ApiResponse<PostBidResponse> homologate(
            @PathVariable UUID processId,
            @Valid @RequestBody HomologateRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.ok(
                postBidService.homologate(processId, request, currentUser.getId()),
                "Processo homologado"
        );
    }

    @PostMapping("/adjudicate")
    @Operation(summary = "Adjudicar",
               description = "Transição HOMOLOGATED → ADJUDICATED. Registra número e data da adjudicação.")
    public ApiResponse<PostBidResponse> adjudicate(
            @PathVariable UUID processId,
            @Valid @RequestBody AdjudicateRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.ok(
                postBidService.adjudicate(processId, request, currentUser.getId()),
                "Processo adjudicado"
        );
    }

    @PostMapping("/complete")
    @Operation(summary = "Concluir fase pós-disputa",
               description = "Transição ADJUDICATED → COMPLETED. Habilita a futura ativação do contrato.")
    public ApiResponse<PostBidResponse> complete(
            @PathVariable UUID processId,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.ok(
                postBidService.complete(processId, currentUser.getId()),
                "Fase pós-disputa concluída"
        );
    }

    @GetMapping
    @Operation(summary = "Consultar fase pós-disputa do processo")
    public ApiResponse<PostBidResponse> getPostBid(@PathVariable UUID processId) {
        return ApiResponse.ok(postBidService.getPostBid(processId));
    }
}
