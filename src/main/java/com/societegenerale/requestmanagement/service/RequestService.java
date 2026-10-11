package com.societegenerale.requestmanagement.service;

import com.societegenerale.requestmanagement.domain.Request;
import com.societegenerale.requestmanagement.domain.RequestState;
import com.societegenerale.requestmanagement.dto.CreateRequestCommand;
import com.societegenerale.requestmanagement.dto.PageResponse;
import com.societegenerale.requestmanagement.dto.RequestResponse;
import com.societegenerale.requestmanagement.dto.ModifyRequestCommand;
import com.societegenerale.requestmanagement.exception.RequestNotFoundException;
import com.societegenerale.requestmanagement.repository.RequestRepository;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class RequestService {

    private static final Logger log = LoggerFactory.getLogger(RequestService.class);

    private final RequestRepository requestRepository;
    private final AuditLogService auditLogService;

    public RequestService(RequestRepository requestRepository, AuditLogService auditLogService) {
        this.requestRepository = requestRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public RequestResponse create(CreateRequestCommand command) {
        log.info("Creating request with name: {}", command.name());

        Request request = Request.builder()
                .name(command.name())
                .body(command.body())
                .state(RequestState.CREATED)
                .build();

        Request savedRequest = requestRepository.save(request);
        auditLogService.record(savedRequest.getId(), null, RequestState.CREATED, null);
        log.info("Request created successfully with id: {}", savedRequest.getId());

        return RequestResponse.fromEntity(savedRequest);
    }

    @Transactional(readOnly = true)
    public RequestResponse getById(Long id) {
        log.info("Fetching request with id: {}", id);
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new RequestNotFoundException(id));
        return RequestResponse.fromEntity(request);
    }

    @Transactional
    public RequestResponse modify(Long id, ModifyRequestCommand command) {
        log.info("Updating request: {} body", id);
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new RequestNotFoundException(id));
        request.modify();
        Request savedRequest = requestRepository.save(request);
        auditLogService.record(savedRequest.getId(), request.getState(), request.getState(), null);
        log.info("Request with id: {} body successfully updated", savedRequest.getId());

        return RequestResponse.fromEntity(savedRequest);
    }

    @Transactional
    public RequestResponse verify(Long id) {
        log.info("Transitioning request with id: {} to VERIFIED", id);
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new RequestNotFoundException(id));

        RequestState oldState = request.getState();
        request.verify();
        Request savedRequest = requestRepository.save(request);
        auditLogService.record(savedRequest.getId(), oldState, RequestState.VERIFIED, null);
        log.info("Request with id: {} successfully transitioned to state: {}", savedRequest.getId(), savedRequest.getState());

        return RequestResponse.fromEntity(savedRequest);
    }

    @Transactional
    public RequestResponse delete(Long id, String reason) {
        log.info("Transitioning request with id: {} to DELETED with reason: {}", id, reason);
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new RequestNotFoundException(id));

        RequestState oldState = request.getState();
        request.delete(reason);
        Request savedRequest = requestRepository.save(request);
        auditLogService.record(savedRequest.getId(), oldState, RequestState.DELETED, reason);
        log.info("Request with id: {} successfully transitioned to state: {}", savedRequest.getId(), savedRequest.getState());

        return RequestResponse.fromEntity(savedRequest);
    }

    @Transactional
    public RequestResponse reject(Long id, String reason) {
        log.info("Transitioning request with id: {} to REJECTED with reason: {}", id, reason);
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new RequestNotFoundException(id));

        RequestState oldState = request.getState();
        request.reject(reason);
        Request savedRequest = requestRepository.save(request);
        auditLogService.record(savedRequest.getId(), oldState, RequestState.REJECTED, reason);
        log.info("Request with id: {} successfully transitioned to state: {}", savedRequest.getId(), savedRequest.getState());

        return RequestResponse.fromEntity(savedRequest);
    }

    @Transactional
    public RequestResponse accept(Long id) {
        log.info("Transitioning request with id: {} to ACCEPTED", id);
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new RequestNotFoundException(id));

        RequestState oldState = request.getState();
        request.accept();
        Request savedRequest = requestRepository.save(request);
        auditLogService.record(savedRequest.getId(), oldState, RequestState.ACCEPTED, null);
        log.info("Request with id: {} successfully transitioned to state: {}", savedRequest.getId(), savedRequest.getState());

        return RequestResponse.fromEntity(savedRequest);
    }

    @Transactional
    public RequestResponse publish(Long id) {
        log.info("Transitioning request with id: {} to PUBLISHED", id);
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new RequestNotFoundException(id));

        RequestState oldState = request.getState();
        request.publish();
        Request savedRequest = requestRepository.save(request);
        auditLogService.record(savedRequest.getId(), oldState, RequestState.PUBLISHED, null, savedRequest.getPublicationId());
        log.info("Request with id: {} successfully transitioned to state: {}, publicationId: {}",
                savedRequest.getId(), savedRequest.getState(), savedRequest.getPublicationId());

        return RequestResponse.fromEntity(savedRequest);
    }

    @Transactional(readOnly = true)
    public PageResponse<RequestResponse> findAll(String name, RequestState state, Pageable pageable) {
        log.info("Browsing requests with name: {}, state: {}, pageable: {}", name, state, pageable);

        Specification<Request> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (name != null && !name.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.trim().toLowerCase() + "%"));
            }
            if (state != null) {
                predicates.add(cb.equal(root.get("state"), state));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Request> page = requestRepository.findAll(spec, pageable);
        return PageResponse.fromPage(page.map(RequestResponse::fromEntity));
    }
}
