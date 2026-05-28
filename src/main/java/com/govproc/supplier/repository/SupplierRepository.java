package com.govproc.supplier.repository;

import com.govproc.supplier.domain.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SupplierRepository extends JpaRepository<Supplier, UUID> {

    boolean existsByDocument(String document);

    List<Supplier> findByActiveTrueOrderByCompanyNameAsc();
}
