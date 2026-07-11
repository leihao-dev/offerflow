package com.offerflow.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "companies")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 200)
    private String name;

    @Column(length = 100)
    private String industry;

    @Column(length = 500)
    private String websiteUrl;

    @Column(length = 500)
    private String careersUrl;

    @Column(length = 100)
    private String referralCode;

    @Column(length = 200)
    private String referralContact;

    @Column(length = 200)
    private String referralMethod;

    @Column(length = 500)
    private String referralUrl;

    @Lob
    private String referralNotes;

    @Lob
    private String companyNotes;

    @Column(length = 500)
    private String externalDebriefUrl;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
