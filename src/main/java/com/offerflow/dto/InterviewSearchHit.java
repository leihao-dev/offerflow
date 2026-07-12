package com.offerflow.dto;

import java.time.LocalDate;

public record InterviewSearchHit(
        Long noteId,
        Long applicationId,
        String companyName,
        String positionTitle,
        LocalDate interviewDate,
        String roundLabel,
        String snippet) {}
