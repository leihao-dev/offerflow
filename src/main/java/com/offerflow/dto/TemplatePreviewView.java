package com.offerflow.dto;

public record TemplatePreviewView(
        String templateId,
        String title,
        String prepExcerpt,
        String debriefRoundLabel,
        String debriefSummary) {}
