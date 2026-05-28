package com.govproc.process.repository;

import com.govproc.process.domain.ProcurementProcess;
import com.govproc.process.domain.ProcessStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProcessRepository extends JpaRepository<ProcurementProcess, UUID> {

    boolean existsByProcessNumberAndUasg(String processNumber, String uasg);

    List<ProcurementProcess> findByStatus(ProcessStatus status);

    List<ProcurementProcess> findAllByOrderByCreatedAtDesc();
}
