package com.offerflow.service;

import com.offerflow.dto.InterviewNoteForm;
import com.offerflow.model.InterviewNote;
import com.offerflow.model.JobApplication;
import com.offerflow.repository.InterviewNoteRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InterviewNoteService {

    private final InterviewNoteRepository repository;
    private final JobApplicationService applicationService;

    public InterviewNoteService(InterviewNoteRepository repository, JobApplicationService applicationService) {
        this.repository = repository;
        this.applicationService = applicationService;
    }

    public InterviewNote create(InterviewNoteForm form) {
        JobApplication application = applicationService.requireApplication(form.getApplicationId());
        InterviewNote note = new InterviewNote();
        note.setApplication(application);
        applyForm(note, form);
        return repository.save(note);
    }

    public InterviewNote update(Long id, InterviewNoteForm form) {
        InterviewNote note = requireNote(id);
        applyForm(note, form);
        return repository.save(note);
    }

    @Transactional(readOnly = true)
    public InterviewNote requireNote(Long id) {
        return repository.findById(id).orElseThrow(() -> new InterviewNoteNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<InterviewNote> findByApplicationId(Long applicationId) {
        return repository.findByApplicationIdOrderByInterviewDateDesc(applicationId);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new InterviewNoteNotFoundException(id);
        }
        repository.deleteById(id);
    }

    private void applyForm(InterviewNote note, InterviewNoteForm form) {
        note.setInterviewDate(form.getInterviewDate());
        note.setRoundLabel(form.getRoundLabel());
        note.setQuestionsAsked(form.getQuestionsAsked());
        note.setSelfAssessment(form.getSelfAssessment());
        note.setImprovements(form.getImprovements());
    }
}
