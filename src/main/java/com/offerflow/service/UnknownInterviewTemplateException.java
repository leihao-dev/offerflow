package com.offerflow.service;

public class UnknownInterviewTemplateException extends RuntimeException {

    public UnknownInterviewTemplateException(String templateId) {
        super("Unknown interview template: " + templateId);
    }
}
