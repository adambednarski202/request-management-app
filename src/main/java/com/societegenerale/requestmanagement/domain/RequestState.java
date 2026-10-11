package com.societegenerale.requestmanagement.domain;

public enum RequestState {
    CREATED,
    DELETED,
    VERIFIED,
    REJECTED,
    ACCEPTED,
    PUBLISHED;

    public boolean canTransitionTo(RequestState targetState) {
        if (targetState == null) {
            return false;
        }
        return switch (this) {
            case CREATED -> targetState == VERIFIED || targetState == DELETED;
            case VERIFIED -> targetState == ACCEPTED || targetState == REJECTED;
            case ACCEPTED -> targetState == PUBLISHED || targetState == REJECTED;
            case DELETED, REJECTED, PUBLISHED -> false;
        };
    }
}

