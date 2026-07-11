package com.offerflow.dto;

import com.offerflow.model.JobApplication;
import java.util.List;

public record DashboardView(
        long activeCount,
        long interviewsThisWeek,
        long overdueCount,
        List<JobApplication> overdueApplications,
        List<JobApplication> recentApplications,
        List<InterviewWeekItem> weekInterviews) {
}
