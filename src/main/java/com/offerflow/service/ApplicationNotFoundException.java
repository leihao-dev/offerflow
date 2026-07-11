package com.offerflow.service;

public class ApplicationNotFoundException extends RuntimeException {

    public ApplicationNotFoundException(Long id) {
        super("Application not found: " + id);
    }
}
