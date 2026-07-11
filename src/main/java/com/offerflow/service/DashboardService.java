package com.offerflow.service;

import com.offerflow.dto.DashboardView;
import com.offerflow.repository.InterviewNoteRepository;
import com.offerflow.repository.JobApplicationRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final JobApplicationRepository applicationRepository;
    private final InterviewNoteRepository interviewNoteRepository;
    private final JobApplicationService applicationService;

    public DashboardService(
            JobApplicationRepository applicationRepository,
            InterviewNoteRepository interviewNoteRepository,
            JobApplicationService applicationService) {
        this.applicationRepository = applicationRepository;
        this.interviewNoteRepository = interviewNoteRepository;
        this.applicationService = applicationService;
    }

    public DashboardView build(LocalDate today) {
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);

        long interviewsThisWeek =
                interviewNoteRepository.findByInterviewDateBetween(weekStart, weekEnd).size();
        var overdue = applicationService.findOverdue(today);

        return new DashboardView(
                applicationService.countActive(),
                interviewsThisWeek,
                overdue.size(),
                overdue,
                applicationRepository.findTop5ByOrderByUpdatedAtDesc());
    }
}
