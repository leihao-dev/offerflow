package com.offerflow.dto;

import java.time.LocalDate;

public record InterviewWeekItem(
        Long noteId,
        Long applicationId,
        LocalDate interviewDate,
        String roundLabel,
        String companyName,
        String positionTitle) {}
