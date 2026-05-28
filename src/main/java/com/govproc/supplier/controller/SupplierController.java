package com.govproc.supplier.controller;

import com.govproc.shared.web.ApiResponse;
import com.govproc.supplier.dto.CreateSupplierRequest;
import com.govproc.supplier.dto.SupplierResponse;
import com.govproc.supplier.service.SupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/suppliers")
@Tag(name = "Suppliers", description = "Gestão de fornecedores")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar fornecedor")
    public ApiResponse<SupplierResponse> create(@Valid @RequestBody CreateSupplierRequest request) {
        return ApiResponse.ok(supplierService.createSupplier(request), "Fornecedor cadastrado com sucesso");
    }

    @GetMapping
    @Operation(summary = "Listar fornecedores ativos")
    public ApiResponse<List<SupplierResponse>> findAll() {
        return ApiResponse.ok(supplierService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar fornecedor por ID")
    public ApiResponse<SupplierResponse> findById(@PathVariable UUID id) {
        return ApiResponse.ok(supplierService.findById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desativar fornecedor",
               description = "Desativação lógica — o cadastro histórico é preservado.")
    public ApiResponse<Void> deactivate(@PathVariable UUID id) {
        supplierService.deactivate(id);
        return ApiResponse.ok(null, "Fornecedor desativado");
    }
}
