package com.offerflow.service;

public class DuplicateCompanyNameException extends RuntimeException {

    public DuplicateCompanyNameException(String name) {
        super("Company already exists: " + name);
    }
}
