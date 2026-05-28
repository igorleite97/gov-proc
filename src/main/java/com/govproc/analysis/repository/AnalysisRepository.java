package com.govproc.analysis.repository;

import com.govproc.analysis.domain.ProcessAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AnalysisRepository extends JpaRepository<ProcessAnalysis, UUID> {

    Optional<ProcessAnalysis> findByProcessId(UUID processId);

    boolean existsByProcessId(UUID processId);
}
