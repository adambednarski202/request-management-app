package com.societegenerale.requestmanagement.controller;

import com.societegenerale.requestmanagement.domain.RequestState;
import com.societegenerale.requestmanagement.dto.*;
import com.societegenerale.requestmanagement.service.RequestService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/requests")
public class RequestController {

    private final RequestService requestService;

    public RequestController(RequestService requestService) {
        this.requestService = requestService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<RequestResponse> create(@Valid @RequestBody CreateRequestCommand command) {
        RequestResponse response = requestService.create(command);
        return ResponseEntity
                .created(URI.create("/api/requests/" + response.id()))
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RequestResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(requestService.getById(id));
    }

    @RequestMapping(value = "/{id}/content", method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<RequestResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ModifyRequestCommand command) {
        return ResponseEntity.ok(requestService.modify(id, command));
    }

    @RequestMapping(value = "/{id}/verify", method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<RequestResponse> verify(@PathVariable Long id) {
        return ResponseEntity.ok(requestService.verify(id));
    }

    @RequestMapping(value = "/{id}/delete", method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<RequestResponse> delete(
            @PathVariable Long id,
            @RequestBody(required = false) StateTransitionCommand command) {
        String reason = command != null ? command.reason() : null;
        return ResponseEntity.ok(requestService.delete(id, reason));
    }

    @RequestMapping(value = "/{id}/reject", method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<RequestResponse> reject(
            @PathVariable Long id,
            @RequestBody(required = false) StateTransitionCommand command) {
        String reason = command != null ? command.reason() : null;
        return ResponseEntity.ok(requestService.reject(id, reason));
    }

    @RequestMapping(value = "/{id}/accept", method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<RequestResponse> accept(@PathVariable Long id) {
        return ResponseEntity.ok(requestService.accept(id));
    }

    @RequestMapping(value = "/{id}/publish", method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<RequestResponse> publish(@PathVariable Long id) {
        return ResponseEntity.ok(requestService.publish(id));
    }

    @GetMapping
    public ResponseEntity<PageResponse<RequestResponse>> browse(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) RequestState state,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(requestService.findAll(name, state, pageable));
    }
}
