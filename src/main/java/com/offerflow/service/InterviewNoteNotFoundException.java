package com.offerflow.service;

public class InterviewNoteNotFoundException extends RuntimeException {

    public InterviewNoteNotFoundException(Long id) {
        super("Interview note not found: " + id);
    }
}
