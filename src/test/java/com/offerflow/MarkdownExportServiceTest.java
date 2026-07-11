package com.offerflow;

import com.offerflow.dto.ApplicationForm;
import com.offerflow.dto.InterviewNoteForm;
import com.offerflow.model.ApplicationStage;
import com.offerflow.service.InterviewNoteService;
import com.offerflow.service.JobApplicationService;
import com.offerflow.service.MarkdownExportService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class MarkdownExportServiceTest {

    @Autowired
    private MarkdownExportService exportService;

    @Autowired
    private JobApplicationService applicationService;

    @Autowired
    private InterviewNoteService interviewNoteService;

    @Test
    void exportContainsApplicationAndInterviewSections() {
        ApplicationForm form = new ApplicationForm();
        form.setCompanyName("美团");
        form.setPositionTitle("Java 后端");
        form.setStage(ApplicationStage.TECH_INTERVIEW);
        form.setAppliedAt(LocalDate.of(2026, 7, 1));
        form.setPrepChecklist("□ JVM");
        var app = applicationService.create(form);

        InterviewNoteForm note = new InterviewNoteForm();
        note.setApplicationId(app.getId());
        note.setInterviewDate(LocalDate.of(2026, 7, 10));
        note.setQuestionsAsked("HashMap 原理");
        interviewNoteService.create(note);

        String md = exportService.export(applicationService.requireApplication(app.getId()));

        assertTrue(md.contains("# 美团"));
        assertTrue(md.contains("Java 后端"));
        assertTrue(md.contains("准备清单"));
        assertTrue(md.contains("HashMap 原理"));
        assertTrue(md.contains("面试复盘"));
    }
}
