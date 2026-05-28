package com.govproc.supplier.service;

import com.govproc.shared.exception.BusinessException;
import com.govproc.shared.exception.ResourceNotFoundException;
import com.govproc.supplier.domain.Supplier;
import com.govproc.supplier.dto.CreateSupplierRequest;
import com.govproc.supplier.dto.SupplierResponse;
import com.govproc.supplier.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public SupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    @Transactional
    public SupplierResponse createSupplier(CreateSupplierRequest request) {
        if (supplierRepository.existsByDocument(request.document())) {
            throw new BusinessException("Fornecedor já cadastrado com documento: " + request.document());
        }
        Supplier supplier = new Supplier(
                request.companyName(),
                request.tradeName(),
                request.document(),
                request.email(),
                request.phone(),
                request.segment()
        );
        supplierRepository.save(supplier);
        return SupplierResponse.from(supplier);
    }

    @Transactional(readOnly = true)
    public List<SupplierResponse> findAll() {
        return supplierRepository.findByActiveTrueOrderByCompanyNameAsc()
                .stream()
                .map(SupplierResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SupplierResponse findById(UUID id) {
        return SupplierResponse.from(loadSupplier(id));
    }

    @Transactional
    public void deactivate(UUID id) {
        Supplier supplier = loadSupplier(id);
        supplier.deactivate();
        supplierRepository.save(supplier);
    }

    private Supplier loadSupplier(UUID id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor não encontrado: " + id));
    }
}
