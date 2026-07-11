package com.offerflow.controller;

import com.offerflow.dto.InterviewNoteForm;
import com.offerflow.model.InterviewNote;
import com.offerflow.model.JobApplication;
import com.offerflow.service.InterviewNoteService;
import com.offerflow.service.JobApplicationService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping
public class InterviewController {

    private final InterviewNoteService interviewNoteService;
    private final JobApplicationService applicationService;

    public InterviewController(InterviewNoteService interviewNoteService, JobApplicationService applicationService) {
        this.interviewNoteService = interviewNoteService;
        this.applicationService = applicationService;
    }

    @GetMapping("/applications/{applicationId}/interviews/new")
    public String createForm(@PathVariable Long applicationId, Model model) {
        JobApplication application = applicationService.requireApplication(applicationId);
        InterviewNoteForm form = new InterviewNoteForm();
        form.setApplicationId(applicationId);
        model.addAttribute("form", form);
        model.addAttribute("application", application);
        model.addAttribute("pageTitle", "新增面试复盘");
        return "interviews/form";
    }

    @PostMapping("/applications/{applicationId}/interviews")
    public String create(
            @PathVariable Long applicationId,
            @Valid @ModelAttribute("form") InterviewNoteForm form,
            BindingResult result,
            Model model) {
        form.setApplicationId(applicationId);
        if (result.hasErrors()) {
            model.addAttribute("application", applicationService.requireApplication(applicationId));
            model.addAttribute("pageTitle", "新增面试复盘");
            return "interviews/form";
        }
        interviewNoteService.create(form);
        return "redirect:/applications/" + applicationId;
    }

    @GetMapping("/interviews/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        InterviewNote note = interviewNoteService.requireNote(id);
        InterviewNoteForm form = toForm(note);
        model.addAttribute("form", form);
        model.addAttribute("application", note.getApplication());
        model.addAttribute("pageTitle", "编辑面试复盘");
        return "interviews/form";
    }

    @PostMapping("/interviews/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") InterviewNoteForm form,
            BindingResult result,
            Model model) {
        InterviewNote note = interviewNoteService.requireNote(id);
        form.setApplicationId(note.getApplication().getId());
        if (result.hasErrors()) {
            model.addAttribute("application", note.getApplication());
            model.addAttribute("pageTitle", "编辑面试复盘");
            return "interviews/form";
        }
        interviewNoteService.update(id, form);
        return "redirect:/applications/" + note.getApplication().getId();
    }

    @PostMapping("/interviews/{id}/delete")
    public String delete(@PathVariable Long id) {
        InterviewNote note = interviewNoteService.requireNote(id);
        Long applicationId = note.getApplication().getId();
        interviewNoteService.delete(id);
        return "redirect:/applications/" + applicationId;
    }

    private InterviewNoteForm toForm(InterviewNote note) {
        InterviewNoteForm form = new InterviewNoteForm();
        form.setId(note.getId());
        form.setApplicationId(note.getApplication().getId());
        form.setInterviewDate(note.getInterviewDate());
        form.setRoundLabel(note.getRoundLabel());
        form.setQuestionsAsked(note.getQuestionsAsked());
        form.setSelfAssessment(note.getSelfAssessment());
        form.setImprovements(note.getImprovements());
        return form;
    }
}
