package com.farmatrade.auth.service;

import com.farmatrade.auth.dto.AuditEventResponse;
import com.farmatrade.auth.entity.SecurityAuditEvent;
import com.farmatrade.auth.repository.SecurityAuditEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {
    private final SecurityAuditEventRepository repository;

    public AuditService(SecurityAuditEventRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String eventType, Long actingUserId, Long targetUserId, String description) {
        SecurityAuditEvent event = new SecurityAuditEvent();
        event.setEventType(eventType);
        event.setActingUserId(actingUserId);
        event.setTargetUserId(targetUserId);
        event.setDescription(description);
        repository.save(event);
    }

    @Transactional(readOnly = true)
    public Page<AuditEventResponse> list(Pageable pageable) {
        return repository.findAll(pageable).map(AuditEventResponse::from);
    }
}
