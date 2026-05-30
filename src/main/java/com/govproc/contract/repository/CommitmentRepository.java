package com.govproc.contract.repository;

import com.govproc.contract.domain.Commitment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommitmentRepository extends JpaRepository<Commitment, UUID> {

    List<Commitment> findByContractIdOrderByIssueDateAsc(UUID contractId);
}
