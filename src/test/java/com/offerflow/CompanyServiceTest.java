package com.offerflow;

import com.offerflow.dto.CompanyForm;
import com.offerflow.model.ApplicationStage;
import com.offerflow.model.Company;
import com.offerflow.model.JobApplication;
import com.offerflow.repository.JobApplicationRepository;
import com.offerflow.service.CompanyHasApplicationsException;
import com.offerflow.service.CompanyNotFoundException;
import com.offerflow.service.CompanyService;
import com.offerflow.service.DuplicateCompanyNameException;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class CompanyServiceTest {

    @Autowired
    private CompanyService companyService;

    @Autowired
    private JobApplicationRepository applicationRepository;

    private CompanyForm sampleForm(String name) {
        CompanyForm form = new CompanyForm();
        form.setName(name);
        form.setIndustry("互联网");
        form.setWebsiteUrl("https://example.com");
        form.setCareersUrl("https://example.com/jobs");
        form.setReferralCode("REF123");
        form.setReferralContact("张三");
        form.setReferralMethod("员工内推");
        return form;
    }

    @Test
    void createsCompany() {
        Company saved = companyService.create(sampleForm("Acme Corp"));

        assertNotNull(saved.getId());
        assertEquals("Acme Corp", saved.getName());
        assertEquals("互联网", saved.getIndustry());
        assertEquals("https://example.com/jobs", saved.getCareersUrl());
    }

    @Test
    void rejectsDuplicateName() {
        companyService.create(sampleForm("Acme Corp"));

        assertThrows(DuplicateCompanyNameException.class, () -> companyService.create(sampleForm("acme corp")));
    }

    @Test
    void updatesCompany() {
        Company saved = companyService.create(sampleForm("Acme Corp"));

        CompanyForm update = sampleForm("Acme Corp");
        update.setIndustry("金融科技");
        update.setReferralCode("NEW999");

        Company updated = companyService.update(saved.getId(), update);

        assertEquals("金融科技", updated.getIndustry());
        assertEquals("NEW999", updated.getReferralCode());
    }

    @Test
    void deletesCompanyWhenNoApplications() {
        Company saved = companyService.create(sampleForm("Lonely Co"));

        companyService.delete(saved.getId());

        assertThrows(CompanyNotFoundException.class, () -> companyService.requireCompany(saved.getId()));
    }

    @Test
    void rejectsDeleteWhenHasApplications() {
        Company company = companyService.create(sampleForm("Linked Co"));

        JobApplication application = new JobApplication();
        application.setCompany(company);
        application.setCompanyName(company.getName());
        application.setPositionTitle("Java Engineer");
        application.setStage(ApplicationStage.APPLIED);
        application.setAppliedAt(LocalDate.now());
        applicationRepository.save(application);

        assertThrows(CompanyHasApplicationsException.class, () -> companyService.delete(company.getId()));
        assertEquals(1, companyService.countApplications(company.getId()));
    }

    @Test
    void findAllFiltersByIndustry() {
        companyService.create(sampleForm("Alpha Tech"));
        CompanyForm finance = sampleForm("Beta Finance");
        finance.setIndustry("金融");
        companyService.create(finance);

        assertEquals(1, companyService.search(Optional.empty(), Optional.of("金融")).size());
        assertEquals(2, companyService.search(Optional.empty(), Optional.empty()).size());
    }

    @Test
    void searchByNamePartialMatch() {
        companyService.create(sampleForm("字节跳动"));
        companyService.create(sampleForm("腾讯"));

        assertEquals(1, companyService.search(Optional.of("字节"), Optional.empty()).size());
        assertEquals(0, companyService.search(Optional.of("不存在"), Optional.empty()).size());
    }
}
