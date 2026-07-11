package com.offerflow;

import com.offerflow.dto.ApplicationForm;
import com.offerflow.model.ApplicationStage;
import com.offerflow.model.JobApplication;
import com.offerflow.service.JobApplicationService;
import com.offerflow.support.FollowUpRules;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationFormValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsFollowUpBeforeAppliedDate() {
        ApplicationForm form = new ApplicationForm();
        form.setCompanyName("Acme");
        form.setPositionTitle("Dev");
        form.setStage(ApplicationStage.APPLIED);
        form.setAppliedAt(LocalDate.of(2026, 7, 10));
        form.setNextFollowUpAt(LocalDate.of(2026, 7, 9));

        Set<ConstraintViolation<ApplicationForm>> violations = validator.validate(form);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("跟进日期")));
    }

    @Test
    void allowsFollowUpOnOrAfterAppliedDate() {
        ApplicationForm form = new ApplicationForm();
        form.setCompanyName("Acme");
        form.setPositionTitle("Dev");
        form.setStage(ApplicationStage.APPLIED);
        form.setAppliedAt(LocalDate.of(2026, 7, 10));
        form.setNextFollowUpAt(LocalDate.of(2026, 7, 10));

        assertTrue(validator.validate(form).isEmpty());
    }

    @Test
    void treatsTodayAsOverdueForActiveApplications() {
        LocalDate today = LocalDate.of(2026, 7, 11);
        JobApplication app = new JobApplication();
        app.setStage(ApplicationStage.SCREENING);
        app.setNextFollowUpAt(today);

        assertTrue(FollowUpRules.isOverdue(app, today));
    }

    @Test
    void ignoresTerminalStagesForOverdue() {
        LocalDate today = LocalDate.of(2026, 7, 11);
        JobApplication app = new JobApplication();
        app.setStage(ApplicationStage.REJECTED);
        app.setNextFollowUpAt(today.minusDays(1));

        assertFalse(FollowUpRules.isOverdue(app, today));
    }
}
