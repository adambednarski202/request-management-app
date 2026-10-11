package com.societegenerale.requestmanagement.repository;

import com.societegenerale.requestmanagement.domain.AuditLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogEntryRepository extends JpaRepository<AuditLogEntry, Long> {
    List<AuditLogEntry> findByRequestIdOrderByTimestampAsc(Long requestId);
}
