package com.offerflow.controller;

import com.offerflow.dto.CompanyForm;
import com.offerflow.model.Company;
import com.offerflow.service.CompanyHasApplicationsException;
import com.offerflow.service.CompanyService;
import com.offerflow.service.DuplicateCompanyNameException;
import com.offerflow.web.FlashMessages;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/companies")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping
    public String list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String industry,
            Model model) {
        model.addAttribute("companies", companyService.search(Optional.ofNullable(q), Optional.ofNullable(industry)));
        model.addAttribute("industries", companyService.findDistinctIndustries());
        model.addAttribute("selectedIndustry", industry);
        model.addAttribute("searchQuery", q);
        return "companies/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", new CompanyForm());
        model.addAttribute("pageTitle", "新增公司档案");
        return "companies/form";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("form") CompanyForm form,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "新增公司档案");
            return "companies/form";
        }
        try {
            Company saved = companyService.create(form);
            redirectAttributes.addFlashAttribute(FlashMessages.SUCCESS, "公司档案已创建。");
            return "redirect:/companies/" + saved.getId();
        } catch (DuplicateCompanyNameException ex) {
            result.rejectValue("name", "duplicate", "该公司档案已存在，请直接编辑或更换名称。");
            model.addAttribute("pageTitle", "新增公司档案");
            return "companies/form";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Company company = companyService.requireCompany(id);
        model.addAttribute("company", company);
        model.addAttribute("applications", companyService.findApplications(id));
        return "companies/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Company company = companyService.requireCompany(id);
        model.addAttribute("form", toForm(company));
        model.addAttribute("pageTitle", "编辑公司档案");
        return "companies/form";
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") CompanyForm form,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "编辑公司档案");
            return "companies/form";
        }
        try {
            companyService.update(id, form);
            redirectAttributes.addFlashAttribute(FlashMessages.SUCCESS, "公司档案已更新。");
            return "redirect:/companies/" + id;
        } catch (DuplicateCompanyNameException ex) {
            result.rejectValue("name", "duplicate", "该公司档案已存在，请直接编辑或更换名称。");
            model.addAttribute("pageTitle", "编辑公司档案");
            return "companies/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            companyService.delete(id);
            redirectAttributes.addFlashAttribute(FlashMessages.SUCCESS, "公司档案已删除。");
            return "redirect:/companies";
        } catch (CompanyHasApplicationsException ex) {
            redirectAttributes.addFlashAttribute(
                    FlashMessages.ERROR, "无法删除：仍有投递记录关联该公司，请先解除关联或删除投递。");
            return "redirect:/companies/" + id;
        }
    }

    private CompanyForm toForm(Company company) {
        CompanyForm form = new CompanyForm();
        form.setId(company.getId());
        form.setName(company.getName());
        form.setIndustry(company.getIndustry());
        form.setWebsiteUrl(company.getWebsiteUrl());
        form.setCareersUrl(company.getCareersUrl());
        form.setReferralCode(company.getReferralCode());
        form.setReferralContact(company.getReferralContact());
        form.setReferralMethod(company.getReferralMethod());
        form.setReferralUrl(company.getReferralUrl());
        form.setReferralNotes(company.getReferralNotes());
        form.setCompanyNotes(company.getCompanyNotes());
        form.setExternalDebriefUrl(company.getExternalDebriefUrl());
        return form;
    }
}
