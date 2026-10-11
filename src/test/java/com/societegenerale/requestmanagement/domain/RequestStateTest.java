package com.societegenerale.requestmanagement.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class RequestStateTest {

    @Test
    @DisplayName("CREATED should allow transition to VERIFIED and DELETED only")
    void createdTransitions() {
        assertThat(RequestState.CREATED.canTransitionTo(RequestState.VERIFIED)).isTrue();
        assertThat(RequestState.CREATED.canTransitionTo(RequestState.DELETED)).isTrue();

        assertThat(RequestState.CREATED.canTransitionTo(RequestState.CREATED)).isFalse();
        assertThat(RequestState.CREATED.canTransitionTo(RequestState.ACCEPTED)).isFalse();
        assertThat(RequestState.CREATED.canTransitionTo(RequestState.REJECTED)).isFalse();
        assertThat(RequestState.CREATED.canTransitionTo(RequestState.PUBLISHED)).isFalse();
        assertThat(RequestState.CREATED.canTransitionTo(null)).isFalse();
    }

    @Test
    @DisplayName("VERIFIED should allow transition to ACCEPTED and REJECTED only")
    void verifiedTransitions() {
        assertThat(RequestState.VERIFIED.canTransitionTo(RequestState.ACCEPTED)).isTrue();
        assertThat(RequestState.VERIFIED.canTransitionTo(RequestState.REJECTED)).isTrue();

        assertThat(RequestState.VERIFIED.canTransitionTo(RequestState.VERIFIED)).isFalse();
        assertThat(RequestState.VERIFIED.canTransitionTo(RequestState.CREATED)).isFalse();
        assertThat(RequestState.VERIFIED.canTransitionTo(RequestState.DELETED)).isFalse();
        assertThat(RequestState.VERIFIED.canTransitionTo(RequestState.PUBLISHED)).isFalse();
        assertThat(RequestState.VERIFIED.canTransitionTo(null)).isFalse();
    }

    @Test
    @DisplayName("ACCEPTED should allow transition to PUBLISHED and REJECTED only")
    void acceptedTransitions() {
        assertThat(RequestState.ACCEPTED.canTransitionTo(RequestState.PUBLISHED)).isTrue();
        assertThat(RequestState.ACCEPTED.canTransitionTo(RequestState.REJECTED)).isTrue();

        assertThat(RequestState.ACCEPTED.canTransitionTo(RequestState.ACCEPTED)).isFalse();
        assertThat(RequestState.ACCEPTED.canTransitionTo(RequestState.CREATED)).isFalse();
        assertThat(RequestState.ACCEPTED.canTransitionTo(RequestState.VERIFIED)).isFalse();
        assertThat(RequestState.ACCEPTED.canTransitionTo(RequestState.DELETED)).isFalse();
        assertThat(RequestState.ACCEPTED.canTransitionTo(null)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = RequestState.class, names = {"DELETED", "REJECTED", "PUBLISHED"})
    @DisplayName("Terminal states should not allow transitions to any state")
    void terminalStatesCannotTransition(RequestState terminalState) {
        for (RequestState target : RequestState.values()) {
            assertThat(terminalState.canTransitionTo(target))
                    .as("%s should not be able to transition to %s", terminalState, target)
                    .isFalse();
        }
    }
}
