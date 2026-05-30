package com.govproc.contract.repository;

import com.govproc.contract.domain.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    List<Invoice> findByContractIdOrderByIssuedAtAsc(UUID contractId);
}
