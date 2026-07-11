package com.offerflow.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "job_applications")
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String companyName;

    @Column(nullable = false, length = 200)
    private String positionTitle;

    @Column(length = 100)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApplicationStage stage = ApplicationStage.APPLIED;

    @Column(nullable = false)
    private LocalDate appliedAt;

    private LocalDate nextFollowUpAt;

    @Column(length = 100)
    private String salaryRange;

    @Lob
    private String jdContent;

    @Lob
    private String companyNotes;

    @Lob
    private String prepChecklist;

    private Boolean prepDone = false;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("interviewDate DESC")
    private List<InterviewNote> interviewNotes = new ArrayList<>();

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

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getPositionTitle() {
        return positionTitle;
    }

    public void setPositionTitle(String positionTitle) {
        this.positionTitle = positionTitle;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public ApplicationStage getStage() {
        return stage;
    }

    public void setStage(ApplicationStage stage) {
        this.stage = stage;
    }

    public LocalDate getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(LocalDate appliedAt) {
        this.appliedAt = appliedAt;
    }

    public LocalDate getNextFollowUpAt() {
        return nextFollowUpAt;
    }

    public void setNextFollowUpAt(LocalDate nextFollowUpAt) {
        this.nextFollowUpAt = nextFollowUpAt;
    }

    public String getSalaryRange() {
        return salaryRange;
    }

    public void setSalaryRange(String salaryRange) {
        this.salaryRange = salaryRange;
    }

    public String getJdContent() {
        return jdContent;
    }

    public void setJdContent(String jdContent) {
        this.jdContent = jdContent;
    }

    public String getCompanyNotes() {
        return companyNotes;
    }

    public void setCompanyNotes(String companyNotes) {
        this.companyNotes = companyNotes;
    }

    public String getPrepChecklist() {
        return prepChecklist;
    }

    public void setPrepChecklist(String prepChecklist) {
        this.prepChecklist = prepChecklist;
    }

    public Boolean getPrepDone() {
        return prepDone;
    }

    public void setPrepDone(Boolean prepDone) {
        this.prepDone = prepDone;
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

    public List<InterviewNote> getInterviewNotes() {
        if (interviewNotes == null) {
            interviewNotes = new ArrayList<>();
        }
        return interviewNotes;
    }

    public void setInterviewNotes(List<InterviewNote> interviewNotes) {
        this.interviewNotes = interviewNotes;
    }
}
