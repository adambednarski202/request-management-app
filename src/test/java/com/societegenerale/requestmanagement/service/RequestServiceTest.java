package com.societegenerale.requestmanagement.service;

import com.societegenerale.requestmanagement.domain.Request;
import com.societegenerale.requestmanagement.domain.RequestState;
import com.societegenerale.requestmanagement.dto.CreateRequestCommand;
import com.societegenerale.requestmanagement.dto.PageResponse;
import com.societegenerale.requestmanagement.dto.RequestResponse;
import com.societegenerale.requestmanagement.dto.ModifyRequestCommand;
import com.societegenerale.requestmanagement.exception.InvalidStateTransitionException;
import com.societegenerale.requestmanagement.exception.RequestNotFoundException;
import com.societegenerale.requestmanagement.repository.RequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestServiceTest {

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private AuditLogService auditLogService;

    private RequestService requestService;

    @BeforeEach
    void setUp() {
        requestService = new RequestService(requestRepository, auditLogService);
    }

    @Test
    @DisplayName("Should successfully create a new request in CREATED state and log audit entry")
    void shouldCreateNewRequest() {
        Long id = 1L;
        CreateRequestCommand command = new CreateRequestCommand("Access Request", "Grant access to DB");

        Request savedRequest = Request.builder()
                .id(id)
                .name(command.name())
                .body(command.body())
                .state(RequestState.CREATED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(requestRepository.save(any(Request.class))).thenReturn(savedRequest);

        RequestResponse response = requestService.create(command);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(id);
        assertThat(response.name()).isEqualTo("Access Request");
        assertThat(response.body()).isEqualTo("Grant access to DB");
        assertThat(response.state()).isEqualTo(RequestState.CREATED);

        verify(auditLogService).record(id, null, RequestState.CREATED, null);
    }

    @Test
    @DisplayName("Should return RequestResponse when getById finds an existing request")
    void shouldGetRequestById() {
        Long id = 2L;
        Request request = Request.builder()
                .id(id)
                .name("VPN Access")
                .body("Access for remote work")
                .state(RequestState.CREATED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(requestRepository.findById(id)).thenReturn(Optional.of(request));

        RequestResponse response = requestService.getById(id);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(id);
        assertThat(response.name()).isEqualTo("VPN Access");
        assertThat(response.state()).isEqualTo(RequestState.CREATED);
    }

    @Test
    @DisplayName("Should throw RequestNotFoundException when getById does not find request")
    void shouldThrowRequestNotFoundExceptionWhenGetByIdNotFound() {
        Long id = 3L;
        when(requestRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requestService.getById(id))
                .isInstanceOf(RequestNotFoundException.class)
                .hasMessage("Request not found with id: " + id);
    }

    @Test
    @DisplayName("Should successfully verify request transitioning from CREATED to VERIFIED")
    void shouldVerifyRequestSuccessfully() {
        Long id = 4L;
        Request existing = Request.builder()
                .id(id)
                .name("Cloud Account")
                .body("AWS Dev Sandbox")
                .state(RequestState.CREATED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(requestRepository.findById(id)).thenReturn(Optional.of(existing));
        when(requestRepository.save(any(Request.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RequestResponse response = requestService.verify(id);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(id);
        assertThat(response.state()).isEqualTo(RequestState.VERIFIED);

        verify(auditLogService).record(id, RequestState.CREATED, RequestState.VERIFIED, null);
    }

    @Test
    @DisplayName("Should throw RequestNotFoundException when verifying non-existent request")
    void shouldThrowRequestNotFoundExceptionWhenVerifyingNonExistent() {
        Long id = 5L;
        when(requestRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requestService.verify(id))
                .isInstanceOf(RequestNotFoundException.class)
                .hasMessage("Request not found with id: " + id);

        verify(requestRepository, never()).save(any(Request.class));
    }

    @Test
    @DisplayName("Should successfully modify existing request and log audit entry")
    void shouldModifyExistingRequest() {
        Long id = 6L;
        ModifyRequestCommand command = new ModifyRequestCommand("Modify access to DB");

        Request existing = Request.builder()
                .id(id)
                .body(command.body())
                .state(RequestState.VERIFIED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(requestRepository.findById(id)).thenReturn(Optional.of(existing));

        when(requestRepository.save(any(Request.class))).thenReturn(existing);

        RequestResponse response = requestService.modify(id, command);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(id);
        assertThat(response.body()).isEqualTo("Modify access to DB");
        assertThat(response.state()).isEqualTo(RequestState.VERIFIED);

        verify(auditLogService).record(id, RequestState.VERIFIED, RequestState.VERIFIED, null);
    }

    @Test
    @DisplayName("Should throw RequestNotFoundException when modifying non-existent request")
    void shouldThrowRequestNotFoundExceptionWhenUpdatingNonExistent() {
        Long id = 7L;
        when(requestRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requestService.modify(id, null))
                .isInstanceOf(RequestNotFoundException.class)
                .hasMessage("Request not found with id: " + id);

        verify(requestRepository, never()).save(any(Request.class));
    }

    @ParameterizedTest
    @EnumSource(value = RequestState.class, names = {"ACCEPTED", "REJECTED", "DELETED", "PUBLISHED"})
    @DisplayName("Should throw InvalidStateTransitionException when modifying request in invalid state")
    void shouldThrowInvalidStateTransitionExceptionWhenModifyingInvalidState(RequestState nonCreatedState) {
        Long id = 8L;
        ModifyRequestCommand command = new ModifyRequestCommand("Modify access to DB");
        Request existing = Request.builder()
                .id(id)
                .name("Software License")
                .body(command.body())
                .state(nonCreatedState)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(requestRepository.findById(id)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> requestService.modify(id, command))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Cannot modify request with id " + id + ": A state must be either VERIFIED or CREATED to modify the request");

        verify(requestRepository, never()).save(any(Request.class));
    }

    @ParameterizedTest
    @EnumSource(value = RequestState.class, names = {"VERIFIED", "ACCEPTED", "REJECTED", "DELETED", "PUBLISHED"})
    @DisplayName("Should throw InvalidStateTransitionException when verifying request in invalid state")
    void shouldThrowInvalidStateTransitionExceptionWhenVerifyingInvalidState(RequestState nonCreatedState) {
        Long id = 9L;
        Request existing = Request.builder()
                .id(id)
                .name("Software License")
                .body("License key renewal")
                .state(nonCreatedState)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(requestRepository.findById(id)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> requestService.verify(id))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Cannot transition request with id " + id + " from state " + nonCreatedState + " to VERIFIED");

        verify(requestRepository, never()).save(any(Request.class));
    }

    @Test
    @DisplayName("Should successfully delete request transitioning from CREATED to DELETED with reason")
    void shouldDeleteRequestSuccessfully() {
        Long id = 10L;
        Request existing = Request.builder()
                .id(id)
                .name("Old Server")
                .body("Decommission")
                .state(RequestState.CREATED)
                .build();

        when(requestRepository.findById(id)).thenReturn(Optional.of(existing));
        when(requestRepository.save(any(Request.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RequestResponse response = requestService.delete(id, "Cancelled by user");

        assertThat(response.state()).isEqualTo(RequestState.DELETED);
        assertThat(response.reason()).isEqualTo("Cancelled by user");

        verify(auditLogService).record(id, RequestState.CREATED, RequestState.DELETED, "Cancelled by user");
    }

    @Test
    @DisplayName("Should successfully reject request transitioning from VERIFIED to REJECTED with reason")
    void shouldRejectRequestSuccessfully() {
        Long id = 11L;
        Request existing = Request.builder()
                .id(id)
                .name("High Memory VM")
                .body("512GB RAM")
                .state(RequestState.VERIFIED)
                .build();

        when(requestRepository.findById(id)).thenReturn(Optional.of(existing));
        when(requestRepository.save(any(Request.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RequestResponse response = requestService.reject(id, "Quota exceeded");

        assertThat(response.state()).isEqualTo(RequestState.REJECTED);
        assertThat(response.reason()).isEqualTo("Quota exceeded");

        verify(auditLogService).record(id, RequestState.VERIFIED, RequestState.REJECTED, "Quota exceeded");
    }

    @Test
    @DisplayName("Should successfully accept request transitioning from VERIFIED to ACCEPTED")
    void shouldAcceptRequestSuccessfully() {
        Long id = 12L;
        Request existing = Request.builder()
                .id(id)
                .name("Standard Laptop")
                .body("ThinkPad T14")
                .state(RequestState.VERIFIED)
                .build();

        when(requestRepository.findById(id)).thenReturn(Optional.of(existing));
        when(requestRepository.save(any(Request.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RequestResponse response = requestService.accept(id);

        assertThat(response.state()).isEqualTo(RequestState.ACCEPTED);

        verify(auditLogService).record(id, RequestState.VERIFIED, RequestState.ACCEPTED, null);
    }

    @Test
    @DisplayName("Should successfully publish request transitioning from ACCEPTED to PUBLISHED and assign publicationId")
    void shouldPublishRequestSuccessfully() {
        Long id = 13L;
        Request existing = Request.builder()
                .id(id)
                .name("Standard Laptop")
                .body("ThinkPad T14")
                .state(RequestState.ACCEPTED)
                .build();

        when(requestRepository.findById(id)).thenReturn(Optional.of(existing));
        when(requestRepository.save(any(Request.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RequestResponse response = requestService.publish(id);

        assertThat(response.state()).isEqualTo(RequestState.PUBLISHED);
        assertThat(response.publicationId()).isEqualTo(id);

        verify(auditLogService).record(id, RequestState.ACCEPTED, RequestState.PUBLISHED, null, id);
    }

    @Test
    @DisplayName("Should find requests with pagination and filters")
    void shouldFindAllWithPagination() {
        Request request = Request.builder()
                .id(14L)
                .name("Database Access")
                .body("Access to PostgreSQL DB")
                .state(RequestState.CREATED)
                .build();

        Page<Request> page = new PageImpl<>(List.of(request), PageRequest.of(0, 10), 1);
        when(requestRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<RequestResponse> result = requestService.findAll("Database", RequestState.CREATED, PageRequest.of(0, 10));

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content().get(0).name()).isEqualTo("Database Access");
    }
}
