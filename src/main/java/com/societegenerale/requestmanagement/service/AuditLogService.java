package com.societegenerale.requestmanagement.service;

import com.societegenerale.requestmanagement.domain.AuditLogEntry;
import com.societegenerale.requestmanagement.domain.RequestState;
import com.societegenerale.requestmanagement.repository.AuditLogEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogEntryRepository auditLogEntryRepository;

    public AuditLogService(AuditLogEntryRepository auditLogEntryRepository) {
        this.auditLogEntryRepository = auditLogEntryRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Long requestId, RequestState oldState, RequestState newState, String reason, Long publicationId) {
        log.info("Recording audit log for requestId: {}, transition: {} -> {}, reason: {}, publicationId: {}",
                requestId, oldState, newState, reason, publicationId);

        AuditLogEntry entry = AuditLogEntry.builder()
                .requestId(requestId)
                .oldState(oldState)
                .newState(newState)
                .reason(reason)
                .publicationId(publicationId)
                .timestamp(Instant.now())
                .build();

        auditLogEntryRepository.save(entry);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Long requestId, RequestState oldState, RequestState newState, String reason) {
        record(requestId, oldState, newState, reason, null);
    }

    @Transactional(readOnly = true)
    public List<AuditLogEntry> getAuditLogsForRequest(Long requestId) {
        return auditLogEntryRepository.findByRequestIdOrderByTimestampAsc(requestId);
    }
}
