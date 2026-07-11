package com.offerflow.service;

import com.offerflow.dto.ApplicationForm;
import com.offerflow.model.ApplicationStage;
import com.offerflow.model.Company;
import com.offerflow.model.JobApplication;
import com.offerflow.repository.JobApplicationRepository;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class JobApplicationService {

    public static final EnumSet<ApplicationStage> TERMINAL_STAGES =
            EnumSet.of(ApplicationStage.OFFER, ApplicationStage.REJECTED, ApplicationStage.WITHDRAWN);

    private final JobApplicationRepository repository;
    private final CompanyService companyService;

    public JobApplicationService(JobApplicationRepository repository, CompanyService companyService) {
        this.repository = repository;
        this.companyService = companyService;
    }

    public JobApplication create(ApplicationForm form) {
        JobApplication application = new JobApplication();
        applyForm(application, form);
        return repository.save(application);
    }

    public JobApplication update(Long id, ApplicationForm form) {
        JobApplication application = requireApplication(id);
        applyForm(application, form);
        return repository.save(application);
    }

    public JobApplication updateStage(Long id, ApplicationStage stage) {
        JobApplication application = requireApplication(id);
        application.setStage(stage);
        return repository.save(application);
    }

    @Transactional(readOnly = true)
    public JobApplication requireApplication(Long id) {
        return repository
                .findByIdWithNotes(id)
                .orElseThrow(() -> new ApplicationNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<JobApplication> findAll(Optional<ApplicationStage> stage) {
        return stage.map(repository::findByStageOrderByUpdatedAtDesc)
                .orElseGet(repository::findAllByOrderByUpdatedAtDesc);
    }

    @Transactional(readOnly = true)
    public List<JobApplication> findOverdue(LocalDate today) {
        return repository.findByNextFollowUpAtNotNullAndNextFollowUpAtLessThanEqualAndStageNotIn(
                today, TERMINAL_STAGES);
    }

    @Transactional(readOnly = true)
    public long countActive() {
        return repository.countByStageNotIn(TERMINAL_STAGES);
    }

    public boolean applyPrepChecklistIfEmpty(Long id, String checklist) {
        JobApplication application = requireApplication(id);
        String existing = application.getPrepChecklist();
        if (existing != null && !existing.isBlank()) {
            return false;
        }
        application.setPrepChecklist(checklist);
        repository.save(application);
        return true;
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ApplicationNotFoundException(id);
        }
        repository.deleteById(id);
    }

    private void applyForm(JobApplication application, ApplicationForm form) {
        applyCompany(application, form);
        application.setPositionTitle(form.getPositionTitle());
        application.setSource(form.getSource());
        application.setStage(form.getStage() != null ? form.getStage() : ApplicationStage.APPLIED);
        application.setAppliedAt(form.getAppliedAt());
        application.setNextFollowUpAt(form.getNextFollowUpAt());
        application.setSalaryRange(form.getSalaryRange());
        application.setJdContent(form.getJdContent());
        application.setCompanyNotes(form.getCompanyNotes());
        application.setPrepChecklist(form.getPrepChecklist());
        application.setPrepDone(form.getPrepDone() != null && form.getPrepDone());
    }

    private void applyCompany(JobApplication application, ApplicationForm form) {
        if (form.getCompanyId() != null) {
            Company company = companyService.requireCompany(form.getCompanyId());
            application.setCompany(company);
            application.setCompanyName(company.getName());
            return;
        }
        application.setCompany(null);
        application.setCompanyName(form.getCompanyName().trim());
    }
}
