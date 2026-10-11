package com.societegenerale.requestmanagement.dto;

import com.societegenerale.requestmanagement.domain.Request;
import com.societegenerale.requestmanagement.domain.RequestState;

import java.time.Instant;

public record RequestResponse(
        Long id,
        String name,
        String body,
        RequestState state,
        String reason,
        Long publicationId,
        Instant createdAt,
        Instant updatedAt
) {
    public static RequestResponse fromEntity(Request request) {
        return new RequestResponse(
                request.getId(),
                request.getName(),
                request.getBody(),
                request.getState(),
                request.getReason(),
                request.getPublicationId(),
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }
}
