package com.farmatrade.auth.repository;

import com.farmatrade.auth.entity.SecurityAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SecurityAuditEventRepository extends JpaRepository<SecurityAuditEvent, Long> {
}
