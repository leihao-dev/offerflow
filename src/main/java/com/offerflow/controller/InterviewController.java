package com.offerflow.controller;

import com.offerflow.dto.InterviewNoteForm;
import com.offerflow.model.InterviewNote;
import com.offerflow.model.JobApplication;
import com.offerflow.service.InterviewNoteService;
import com.offerflow.service.InterviewTemplateService;
import com.offerflow.service.JobApplicationService;
import com.offerflow.service.UnknownInterviewTemplateException;
import com.offerflow.web.FlashMessages;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class InterviewController {

    private final InterviewNoteService interviewNoteService;
    private final JobApplicationService applicationService;
    private final InterviewTemplateService interviewTemplateService;

    public InterviewController(
            InterviewNoteService interviewNoteService,
            JobApplicationService applicationService,
            InterviewTemplateService interviewTemplateService) {
        this.interviewNoteService = interviewNoteService;
        this.applicationService = applicationService;
        this.interviewTemplateService = interviewTemplateService;
    }

    @GetMapping("/applications/{applicationId}/interviews/new")
    public String createForm(
            @PathVariable Long applicationId,
            @RequestParam(required = false) String template,
            Model model,
            RedirectAttributes redirectAttributes) {
        JobApplication application = applicationService.requireApplication(applicationId);
        InterviewNoteForm form = new InterviewNoteForm();
        form.setApplicationId(applicationId);
        if (template != null && !template.isBlank()) {
            try {
                interviewTemplateService.applyDebriefTemplate(form, template);
                model.addAttribute("templateLoaded", true);
            } catch (UnknownInterviewTemplateException ex) {
                redirectAttributes.addFlashAttribute(FlashMessages.ERROR, "模板不存在：" + template);
                return "redirect:/applications/" + applicationId;
            }
        }
        model.addAttribute("form", form);
        model.addAttribute("jobApplication", application);
        model.addAttribute("pageTitle", "新增面试复盘");
        model.addAttribute("pageSubtitle", application.getCompanyName() + " · " + application.getPositionTitle());
        return "interviews/form";
    }

    @PostMapping("/applications/{applicationId}/interviews")
    public String create(
            @PathVariable Long applicationId,
            @Valid @ModelAttribute("form") InterviewNoteForm form,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        form.setApplicationId(applicationId);
        if (result.hasErrors()) {
            JobApplication application = applicationService.requireApplication(applicationId);
            model.addAttribute("jobApplication", application);
            model.addAttribute("pageTitle", "新增面试复盘");
            model.addAttribute("pageSubtitle", application.getCompanyName() + " · " + application.getPositionTitle());
            return "interviews/form";
        }
        interviewNoteService.create(form);
        redirectAttributes.addFlashAttribute(FlashMessages.SUCCESS, "面试复盘已添加。");
        return "redirect:/applications/" + applicationId;
    }

    @GetMapping("/interviews/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        InterviewNote note = interviewNoteService.requireNote(id);
        InterviewNoteForm form = toForm(note);
        model.addAttribute("form", form);
        model.addAttribute("jobApplication", note.getApplication());
        model.addAttribute("pageTitle", "编辑面试复盘");
        model.addAttribute(
                "pageSubtitle",
                note.getApplication().getCompanyName() + " · " + note.getApplication().getPositionTitle());
        return "interviews/form";
    }

    @PostMapping("/interviews/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") InterviewNoteForm form,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        InterviewNote note = interviewNoteService.requireNote(id);
        form.setApplicationId(note.getApplication().getId());
        if (result.hasErrors()) {
            model.addAttribute("jobApplication", note.getApplication());
            model.addAttribute("pageTitle", "编辑面试复盘");
            model.addAttribute(
                    "pageSubtitle",
                    note.getApplication().getCompanyName() + " · " + note.getApplication().getPositionTitle());
            return "interviews/form";
        }
        interviewNoteService.update(id, form);
        redirectAttributes.addFlashAttribute(FlashMessages.SUCCESS, "面试复盘已更新。");
        return "redirect:/applications/" + note.getApplication().getId();
    }

    @PostMapping("/interviews/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        InterviewNote note = interviewNoteService.requireNote(id);
        Long applicationId = note.getApplication().getId();
        interviewNoteService.delete(id);
        redirectAttributes.addFlashAttribute(FlashMessages.SUCCESS, "面试复盘已删除。");
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
