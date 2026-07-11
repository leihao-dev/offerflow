package com.offerflow.support;

import com.offerflow.model.ApplicationStage;
import com.offerflow.model.JobApplication;
import com.offerflow.service.JobApplicationService;
import java.time.LocalDate;
import java.util.EnumSet;

public final class FollowUpRules {

    private static final EnumSet<ApplicationStage> TERMINAL_STAGES = JobApplicationService.TERMINAL_STAGES;

    private FollowUpRules() {}

    public static boolean isOverdue(JobApplication application, LocalDate today) {
        if (application.getNextFollowUpAt() == null) {
            return false;
        }
        if (TERMINAL_STAGES.contains(application.getStage())) {
            return false;
        }
        return !application.getNextFollowUpAt().isAfter(today);
    }
}
