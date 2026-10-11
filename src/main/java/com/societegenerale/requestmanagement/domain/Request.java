package com.societegenerale.requestmanagement.domain;

import com.societegenerale.requestmanagement.exception.InvalidStateTransitionException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Request {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestState state;

    @Column
    private String reason;

    @Column(unique = true)
    private Long publicationId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.state == null) {
            this.state = RequestState.CREATED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void transitionTo(RequestState targetState) {
        if (this.state == null || !this.state.canTransitionTo(targetState)) {
            throw new InvalidStateTransitionException(this.id, this.state, targetState);
        }
        this.state = targetState;
    }

    public void verify() {
        transitionTo(RequestState.VERIFIED);
    }

    public void modify() {
        if (body == null || body.trim().isEmpty()) {
            throw new IllegalArgumentException("A body must be supplied whenever a request is modified");
        }
        if (state == null || (state != RequestState.VERIFIED && state != RequestState.CREATED))
        {
            throw new InvalidStateTransitionException(id, "A state must be either VERIFIED or CREATED to modify the request");
        }
    }

    public void delete(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("A reason must be supplied whenever a request is deleted");
        }
        transitionTo(RequestState.DELETED);
        this.reason = reason;
    }

    public void reject(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("A reason must be supplied whenever a request is rejected");
        }
        transitionTo(RequestState.REJECTED);
        this.reason = reason;
    }

    public void accept() {
        transitionTo(RequestState.ACCEPTED);
    }

    public void publish() {
        transitionTo(RequestState.PUBLISHED);
        this.publicationId = this.id;
    }
}
