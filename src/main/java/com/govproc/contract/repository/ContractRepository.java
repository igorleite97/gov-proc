package com.govproc.contract.repository;

import com.govproc.contract.domain.Contract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ContractRepository extends JpaRepository<Contract, UUID> {

    Optional<Contract> findByProcessId(UUID processId);
    boolean existsByProcessId(UUID processId);
}
