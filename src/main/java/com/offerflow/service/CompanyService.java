package com.offerflow.service;

import com.offerflow.dto.CompanyForm;
import com.offerflow.model.Company;
import com.offerflow.repository.CompanyRepository;
import com.offerflow.repository.JobApplicationRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final JobApplicationRepository applicationRepository;

    public CompanyService(CompanyRepository companyRepository, JobApplicationRepository applicationRepository) {
        this.companyRepository = companyRepository;
        this.applicationRepository = applicationRepository;
    }

    public Company create(CompanyForm form) {
        ensureUniqueName(form.getName(), null);
        Company company = new Company();
        applyForm(company, form);
        return companyRepository.save(company);
    }

    public Company update(Long id, CompanyForm form) {
        Company company = requireCompany(id);
        ensureUniqueName(form.getName(), id);
        applyForm(company, form);
        return companyRepository.save(company);
    }

    @Transactional(readOnly = true)
    public Company requireCompany(Long id) {
        return companyRepository.findById(id).orElseThrow(() -> new CompanyNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Company> findAll(Optional<String> industry) {
        return industry.filter(value -> !value.isBlank())
                .map(companyRepository::findByIndustryOrderByNameAsc)
                .orElseGet(companyRepository::findAllByOrderByNameAsc);
    }

    @Transactional(readOnly = true)
    public long countApplications(Long companyId) {
        return applicationRepository.countByCompanyId(companyId);
    }

    public void delete(Long id) {
        if (!companyRepository.existsById(id)) {
            throw new CompanyNotFoundException(id);
        }
        if (applicationRepository.countByCompanyId(id) > 0) {
            throw new CompanyHasApplicationsException(id);
        }
        companyRepository.deleteById(id);
    }

    private void ensureUniqueName(String name, Long excludeId) {
        boolean duplicate = excludeId == null
                ? companyRepository.existsByNameIgnoreCase(name)
                : companyRepository.existsByNameIgnoreCaseAndIdNot(name, excludeId);
        if (duplicate) {
            throw new DuplicateCompanyNameException(name);
        }
    }

    private void applyForm(Company company, CompanyForm form) {
        company.setName(form.getName().trim());
        company.setIndustry(trimToNull(form.getIndustry()));
        company.setWebsiteUrl(trimToNull(form.getWebsiteUrl()));
        company.setCareersUrl(trimToNull(form.getCareersUrl()));
        company.setReferralCode(trimToNull(form.getReferralCode()));
        company.setReferralContact(trimToNull(form.getReferralContact()));
        company.setReferralMethod(trimToNull(form.getReferralMethod()));
        company.setReferralUrl(trimToNull(form.getReferralUrl()));
        company.setReferralNotes(trimToNull(form.getReferralNotes()));
        company.setCompanyNotes(trimToNull(form.getCompanyNotes()));
        company.setExternalDebriefUrl(trimToNull(form.getExternalDebriefUrl()));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
