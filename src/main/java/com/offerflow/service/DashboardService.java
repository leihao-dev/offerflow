package com.offerflow.service;

import com.offerflow.dto.DashboardView;
import com.offerflow.dto.InterviewWeekItem;
import com.offerflow.model.InterviewNote;
import com.offerflow.repository.InterviewNoteRepository;
import com.offerflow.repository.JobApplicationRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
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

        List<InterviewNote> weekNotes =
                interviewNoteRepository.findByInterviewDateBetween(weekStart, weekEnd);
        List<InterviewWeekItem> weekInterviews = weekNotes.stream()
                .sorted(Comparator.comparing(InterviewNote::getInterviewDate)
                        .thenComparing(n -> n.getApplication().getCompanyName()))
                .map(n -> new InterviewWeekItem(
                        n.getId(),
                        n.getApplication().getId(),
                        n.getInterviewDate(),
                        n.getRoundLabel(),
                        n.getApplication().getCompanyName(),
                        n.getApplication().getPositionTitle()))
                .toList();
        var overdue = applicationService.findOverdue(today);

        return new DashboardView(
                applicationService.countActive(),
                weekNotes.size(),
                overdue.size(),
                overdue,
                applicationRepository.findTop5ByOrderByUpdatedAtDesc(),
                weekInterviews);
    }
}
