package com.offerflow;

import com.offerflow.dto.ApplicationForm;
import com.offerflow.model.ApplicationStage;
import com.offerflow.service.JobApplicationService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Transactional
class JobApplicationServiceTest {

    @Autowired
    private JobApplicationService service;

    @Test
    void createsApplication() {
        ApplicationForm form = new ApplicationForm();
        form.setCompanyName("Acme Corp");
        form.setPositionTitle("Java Engineer");
        form.setSource("Boss直聘");
        form.setStage(ApplicationStage.APPLIED);
        form.setAppliedAt(LocalDate.now());

        var saved = service.create(form);

        assertNotNull(saved.getId());
        assertEquals("Acme Corp", saved.getCompanyName());
        assertEquals(ApplicationStage.APPLIED, saved.getStage());
    }

    @Test
    void countsActiveExcludingTerminalStages() {
        ApplicationForm active = new ApplicationForm();
        active.setCompanyName("Active Co");
        active.setPositionTitle("Dev");
        active.setStage(ApplicationStage.SCREENING);
        active.setAppliedAt(LocalDate.now());
        service.create(active);

        ApplicationForm rejected = new ApplicationForm();
        rejected.setCompanyName("Rejected Co");
        rejected.setPositionTitle("Dev");
        rejected.setStage(ApplicationStage.REJECTED);
        rejected.setAppliedAt(LocalDate.now());
        service.create(rejected);

        assertEquals(1, service.countActive());
    }
}
