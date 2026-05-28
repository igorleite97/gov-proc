package com.govproc.timeline.repository;

import com.govproc.timeline.domain.ProcessTimelineEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TimelineRepository extends JpaRepository<ProcessTimelineEvent, UUID> {

    List<ProcessTimelineEvent> findByProcessIdOrderByCreatedAtAsc(UUID processId);
}
