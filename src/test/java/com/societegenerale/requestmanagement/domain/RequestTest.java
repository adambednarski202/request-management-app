package com.societegenerale.requestmanagement.domain;

import com.societegenerale.requestmanagement.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestTest {

    @Test
    @DisplayName("Should successfully verify request in CREATED state")
    void shouldVerifyRequestInCreatedState() {
        Long id = 1L;
        Request request = Request.builder()
                .id(id)
                .name("New Laptop")
                .body("MacBook Pro 16")
                .state(RequestState.CREATED)
                .build();

        request.verify();

        assertThat(request.getState()).isEqualTo(RequestState.VERIFIED);
    }

    @ParameterizedTest
    @EnumSource(value = RequestState.class, names = {"VERIFIED", "ACCEPTED", "REJECTED", "DELETED", "PUBLISHED"})
    @DisplayName("Should throw InvalidStateTransitionException when verifying request not in CREATED state")
    void shouldFailVerificationWhenNotInCreatedState(RequestState initialState) {
        Long id = 2L;
        Request request = Request.builder()
                .id(id)
                .name("Server Access")
                .body("Access to prod DB")
                .state(initialState)
                .build();

        assertThatThrownBy(request::verify)
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Cannot transition request with id " + id + " from state " + initialState + " to VERIFIED");
    }

    @Test
    @DisplayName("Should successfully delete request in CREATED state with reason")
    void shouldDeleteRequestInCreatedState() {
        Long id = 3L;
        Request request = Request.builder()
                .id(id)
                .name("Old Server")
                .body("Decommission")
                .state(RequestState.CREATED)
                .build();

        request.delete("No longer needed");

        assertThat(request.getState()).isEqualTo(RequestState.DELETED);
        assertThat(request.getReason()).isEqualTo("No longer needed");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when deleting without reason")
    void shouldFailDeleteWithoutReason() {
        Request request = Request.builder()
                .id(4L)
                .name("Old Server")
                .body("Decommission")
                .state(RequestState.CREATED)
                .build();

        assertThatThrownBy(() -> request.delete("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A reason must be supplied whenever a request is deleted");
    }

    @Test
    @DisplayName("Should successfully reject request in VERIFIED state with reason")
    void shouldRejectRequestInVerifiedState() {
        Request request = Request.builder()
                .id(5L)
                .name("High Memory VM")
                .body("512GB RAM")
                .state(RequestState.VERIFIED)
                .build();

        request.reject("Budget exceeded");

        assertThat(request.getState()).isEqualTo(RequestState.REJECTED);
        assertThat(request.getReason()).isEqualTo("Budget exceeded");
    }

    @Test
    @DisplayName("Should successfully reject request in ACCEPTED state with reason")
    void shouldRejectRequestInAcceptedState() {
        Request request = Request.builder()
                .id(6L)
                .name("High Memory VM")
                .body("512GB RAM")
                .state(RequestState.ACCEPTED)
                .build();

        request.reject("Manager vetoed");

        assertThat(request.getState()).isEqualTo(RequestState.REJECTED);
        assertThat(request.getReason()).isEqualTo("Manager vetoed");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when rejecting without reason")
    void shouldFailRejectWithoutReason() {
        Request request = Request.builder()
                .id(7L)
                .name("High Memory VM")
                .body("512GB RAM")
                .state(RequestState.VERIFIED)
                .build();

        assertThatThrownBy(() -> request.reject(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A reason must be supplied whenever a request is rejected");
    }

    @Test
    @DisplayName("Should successfully accept request in VERIFIED state")
    void shouldAcceptRequestInVerifiedState() {
        Request request = Request.builder()
                .id(8L)
                .name("IntelliJ License")
                .body("Standard developer tooling")
                .state(RequestState.VERIFIED)
                .build();

        request.accept();

        assertThat(request.getState()).isEqualTo(RequestState.ACCEPTED);
    }

    @Test
    @DisplayName("Should successfully publish request in ACCEPTED state and assign publicationId")
    void shouldPublishRequestInAcceptedState() {
        Request request = Request.builder()
                .id(9L)
                .name("Public API Access")
                .body("Access credentials for external partners")
                .state(RequestState.ACCEPTED)
                .build();

        request.publish();

        assertThat(request.getState()).isEqualTo(RequestState.PUBLISHED);
        assertThat(request.getPublicationId()).isEqualTo(9L);
    }

    @Test
    @DisplayName("Should successfully publish request using default numeric generator")
    void shouldPublishRequestUsingDefaultGenerator() {
        Request request = Request.builder()
                .id(10L)
                .name("Internal API")
                .body("Developer endpoint")
                .state(RequestState.ACCEPTED)
                .build();

        request.publish();

        assertThat(request.getState()).isEqualTo(RequestState.PUBLISHED);
        assertThat(request.getPublicationId()).isNotNull();
        assertThat(request.getPublicationId()).isPositive();
    }
}
