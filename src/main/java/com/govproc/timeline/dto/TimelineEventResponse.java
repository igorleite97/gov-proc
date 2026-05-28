package com.govproc.timeline.dto;

import com.govproc.timeline.domain.ProcessEventType;
import com.govproc.timeline.domain.ProcessTimelineEvent;

import java.time.Instant;
import java.util.UUID;

public record TimelineEventResponse(
        UUID id,
        UUID processId,
        ProcessEventType eventType,
        String description,
        UUID performedBy,
        Instant occurredAt
) {

    public static TimelineEventResponse from(ProcessTimelineEvent e) {
        return new TimelineEventResponse(
                e.getId(),
                e.getProcessId(),
                e.getEventType(),
                e.getDescription(),
                e.getPerformedBy(),
                e.getCreatedAt()
        );
    }
}
