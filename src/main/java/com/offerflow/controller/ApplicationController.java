package com.offerflow.controller;

import com.offerflow.dto.ApplicationForm;
import com.offerflow.model.ApplicationStage;
import com.offerflow.model.JobApplication;
import com.offerflow.service.JobApplicationService;
import com.offerflow.web.StageLabels;
import jakarta.validation.Valid;
import java.util.Optional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/applications")
public class ApplicationController {

    private final JobApplicationService applicationService;

    public ApplicationController(JobApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) ApplicationStage stage, Model model) {
        model.addAttribute("applications", applicationService.findAll(Optional.ofNullable(stage)));
        model.addAttribute("selectedStage", stage);
        model.addAttribute("stages", ApplicationStage.values());
        model.addAttribute("stageLabels", StageLabels.all());
        return "applications/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", new ApplicationForm());
        model.addAttribute("stages", ApplicationStage.values());
        model.addAttribute("stageLabels", StageLabels.all());
        model.addAttribute("pageTitle", "新增投递");
        return "applications/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") ApplicationForm form, BindingResult result, Model model) {
        if (result.hasErrors()) {
            populateFormModel(model, "新增投递");
            return "applications/form";
        }
        JobApplication saved = applicationService.create(form);
        return "redirect:/applications/" + saved.getId();
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        JobApplication application = applicationService.requireApplication(id);
        model.addAttribute("application", application);
        model.addAttribute("stages", ApplicationStage.values());
        model.addAttribute("stageLabels", StageLabels.all());
        return "applications/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        JobApplication application = applicationService.requireApplication(id);
        model.addAttribute("form", toForm(application));
        populateFormModel(model, "编辑投递");
        return "applications/form";
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") ApplicationForm form,
            BindingResult result,
            Model model) {
        if (result.hasErrors()) {
            populateFormModel(model, "编辑投递");
            return "applications/form";
        }
        applicationService.update(id, form);
        return "redirect:/applications/" + id;
    }

    @PostMapping("/{id}/stage")
    public String updateStage(@PathVariable Long id, @RequestParam ApplicationStage stage) {
        applicationService.updateStage(id, stage);
        return "redirect:/applications/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        applicationService.delete(id);
        return "redirect:/applications";
    }

    private void populateFormModel(Model model, String pageTitle) {
        model.addAttribute("stages", ApplicationStage.values());
        model.addAttribute("stageLabels", StageLabels.all());
        model.addAttribute("pageTitle", pageTitle);
    }

    private ApplicationForm toForm(JobApplication application) {
        ApplicationForm form = new ApplicationForm();
        form.setId(application.getId());
        form.setCompanyName(application.getCompanyName());
        form.setPositionTitle(application.getPositionTitle());
        form.setSource(application.getSource());
        form.setStage(application.getStage());
        form.setAppliedAt(application.getAppliedAt());
        form.setNextFollowUpAt(application.getNextFollowUpAt());
        form.setSalaryRange(application.getSalaryRange());
        form.setJdContent(application.getJdContent());
        form.setCompanyNotes(application.getCompanyNotes());
        form.setPrepChecklist(application.getPrepChecklist());
        form.setPrepDone(application.getPrepDone());
        return form;
    }
}
