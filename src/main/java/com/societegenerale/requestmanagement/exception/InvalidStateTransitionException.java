package com.societegenerale.requestmanagement.exception;

import com.societegenerale.requestmanagement.domain.RequestState;

public class InvalidStateTransitionException extends RuntimeException {

    private final Long requestId;
    private RequestState currentState;
    private RequestState targetState;

    public InvalidStateTransitionException(Long requestId, RequestState currentState, RequestState targetState) {
        super(String.format("Cannot transition request%s from state %s to %s",
                requestId != null ? " with id " + requestId : "", currentState, targetState));
        this.requestId = requestId;
        this.currentState = currentState;
        this.targetState = targetState;
    }

    public InvalidStateTransitionException(Long requestId, String message) {
        super(String.format("Cannot modify request%s: %s",
                requestId != null ? " with id " + requestId : "", message));
        this.requestId = requestId;
    }

    public InvalidStateTransitionException(RequestState currentState, RequestState targetState) {
        this(null, currentState, targetState);
    }

    public Long getRequestId() {
        return requestId;
    }

    public RequestState getCurrentState() {
        return currentState;
    }

    public RequestState getTargetState() {
        return targetState;
    }
}
