package com.govproc.contract.repository;

import com.govproc.contract.domain.Addendum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AddendumRepository extends JpaRepository<Addendum, UUID> {

    List<Addendum> findByContractIdOrderBySignedAtAsc(UUID contractId);
}
