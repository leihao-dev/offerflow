package com.offerflow;

import com.offerflow.dto.ApplicationForm;
import com.offerflow.model.ApplicationStage;
import com.offerflow.service.DashboardService;
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
