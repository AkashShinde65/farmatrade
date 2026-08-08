package com.farmatrade.auth.dto;

import com.farmatrade.auth.entity.SecurityAuditEvent;
import java.time.Instant;

public record AuditEventResponse(
        Long id,
        String eventType,
        Long actingUserId,
        Long targetUserId,
        String description,
        Instant createdAt
) {
    public static AuditEventResponse from(SecurityAuditEvent event) {
        return new AuditEventResponse(
                event.getId(),
                event.getEventType(),
                event.getActingUserId(),
                event.getTargetUserId(),
                event.getDescription(),
                event.getCreatedAt()
        );
    }
}
