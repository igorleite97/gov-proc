package com.govproc.postbid.repository;

import com.govproc.postbid.domain.PostBid;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PostBidRepository extends JpaRepository<PostBid, UUID> {

    Optional<PostBid> findByProcessId(UUID processId);
    boolean existsByProcessId(UUID processId);
}
