package com.societegenerale.requestmanagement.controller;

import tools.jackson.databind.ObjectMapper;
import com.societegenerale.requestmanagement.domain.Request;
import com.societegenerale.requestmanagement.domain.RequestState;
import com.societegenerale.requestmanagement.dto.CreateRequestCommand;
import com.societegenerale.requestmanagement.repository.RequestRepository;
import com.societegenerale.requestmanagement.domain.AuditLogEntry;
import com.societegenerale.requestmanagement.dto.StateTransitionCommand;
import com.societegenerale.requestmanagement.repository.AuditLogEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class RequestControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private AuditLogEntryRepository auditLogEntryRepository;

    @BeforeEach
    void setUp() {
        auditLogEntryRepository.deleteAll();
        requestRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/requests - Should create request and persist in H2 database")
    void shouldCreateRequestAndPersistInDatabase() throws Exception {
        CreateRequestCommand command = new CreateRequestCommand("Software License", "JetBrains IntelliJ IDEA license");

        mockMvc.perform(post("/api/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Software License"))
                .andExpect(jsonPath("$.body").value("JetBrains IntelliJ IDEA license"))
                .andExpect(jsonPath("$.state").value("CREATED"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        List<Request> allRequests = requestRepository.findAll();
        assertThat(allRequests).hasSize(1);
        Request persisted = allRequests.get(0);
        assertThat(persisted.getName()).isEqualTo("Software License");
        assertThat(persisted.getBody()).isEqualTo("JetBrains IntelliJ IDEA license");
        assertThat(persisted.getState()).isEqualTo(RequestState.CREATED);
    }

    @Test
    @DisplayName("POST /api/requests - Should return 400 Bad Request when name is blank")
    void shouldReturnBadRequestWhenNameIsBlank() throws Exception {
        CreateRequestCommand command = new CreateRequestCommand("", "Valid body");

        mockMvc.perform(post("/api/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.invalidParams.name").value("Name is mandatory"));

        assertThat(requestRepository.count()).isZero();
    }

    @Test
    @DisplayName("POST /api/requests - Should return 400 Bad Request when body is blank")
    void shouldReturnBadRequestWhenBodyIsBlank() throws Exception {
        CreateRequestCommand command = new CreateRequestCommand("Valid name", "");

        mockMvc.perform(post("/api/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.invalidParams.body").value("Body is mandatory"));

        assertThat(requestRepository.count()).isZero();
    }

    @Test
    @DisplayName("GET /api/requests/{id} - Should return 200 OK and request data when request exists")
    void shouldGetRequestById() throws Exception {
        Request request = requestRepository.save(Request.builder()
                .name("Database Access")
                .body("Access to PostgreSQL DB")
                .state(RequestState.CREATED)
                .build());

        mockMvc.perform(get("/api/requests/" + request.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(request.getId()))
                .andExpect(jsonPath("$.name").value("Database Access"))
                .andExpect(jsonPath("$.body").value("Access to PostgreSQL DB"))
                .andExpect(jsonPath("$.state").value("CREATED"));
    }

    @Test
    @DisplayName("GET /api/requests/{id} - Should return 404 Not Found when request does not exist")
    void shouldReturn404WhenGetRequestNotFound() throws Exception {
        Long nonExistentId = 999999L;
        mockMvc.perform(get("/api/requests/" + nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Request Not Found"))
                .andExpect(jsonPath("$.detail").value("Request not found with id: " + nonExistentId));
    }

    @Test
    @DisplayName("POST /api/requests/{id}/verify - Should verify request in CREATED state and return 200 OK")
    void shouldVerifyRequestInCreatedState() throws Exception {
        Request request = requestRepository.save(Request.builder()
                .name("New Hardware")
                .body("Requesting 32GB RAM upgrade")
                .state(RequestState.CREATED)
                .build());

        mockMvc.perform(post("/api/requests/" + request.getId() + "/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(request.getId()))
                .andExpect(jsonPath("$.name").value("New Hardware"))
                .andExpect(jsonPath("$.body").value("Requesting 32GB RAM upgrade"))
                .andExpect(jsonPath("$.state").value("VERIFIED"));

        Request updated = requestRepository.findById(request.getId()).orElseThrow();
        assertThat(updated.getState()).isEqualTo(RequestState.VERIFIED);
    }

    @Test
    @DisplayName("PUT /api/requests/{id}/verify - Should also succeed via PUT method")
    void shouldVerifyRequestViaPut() throws Exception {
        Request request = requestRepository.save(Request.builder()
                .name("Cloud Provisioning")
                .body("Spin up ECS cluster")
                .state(RequestState.CREATED)
                .build());

        mockMvc.perform(put("/api/requests/" + request.getId() + "/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(request.getId()))
                .andExpect(jsonPath("$.state").value("VERIFIED"));

        Request updated = requestRepository.findById(request.getId()).orElseThrow();
        assertThat(updated.getState()).isEqualTo(RequestState.VERIFIED);
    }

    @Test
    @DisplayName("POST /api/requests/{id}/verify - Should return 404 Not Found when request does not exist")
    void shouldReturn404WhenVerifyingNonExistentRequest() throws Exception {
        Long nonExistentId = 999999L;
        mockMvc.perform(post("/api/requests/" + nonExistentId + "/verify"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Request Not Found"))
                .andExpect(jsonPath("$.detail").value("Request not found with id: " + nonExistentId));
    }

    @Test
    @DisplayName("POST /api/requests/{id}/verify - Should return 409 Conflict when request is already VERIFIED")
    void shouldReturn409WhenRequestAlreadyVerified() throws Exception {
        Request request = requestRepository.save(Request.builder()
                .name("Network Access")
                .body("VPN configuration")
                .state(RequestState.VERIFIED)
                .build());

        mockMvc.perform(post("/api/requests/" + request.getId() + "/verify"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Invalid State Transition"))
                .andExpect(jsonPath("$.detail").value("Cannot transition request with id " + request.getId() + " from state VERIFIED to VERIFIED"))
                .andExpect(jsonPath("$.currentState").value("VERIFIED"))
                .andExpect(jsonPath("$.targetState").value("VERIFIED"));

        Request unchanged = requestRepository.findById(request.getId()).orElseThrow();
        assertThat(unchanged.getState()).isEqualTo(RequestState.VERIFIED);
    }

    @Test
    @DisplayName("POST /api/requests/{id}/verify - Should return 409 Conflict when request is DELETED")
    void shouldReturn409WhenRequestIsDeleted() throws Exception {
        Request request = requestRepository.save(Request.builder()
                .name("Discarded Request")
                .body("Obsolete request body")
                .state(RequestState.DELETED)
                .build());

        mockMvc.perform(post("/api/requests/" + request.getId() + "/verify"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Invalid State Transition"))
                .andExpect(jsonPath("$.detail").value("Cannot transition request with id " + request.getId() + " from state DELETED to VERIFIED"));

        Request unchanged = requestRepository.findById(request.getId()).orElseThrow();
        assertThat(unchanged.getState()).isEqualTo(RequestState.DELETED);
    }

    @Test
    @DisplayName("POST /api/requests/{id}/delete - Should delete request with reason and persist in DB & audit log")
    void shouldDeleteRequestWithReason() throws Exception {
        Request request = requestRepository.save(Request.builder()
                .name("Unneeded VM")
                .body("Decommission VM")
                .state(RequestState.CREATED)
                .build());

        StateTransitionCommand command = new StateTransitionCommand("Project cancelled");

        mockMvc.perform(post("/api/requests/" + request.getId() + "/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(request.getId()))
                .andExpect(jsonPath("$.state").value("DELETED"))
                .andExpect(jsonPath("$.reason").value("Project cancelled"));

        Request updated = requestRepository.findById(request.getId()).orElseThrow();
        assertThat(updated.getState()).isEqualTo(RequestState.DELETED);
        assertThat(updated.getReason()).isEqualTo("Project cancelled");

        List<AuditLogEntry> logs = auditLogEntryRepository.findByRequestIdOrderByTimestampAsc(request.getId());
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getOldState()).isEqualTo(RequestState.CREATED);
        assertThat(logs.get(0).getNewState()).isEqualTo(RequestState.DELETED);
        assertThat(logs.get(0).getReason()).isEqualTo("Project cancelled");
    }

    @Test
    @DisplayName("POST /api/requests/{id}/delete - Should return 400 Bad Request when reason is missing")
    void shouldFailToDeleteWithoutReason() throws Exception {
        Request request = requestRepository.save(Request.builder()
                .name("Unneeded VM")
                .body("Decommission VM")
                .state(RequestState.CREATED)
                .build());

        mockMvc.perform(post("/api/requests/" + request.getId() + "/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StateTransitionCommand(""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("A reason must be supplied whenever a request is deleted"));
    }

    @Test
    @DisplayName("POST /api/requests/{id}/reject - Should reject verified request with reason and persist in DB & audit log")
    void shouldRejectVerifiedRequestWithReason() throws Exception {
        Request request = requestRepository.save(Request.builder()
                .name("Cluster Upgrade")
                .body("Upgrade to 128 nodes")
                .state(RequestState.VERIFIED)
                .build());

        StateTransitionCommand command = new StateTransitionCommand("Budget constraint");

        mockMvc.perform(post("/api/requests/" + request.getId() + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(request.getId()))
                .andExpect(jsonPath("$.state").value("REJECTED"))
                .andExpect(jsonPath("$.reason").value("Budget constraint"));

        Request updated = requestRepository.findById(request.getId()).orElseThrow();
        assertThat(updated.getState()).isEqualTo(RequestState.REJECTED);
        assertThat(updated.getReason()).isEqualTo("Budget constraint");

        List<AuditLogEntry> logs = auditLogEntryRepository.findByRequestIdOrderByTimestampAsc(request.getId());
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getOldState()).isEqualTo(RequestState.VERIFIED);
        assertThat(logs.get(0).getNewState()).isEqualTo(RequestState.REJECTED);
        assertThat(logs.get(0).getReason()).isEqualTo("Budget constraint");
    }

    @Test
    @DisplayName("POST /api/requests/{id}/reject - Should return 400 Bad Request when reject reason is missing")
    void shouldFailToRejectWithoutReason() throws Exception {
        Request request = requestRepository.save(Request.builder()
                .name("Cluster Upgrade")
                .body("Upgrade to 128 nodes")
                .state(RequestState.VERIFIED)
                .build());

        mockMvc.perform(post("/api/requests/" + request.getId() + "/reject"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("A reason must be supplied whenever a request is rejected"));
    }

    @Test
    @DisplayName("POST /api/requests/{id}/accept - Should accept verified request and persist in DB & audit log")
    void shouldAcceptVerifiedRequest() throws Exception {
        Request request = requestRepository.save(Request.builder()
                .name("Dev Machine")
                .body("Apple Silicon workstation")
                .state(RequestState.VERIFIED)
                .build());

        mockMvc.perform(post("/api/requests/" + request.getId() + "/accept"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(request.getId()))
                .andExpect(jsonPath("$.state").value("ACCEPTED"));

        Request updated = requestRepository.findById(request.getId()).orElseThrow();
        assertThat(updated.getState()).isEqualTo(RequestState.ACCEPTED);

        List<AuditLogEntry> logs = auditLogEntryRepository.findByRequestIdOrderByTimestampAsc(request.getId());
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getOldState()).isEqualTo(RequestState.VERIFIED);
        assertThat(logs.get(0).getNewState()).isEqualTo(RequestState.ACCEPTED);
    }

    @Test
    @DisplayName("POST /api/requests/{id}/publish - Should publish accepted request, assign publicationId, and log audit entry")
    void shouldPublishAcceptedRequest() throws Exception {
        Request request = requestRepository.save(Request.builder()
                .name("Production Service")
                .body("Deploy microservice")
                .state(RequestState.ACCEPTED)
                .build());

        mockMvc.perform(post("/api/requests/" + request.getId() + "/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(request.getId()))
                .andExpect(jsonPath("$.state").value("PUBLISHED"))
                .andExpect(jsonPath("$.publicationId").isNumber());

        Request updated = requestRepository.findById(request.getId()).orElseThrow();
        assertThat(updated.getState()).isEqualTo(RequestState.PUBLISHED);
        assertThat(updated.getPublicationId()).isNotNull();
        assertThat(updated.getPublicationId()).isGreaterThanOrEqualTo(updated.getId());

        List<AuditLogEntry> logs = auditLogEntryRepository.findByRequestIdOrderByTimestampAsc(request.getId());
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getOldState()).isEqualTo(RequestState.ACCEPTED);
        assertThat(logs.get(0).getNewState()).isEqualTo(RequestState.PUBLISHED);
        assertThat(logs.get(0).getPublicationId()).isEqualTo(updated.getPublicationId());
    }

    @Test
    @DisplayName("GET /api/requests - Should list requests with pagination and filtering by name and state")
    void shouldBrowseRequestsWithPaginationAndFiltering() throws Exception {
        requestRepository.save(Request.builder().name("Alpha Request").body("Body A").state(RequestState.CREATED).build());
        requestRepository.save(Request.builder().name("Beta Request").body("Body B").state(RequestState.VERIFIED).build());
        requestRepository.save(Request.builder().name("Alpha Extra").body("Body C").state(RequestState.VERIFIED).build());

        // Test filtering by name
        mockMvc.perform(get("/api/requests")
                        .param("name", "Alpha")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Alpha Request"))
                .andExpect(jsonPath("$.content[1].name").value("Alpha Extra"));

        // Test filtering by state
        mockMvc.perform(get("/api/requests")
                        .param("state", "VERIFIED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].state").value("VERIFIED"))
                .andExpect(jsonPath("$.content[1].state").value("VERIFIED"));

        // Test filtering by name AND state
        mockMvc.perform(get("/api/requests")
                        .param("name", "Alpha")
                        .param("state", "VERIFIED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Alpha Extra"))
                .andExpect(jsonPath("$.content[0].state").value("VERIFIED"));

        // Test default pagination (all 3 returned)
        mockMvc.perform(get("/api/requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.pageNumber").value(0));
    }
}
