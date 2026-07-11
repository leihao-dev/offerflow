package com.offerflow;

import com.offerflow.dto.ApplicationForm;
import com.offerflow.dto.InterviewNoteForm;
import com.offerflow.model.ApplicationStage;
import com.offerflow.service.DashboardService;
import com.offerflow.service.InterviewNoteService;
import com.offerflow.service.JobApplicationService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class DashboardServiceTest {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private JobApplicationService applicationService;

    @Autowired
    private InterviewNoteService interviewNoteService;

    @Test
    void listsInterviewsThisWeek() {
        var app = applicationService.create(sampleApp());
        InterviewNoteForm note = new InterviewNoteForm();
        note.setApplicationId(app.getId());
        note.setInterviewDate(LocalDate.now());
        note.setRoundLabel("一面");
        interviewNoteService.create(note);

        var view = dashboardService.build(LocalDate.now());

        assertEquals(1, view.interviewsThisWeek());
        assertEquals(1, view.weekInterviews().size());
        assertEquals("一面", view.weekInterviews().get(0).roundLabel());
        assertEquals(app.getId(), view.weekInterviews().get(0).applicationId());
    }

    private ApplicationForm sampleApp() {
        ApplicationForm form = new ApplicationForm();
        form.setCompanyName("面试公司");
        form.setPositionTitle("工程师");
        form.setStage(ApplicationStage.TECH_INTERVIEW);
        form.setAppliedAt(LocalDate.now());
        return form;
    }

    @Test
    void reportsOverdueFollowUps() {
        ApplicationForm overdue = new ApplicationForm();
        overdue.setCompanyName("Overdue Co");
        overdue.setPositionTitle("Backend");
        overdue.setStage(ApplicationStage.TECH_INTERVIEW);
        overdue.setAppliedAt(LocalDate.now().minusDays(10));
        overdue.setNextFollowUpAt(LocalDate.now().minusDays(2));
        applicationService.create(overdue);

        var view = dashboardService.build(LocalDate.now());

        assertEquals(1, view.overdueCount());
        assertEquals(1, view.activeCount());
    }

    @Test
    void countsFollowUpDueTodayAsOverdue() {
        ApplicationForm dueToday = new ApplicationForm();
        dueToday.setCompanyName("Due Today Co");
        dueToday.setPositionTitle("Backend");
        dueToday.setStage(ApplicationStage.SCREENING);
        dueToday.setAppliedAt(LocalDate.now().minusDays(5));
        dueToday.setNextFollowUpAt(LocalDate.now());
        applicationService.create(dueToday);

        var view = dashboardService.build(LocalDate.now());

        assertEquals(1, view.overdueCount());
    }
}
