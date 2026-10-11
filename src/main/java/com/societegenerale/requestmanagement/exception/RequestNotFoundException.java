package com.societegenerale.requestmanagement.exception;

public class RequestNotFoundException extends RuntimeException {

    public RequestNotFoundException(Long id) {
        super("Request not found with id: " + id);
    }

    public RequestNotFoundException(String message) {
        super(message);
    }
}
