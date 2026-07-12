package com.offerflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.offerflow.dto.ApplyPrepResult;
import com.offerflow.dto.DebriefTemplate;
import com.offerflow.dto.InterviewNoteForm;
import com.offerflow.dto.InterviewTemplatePack;
import com.offerflow.dto.TemplatePackInfo;
import com.offerflow.dto.TemplatePreviewView;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InterviewTemplateService {

    public static final String JAVA_BACKEND = "java-backend";
    public static final String FRONTEND_REACT = "frontend-react";
    public static final String GO_BACKEND = "go-backend";

    private static final Map<String, String> TEMPLATE_RESOURCES = Map.of(
            JAVA_BACKEND, "seeds/java-backend-interview.json",
            FRONTEND_REACT, "seeds/frontend-react-interview.json",
            GO_BACKEND, "seeds/go-backend-interview.json");

    private static final List<String> TEMPLATE_ORDER =
            List.of(JAVA_BACKEND, FRONTEND_REACT, GO_BACKEND);

    private static final int PREP_PREVIEW_LINES = 8;

    private final JobApplicationService applicationService;
    private final ObjectMapper objectMapper;

    public InterviewTemplateService(JobApplicationService applicationService, ObjectMapper objectMapper) {
        this.applicationService = applicationService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public InterviewTemplatePack requirePack(String templateId) {
        return loadPack(resolveResourcePath(templateId));
    }

    @Transactional(readOnly = true)
    public List<TemplatePackInfo> listAvailableTemplates() {
        return TEMPLATE_ORDER.stream()
                .map(id -> {
                    InterviewTemplatePack pack = requirePack(id);
                    return new TemplatePackInfo(id, pack.title());
                })
                .toList();
    }

    public ApplyPrepResult applyPrepChecklist(Long applicationId, String templateId) {
        InterviewTemplatePack pack = requirePack(templateId);
        boolean applied = applicationService.applyPrepChecklistIfEmpty(applicationId, pack.prepChecklist());
        return new ApplyPrepResult(applied);
    }

    public void applyDebriefTemplate(InterviewNoteForm form, String templateId) {
        DebriefTemplate debrief = requirePack(templateId).debrief();
        if (isBlank(form.getRoundLabel()) && debrief.roundLabel() != null) {
            form.setRoundLabel(debrief.roundLabel());
        }
        if (isBlank(form.getQuestionsAsked())) {
            form.setQuestionsAsked(debrief.questionsAsked());
        }
        if (isBlank(form.getSelfAssessment())) {
            form.setSelfAssessment(debrief.selfAssessment());
        }
        if (isBlank(form.getImprovements())) {
            form.setImprovements(debrief.improvements());
        }
    }

    @Transactional(readOnly = true)
    public TemplatePreviewView previewTemplate(String templateId) {
        InterviewTemplatePack pack = requirePack(templateId);
        DebriefTemplate debrief = pack.debrief();
        return new TemplatePreviewView(
                pack.id(),
                pack.title(),
                excerptPrep(pack.prepChecklist()),
                debrief.roundLabel(),
                summarizeDebrief(debrief));
    }

    private static String excerptPrep(String checklist) {
        if (checklist == null || checklist.isBlank()) {
            return "";
        }
        String[] lines = checklist.split("\\R");
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            sb.append(line).append('\n');
            count++;
            if (count >= PREP_PREVIEW_LINES) {
                break;
            }
        }
        if (lines.length > PREP_PREVIEW_LINES) {
            sb.append('…');
        }
        return sb.toString().trim();
    }

    private static String summarizeDebrief(DebriefTemplate debrief) {
        StringBuilder sb = new StringBuilder();
        if (debrief.roundLabel() != null) {
            sb.append("轮次：").append(debrief.roundLabel()).append('\n');
        }
        appendFirstLine(sb, "问题框架", debrief.questionsAsked());
        appendFirstLine(sb, "自评框架", debrief.selfAssessment());
        appendFirstLine(sb, "改进框架", debrief.improvements());
        return sb.toString().trim();
    }

    private static void appendFirstLine(StringBuilder sb, String label, String block) {
        if (block == null || block.isBlank()) {
            return;
        }
        String first = block.lines().filter(line -> !line.isBlank()).findFirst().orElse("");
        sb.append(label).append("：").append(first).append('\n');
    }

    private String resolveResourcePath(String templateId) {
        String resourcePath = TEMPLATE_RESOURCES.get(templateId);
        if (resourcePath == null) {
            throw new UnknownInterviewTemplateException(templateId);
        }
        return resourcePath;
    }

    private InterviewTemplatePack loadPack(String resourcePath) {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        if (!resource.exists()) {
            throw new IllegalStateException("Template resource missing: " + resourcePath);
        }
        try (InputStream input = resource.getInputStream()) {
            return objectMapper.readValue(input, InterviewTemplatePack.class);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read template resource: " + resourcePath, ex);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
