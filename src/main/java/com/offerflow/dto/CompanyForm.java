package com.offerflow.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CompanyForm {

    private static final String URL_PATTERN = "https?://.+";

    private Long id;

    @NotBlank(message = "公司名称不能为空")
    @Size(max = 200)
    private String name;

    @Size(max = 100)
    private String industry;

    @Size(max = 500)
    private String websiteUrl;

    @Size(max = 500)
    private String careersUrl;

    @Size(max = 100)
    private String referralCode;

    @Size(max = 200)
    private String referralContact;

    @Size(max = 200)
    private String referralMethod;

    @Size(max = 500)
    private String referralUrl;

    private String referralNotes;
    private String companyNotes;

    @Size(max = 500)
    private String externalDebriefUrl;

    @AssertTrue(message = "官网链接须以 http:// 或 https:// 开头")
    public boolean isWebsiteUrlValid() {
        return isBlankOrHttpUrl(websiteUrl);
    }

    @AssertTrue(message = "招聘页链接须以 http:// 或 https:// 开头")
    public boolean isCareersUrlValid() {
        return isBlankOrHttpUrl(careersUrl);
    }

    @AssertTrue(message = "内推链接须以 http:// 或 https:// 开头")
    public boolean isReferralUrlValid() {
        return isBlankOrHttpUrl(referralUrl);
    }

    @AssertTrue(message = "面经外链须以 http:// 或 https:// 开头")
    public boolean isExternalDebriefUrlValid() {
        return isBlankOrHttpUrl(externalDebriefUrl);
    }

    private static boolean isBlankOrHttpUrl(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return value.matches(URL_PATTERN);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public String getCareersUrl() {
        return careersUrl;
    }

    public void setCareersUrl(String careersUrl) {
        this.careersUrl = careersUrl;
    }

    public String getReferralCode() {
        return referralCode;
    }

    public void setReferralCode(String referralCode) {
        this.referralCode = referralCode;
    }

    public String getReferralContact() {
        return referralContact;
    }

    public void setReferralContact(String referralContact) {
        this.referralContact = referralContact;
    }

    public String getReferralMethod() {
        return referralMethod;
    }

    public void setReferralMethod(String referralMethod) {
        this.referralMethod = referralMethod;
    }

    public String getReferralUrl() {
        return referralUrl;
    }

    public void setReferralUrl(String referralUrl) {
        this.referralUrl = referralUrl;
    }

    public String getReferralNotes() {
        return referralNotes;
    }

    public void setReferralNotes(String referralNotes) {
        this.referralNotes = referralNotes;
    }

    public String getCompanyNotes() {
        return companyNotes;
    }

    public void setCompanyNotes(String companyNotes) {
        this.companyNotes = companyNotes;
    }

    public String getExternalDebriefUrl() {
        return externalDebriefUrl;
    }

    public void setExternalDebriefUrl(String externalDebriefUrl) {
        this.externalDebriefUrl = externalDebriefUrl;
    }
}
