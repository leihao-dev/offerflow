package com.offerflow.dto;

import com.offerflow.model.ApplicationStage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class ApplicationForm {

    private Long id;

    @NotBlank(message = "公司名称不能为空")
    @Size(max = 200)
    private String companyName;

    @NotBlank(message = "岗位名称不能为空")
    @Size(max = 200)
    private String positionTitle;

    @Size(max = 100)
    private String source;

    @NotNull(message = "请选择阶段")
    private ApplicationStage stage = ApplicationStage.APPLIED;

    @NotNull(message = "投递日期不能为空")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate appliedAt = LocalDate.now();

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate nextFollowUpAt;

    @Size(max = 100)
    private String salaryRange;

    private String jdContent;
    private String companyNotes;
    private String prepChecklist;
    private Boolean prepDone = false;

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
}
