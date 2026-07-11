package com.offerflow.controller;

import com.offerflow.dto.ApplicationForm;
import com.offerflow.dto.ApplyPrepResult;
import com.offerflow.model.ApplicationStage;
import com.offerflow.model.JobApplication;
import com.offerflow.service.CompanyService;
import com.offerflow.service.InterviewTemplateService;
import com.offerflow.service.JobApplicationService;
import com.offerflow.service.MarkdownExportService;
import com.offerflow.service.UnknownInterviewTemplateException;
import com.offerflow.web.FlashMessages;
import com.offerflow.web.StageLabels;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/applications")
public class ApplicationController {

    private final JobApplicationService applicationService;
    private final CompanyService companyService;
    private final InterviewTemplateService interviewTemplateService;
    private final MarkdownExportService markdownExportService;

    public ApplicationController(
            JobApplicationService applicationService,
            CompanyService companyService,
            InterviewTemplateService interviewTemplateService,
            MarkdownExportService markdownExportService) {
        this.applicationService = applicationService;
        this.companyService = companyService;
        this.interviewTemplateService = interviewTemplateService;
        this.markdownExportService = markdownExportService;
    }

    @GetMapping
    public String list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) ApplicationStage stage,
            Model model) {
        LocalDate today = LocalDate.now();
        model.addAttribute(
                "applications", applicationService.search(Optional.ofNullable(q), Optional.ofNullable(stage)));
        model.addAttribute("searchQuery", q);
        model.addAttribute("selectedStage", stage);
        model.addAttribute("stages", ApplicationStage.values());
        model.addAttribute("stageLabels", StageLabels.all());
        model.addAttribute("today", today);
        return "applications/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", new ApplicationForm());
        populateFormModel(model, "新增投递");
        return "applications/form";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("form") ApplicationForm form,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            populateFormModel(model, "新增投递");
            return "applications/form";
        }
        JobApplication saved = applicationService.create(form);
        redirectAttributes.addFlashAttribute(FlashMessages.SUCCESS, "投递记录已创建。");
        return "redirect:/applications/" + saved.getId();
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        JobApplication application = applicationService.requireApplication(id);
        model.addAttribute("jobApplication", application);
        model.addAttribute("stages", ApplicationStage.values());
        model.addAttribute("stageLabels", StageLabels.all());
        return "applications/detail";
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportMarkdown(@PathVariable Long id) {
        JobApplication application = applicationService.requireApplication(id);
        String markdown = markdownExportService.export(application);
        String filename = markdownExportService.buildFilename(application);
        byte[] bytes = markdown.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "markdown", StandardCharsets.UTF_8))
                .body(bytes);
    }

    @PostMapping("/{id}/apply-template")
    public String applyTemplate(
            @PathVariable Long id,
            @RequestParam(defaultValue = InterviewTemplateService.JAVA_BACKEND) String template,
            RedirectAttributes redirectAttributes) {
        try {
            ApplyPrepResult result = interviewTemplateService.applyPrepChecklist(id, template);
            if (result.applied()) {
                redirectAttributes.addFlashAttribute(FlashMessages.SUCCESS, "已填充准备清单。");
            } else {
                redirectAttributes.addFlashAttribute(
                        FlashMessages.SUCCESS, "准备清单已有内容，未覆盖。");
            }
        } catch (UnknownInterviewTemplateException ex) {
            redirectAttributes.addFlashAttribute(FlashMessages.ERROR, "模板不存在：" + template);
        }
        return "redirect:/applications/" + id;
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
            Model model,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            populateFormModel(model, "编辑投递");
            return "applications/form";
        }
        applicationService.update(id, form);
        redirectAttributes.addFlashAttribute(FlashMessages.SUCCESS, "投递记录已更新。");
        return "redirect:/applications/" + id;
    }

    @PostMapping("/{id}/stage")
    public String updateStage(
            @PathVariable Long id,
            @RequestParam ApplicationStage stage,
            RedirectAttributes redirectAttributes) {
        applicationService.updateStage(id, stage);
        redirectAttributes.addFlashAttribute(
                FlashMessages.SUCCESS, "阶段已更新为「" + StageLabels.label(stage) + "」。");
        return "redirect:/applications/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        applicationService.delete(id);
        redirectAttributes.addFlashAttribute(FlashMessages.SUCCESS, "投递记录已删除。");
        return "redirect:/applications";
    }

    private void populateFormModel(Model model, String pageTitle) {
        model.addAttribute("stages", ApplicationStage.values());
        model.addAttribute("stageLabels", StageLabels.all());
        model.addAttribute("companies", companyService.findAll(Optional.empty()));
        model.addAttribute("pageTitle", pageTitle);
    }

    private ApplicationForm toForm(JobApplication application) {
        ApplicationForm form = new ApplicationForm();
        form.setId(application.getId());
        if (application.getCompany() != null) {
            form.setCompanyId(application.getCompany().getId());
            form.setCompanyName(application.getCompany().getName());
        } else {
            form.setCompanyName(application.getCompanyName());
        }
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
