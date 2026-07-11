package com.offerflow;

import com.offerflow.dto.ApplicationForm;
import com.offerflow.dto.InterviewNoteForm;
import com.offerflow.model.ApplicationStage;
import com.offerflow.service.InterviewTemplateService;
import com.offerflow.service.JobApplicationService;
import com.offerflow.service.UnknownInterviewTemplateException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class InterviewTemplateServiceTest {

    @Autowired
    private InterviewTemplateService templateService;

    @Autowired
    private JobApplicationService applicationService;

    @Test
    void loadJavaBackendPack() {
        var pack = templateService.requirePack(InterviewTemplateService.JAVA_BACKEND);

        assertEquals("java-backend", pack.id());
        assertEquals("Java 后端", pack.title());
        assertTrue(pack.prepChecklist().contains("JVM"));
        assertTrue(pack.debrief().questionsAsked().contains("八股"));
    }

    @Test
    void applyPrepOnEmptyChecklist() {
        var app = applicationService.create(sampleApplication());

        var result = templateService.applyPrepChecklist(app.getId(), InterviewTemplateService.JAVA_BACKEND);

        assertTrue(result.applied());
        var reloaded = applicationService.requireApplication(app.getId());
        assertTrue(reloaded.getPrepChecklist().contains("JVM"));
    }

    @Test
    void applyPrepSkipsWhenChecklistExists() {
        ApplicationForm form = sampleApplication();
        form.setPrepChecklist("已有清单");
        var app = applicationService.create(form);

        var result = templateService.applyPrepChecklist(app.getId(), InterviewTemplateService.JAVA_BACKEND);

        assertFalse(result.applied());
        assertEquals("已有清单", applicationService.requireApplication(app.getId()).getPrepChecklist());
    }

    @Test
    void applyDebriefTemplateFillsBlankFormFields() {
        InterviewNoteForm form = new InterviewNoteForm();

        templateService.applyDebriefTemplate(form, InterviewTemplateService.JAVA_BACKEND);

        assertEquals("技术面", form.getRoundLabel());
        assertNotNull(form.getQuestionsAsked());
        assertTrue(form.getQuestionsAsked().contains("项目深挖"));
        assertNotNull(form.getSelfAssessment());
        assertNotNull(form.getImprovements());
    }

    @Test
    void applyDebriefTemplateDoesNotOverwriteExistingFields() {
        InterviewNoteForm form = new InterviewNoteForm();
        form.setQuestionsAsked("我的问题记录");

        templateService.applyDebriefTemplate(form, InterviewTemplateService.JAVA_BACKEND);

        assertEquals("我的问题记录", form.getQuestionsAsked());
        assertNotNull(form.getSelfAssessment());
    }

    @Test
    void rejectsUnknownTemplateId() {
        assertThrows(
                UnknownInterviewTemplateException.class,
                () -> templateService.requirePack("unknown-pack"));
    }

    private ApplicationForm sampleApplication() {
        ApplicationForm form = new ApplicationForm();
        form.setCompanyName("测试公司");
        form.setPositionTitle("Java 后端");
        form.setStage(ApplicationStage.APPLIED);
        form.setAppliedAt(LocalDate.now());
        return form;
    }
}
