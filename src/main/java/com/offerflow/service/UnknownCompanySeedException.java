package com.offerflow.service;

public class UnknownCompanySeedException extends RuntimeException {

    public UnknownCompanySeedException(String seedId) {
        super("Unknown company seed: " + seedId);
    }
}
