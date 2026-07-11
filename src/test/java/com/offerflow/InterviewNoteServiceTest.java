package com.offerflow;

import com.offerflow.dto.ApplicationForm;
import com.offerflow.dto.InterviewNoteForm;
import com.offerflow.model.ApplicationStage;
import com.offerflow.service.InterviewNoteNotFoundException;
import com.offerflow.service.InterviewNoteService;
import com.offerflow.service.JobApplicationService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class InterviewNoteServiceTest {

    @Autowired
    private InterviewNoteService interviewNoteService;

    @Autowired
    private JobApplicationService applicationService;

    @Test
    void createsNoteLinkedToApplication() {
        ApplicationForm appForm = new ApplicationForm();
        appForm.setCompanyName("Test Co");
        appForm.setPositionTitle("Engineer");
        appForm.setStage(ApplicationStage.TECH_INTERVIEW);
        appForm.setAppliedAt(LocalDate.now());
        var app = applicationService.create(appForm);

        InterviewNoteForm noteForm = new InterviewNoteForm();
        noteForm.setApplicationId(app.getId());
        noteForm.setInterviewDate(LocalDate.now());
        noteForm.setRoundLabel("一面");
        noteForm.setQuestionsAsked("介绍项目");

        var saved = interviewNoteService.create(noteForm);

        assertNotNull(saved.getId());
        assertEquals(app.getId(), saved.getApplication().getId());
        assertEquals("一面", saved.getRoundLabel());
    }

    @Test
    void deleteRemovesNote() {
        ApplicationForm appForm = new ApplicationForm();
        appForm.setCompanyName("Delete Co");
        appForm.setPositionTitle("Dev");
        appForm.setStage(ApplicationStage.APPLIED);
        appForm.setAppliedAt(LocalDate.now());
        var app = applicationService.create(appForm);

        InterviewNoteForm noteForm = new InterviewNoteForm();
        noteForm.setApplicationId(app.getId());
        noteForm.setInterviewDate(LocalDate.now());
        var note = interviewNoteService.create(noteForm);

        interviewNoteService.delete(note.getId());

        assertThrows(InterviewNoteNotFoundException.class, () -> interviewNoteService.requireNote(note.getId()));
    }
}
