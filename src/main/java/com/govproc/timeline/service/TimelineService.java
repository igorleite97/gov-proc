package com.govproc.timeline.service;

import com.govproc.timeline.domain.ProcessEventType;
import com.govproc.timeline.domain.ProcessTimelineEvent;
import com.govproc.timeline.dto.TimelineEventResponse;
import com.govproc.timeline.repository.TimelineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TimelineService {

    private final TimelineRepository timelineRepository;

    public TimelineService(TimelineRepository timelineRepository) {
        this.timelineRepository = timelineRepository;
    }

    @Transactional
    public void record(UUID processId, ProcessEventType eventType,
                       String description, UUID performedBy) {
        ProcessTimelineEvent event = new ProcessTimelineEvent(
                processId, eventType, description, performedBy);
        timelineRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<TimelineEventResponse> findByProcessId(UUID processId) {
        return timelineRepository
                .findByProcessIdOrderByCreatedAtAsc(processId)
                .stream()
                .map(TimelineEventResponse::from)
                .toList();
    }
}
