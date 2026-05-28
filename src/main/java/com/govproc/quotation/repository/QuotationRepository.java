package com.govproc.quotation.repository;

import com.govproc.quotation.domain.Quotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuotationRepository extends JpaRepository<Quotation, UUID> {

    List<Quotation> findByProcessIdOrderByTotalCostAsc(UUID processId);

    Optional<Quotation> findByIdAndProcessId(UUID id, UUID processId);

    boolean existsByProcessIdAndSelectedTrue(UUID processId);

    /**
     * Desseleciona todas as cotacoes do processo em um unico UPDATE atomico.
     * Chamado antes de selecionar uma nova cotacao para garantir
     * que apenas uma fique com selected=true por processo.
     */
    @Modifying
    @Query("UPDATE Quotation q SET q.selected = false WHERE q.processId = :processId")
    void clearSelectedByProcess(@Param("processId") UUID processId);
}
