package com.offerflow.service;

public class CompanyHasApplicationsException extends RuntimeException {

    public CompanyHasApplicationsException(Long id) {
        super("Company has linked applications: " + id);
    }
}
