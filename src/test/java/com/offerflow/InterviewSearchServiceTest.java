package com.offerflow;

import com.offerflow.dto.ApplicationForm;
import com.offerflow.dto.InterviewNoteForm;
import com.offerflow.dto.InterviewSearchHit;
import com.offerflow.model.ApplicationStage;
import com.offerflow.service.InterviewNoteService;
import com.offerflow.service.InterviewSearchService;
import com.offerflow.service.JobApplicationService;
import com.offerflow.support.DebriefLimits;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class InterviewSearchServiceTest {

    @Autowired
    private InterviewSearchService searchService;

    @Autowired
    private JobApplicationService applicationService;

    @Autowired
    private InterviewNoteService interviewNoteService;

    @Test
    void searchMatchesQuestionsAsked() {
        var app = applicationService.create(sampleApp("美团", "Java 后端"));
        InterviewNoteForm note = new InterviewNoteForm();
        note.setApplicationId(app.getId());
        note.setInterviewDate(LocalDate.of(2026, 7, 10));
        note.setQuestionsAsked("线程池原理与拒绝策略");
        interviewNoteService.create(note);

        List<InterviewSearchHit> hits = searchService.search("线程池");

        assertEquals(1, hits.size());
        assertEquals("美团", hits.get(0).companyName());
        assertTrue(hits.get(0).snippet().contains("线程"));
    }

    @Test
    void searchMatchesCompanyName() {
        var app = applicationService.create(sampleApp("字节跳动", "Go 后端"));
        InterviewNoteForm note = new InterviewNoteForm();
        note.setApplicationId(app.getId());
        note.setInterviewDate(LocalDate.now());
        note.setQuestionsAsked("基础问题");
        interviewNoteService.create(note);

        List<InterviewSearchHit> hits = searchService.search("字节");

        assertEquals(1, hits.size());
        assertEquals("字节跳动", hits.get(0).companyName());
    }

    @Test
    void searchReturnsEmptyForBlankQuery() {
        assertTrue(searchService.search("   ").isEmpty());
        assertTrue(searchService.search(null).isEmpty());
    }

    @Test
    void listRecentRespectsLimit() {
        var app = applicationService.create(sampleApp("LimitCo", "Java"));
        for (int i = 0; i < DebriefLimits.RECENT_DEBRIEF_LIMIT + 1; i++) {
            createNote(app.getId(), LocalDate.of(2026, 7, 1).plusDays(i), "note-" + i);
        }

        List<InterviewSearchHit> recent = searchService.listRecent();

        assertEquals(DebriefLimits.RECENT_DEBRIEF_LIMIT, recent.size());
    }

    @Test
    void listRecentOrdersByInterviewDateDesc() {
        var app = applicationService.create(sampleApp("OrderCo", "Go"));
        createNote(app.getId(), LocalDate.of(2026, 7, 1), "older");
        createNote(app.getId(), LocalDate.of(2026, 7, 15), "newer");

        List<InterviewSearchHit> recent = searchService.listRecent();

        assertFalse(recent.isEmpty());
        assertEquals(LocalDate.of(2026, 7, 15), recent.get(0).interviewDate());
    }

    @Test
    void searchStillFindsAcrossAllNotes() {
        var app = applicationService.create(sampleApp("SearchCo", "Java"));
        createNote(app.getId(), LocalDate.of(2026, 1, 1), "UniquePhase7bMarker");

        List<InterviewSearchHit> hits = searchService.search("UniquePhase7bMarker");

        assertEquals(1, hits.size());
        assertEquals("SearchCo", hits.get(0).companyName());
    }

    private void createNote(Long appId, LocalDate date, String questions) {
        InterviewNoteForm note = new InterviewNoteForm();
        note.setApplicationId(appId);
        note.setInterviewDate(date);
        note.setQuestionsAsked(questions);
        interviewNoteService.create(note);
    }

    private ApplicationForm sampleApp(String company, String position) {
        ApplicationForm form = new ApplicationForm();
        form.setCompanyName(company);
        form.setPositionTitle(position);
        form.setStage(ApplicationStage.TECH_INTERVIEW);
        form.setAppliedAt(LocalDate.now());
        return form;
    }
}
