package com.govproc.dispute.repository;

import com.govproc.dispute.domain.Dispute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DisputeRepository extends JpaRepository<Dispute, UUID> {

    Optional<Dispute> findByProcessId(UUID processId);
    boolean existsByProcessId(UUID processId);
}
