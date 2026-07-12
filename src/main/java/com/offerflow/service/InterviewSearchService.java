package com.offerflow.service;

import com.offerflow.dto.InterviewSearchHit;
import com.offerflow.model.InterviewNote;
import com.offerflow.model.JobApplication;
import com.offerflow.repository.InterviewNoteRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InterviewSearchService {

    private static final int MAX_QUERY_LENGTH = 200;
    private static final int SNIPPET_LENGTH = 120;

    private final InterviewNoteRepository interviewNoteRepository;

    public InterviewSearchService(InterviewNoteRepository interviewNoteRepository) {
        this.interviewNoteRepository = interviewNoteRepository;
    }

    @Transactional(readOnly = true)
    public List<InterviewSearchHit> search(String rawQuery) {
        String q = normalizeQuery(rawQuery);
        if (q.isEmpty()) {
            return List.of();
        }
        return interviewNoteRepository.findAllWithApplicationOrderByInterviewDateDesc().stream()
                .filter(note -> matches(note, q))
                .map(note -> toHit(note, q))
                .toList();
    }

    private static boolean matches(InterviewNote note, String q) {
        String lower = q.toLowerCase();
        JobApplication app = note.getApplication();
        return containsIgnoreCase(note.getQuestionsAsked(), lower)
                || containsIgnoreCase(note.getSelfAssessment(), lower)
                || containsIgnoreCase(note.getImprovements(), lower)
                || containsIgnoreCase(note.getRoundLabel(), lower)
                || (app != null && containsIgnoreCase(app.getCompanyName(), lower))
                || (app != null && containsIgnoreCase(app.getPositionTitle(), lower));
    }

    private static boolean containsIgnoreCase(String value, String lowerQuery) {
        return value != null && value.toLowerCase().contains(lowerQuery);
    }

    private static String normalizeQuery(String rawQuery) {
        if (rawQuery == null) {
            return "";
        }
        String trimmed = rawQuery.trim();
        if (trimmed.length() > MAX_QUERY_LENGTH) {
            return trimmed.substring(0, MAX_QUERY_LENGTH);
        }
        return trimmed;
    }

    private InterviewSearchHit toHit(InterviewNote note, String q) {
        JobApplication app = note.getApplication();
        return new InterviewSearchHit(
                note.getId(),
                app.getId(),
                app.getCompanyName(),
                app.getPositionTitle(),
                note.getInterviewDate(),
                note.getRoundLabel(),
                buildSnippet(note, q));
    }

    private String buildSnippet(InterviewNote note, String q) {
        String source = firstMatchingField(note, q);
        if (source == null || source.isBlank()) {
            source = coalesce(note.getQuestionsAsked(), note.getSelfAssessment(), note.getImprovements());
        }
        if (source == null) {
            return "";
        }
        String flat = source.replace('\n', ' ').trim();
        int idx = flat.toLowerCase().indexOf(q.toLowerCase());
        if (idx < 0) {
            return truncate(flat, SNIPPET_LENGTH);
        }
        int start = Math.max(0, idx - 40);
        int end = Math.min(flat.length(), idx + q.length() + 60);
        String slice = flat.substring(start, end);
        if (start > 0) {
            slice = "…" + slice;
        }
        if (end < flat.length()) {
            slice = slice + "…";
        }
        return slice;
    }

    private static String firstMatchingField(InterviewNote note, String q) {
        String lower = q.toLowerCase();
        String[] fields = {
            note.getQuestionsAsked(),
            note.getSelfAssessment(),
            note.getImprovements(),
            note.getRoundLabel()
        };
        for (String field : fields) {
            if (field != null && field.toLowerCase().contains(lower)) {
                return field;
            }
        }
        JobApplication app = note.getApplication();
        if (app != null) {
            if (app.getCompanyName() != null && app.getCompanyName().toLowerCase().contains(lower)) {
                return app.getCompanyName();
            }
            if (app.getPositionTitle() != null && app.getPositionTitle().toLowerCase().contains(lower)) {
                return app.getPositionTitle();
            }
        }
        return null;
    }

    private static String coalesce(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    private static String truncate(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max) + "…";
    }
}
