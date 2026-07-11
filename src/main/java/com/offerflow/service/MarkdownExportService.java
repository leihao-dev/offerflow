package com.offerflow.service;

import com.offerflow.model.Company;
import com.offerflow.model.InterviewNote;
import com.offerflow.model.JobApplication;
import com.offerflow.repository.InterviewNoteRepository;
import com.offerflow.web.StageLabels;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MarkdownExportService {

    private final InterviewNoteRepository interviewNoteRepository;

    public MarkdownExportService(InterviewNoteRepository interviewNoteRepository) {
        this.interviewNoteRepository = interviewNoteRepository;
    }

    public String export(JobApplication app) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(app.getCompanyName()).append(" — ").append(app.getPositionTitle()).append("\n\n");
        sb.append("- 阶段：").append(StageLabels.label(app.getStage())).append("\n");
        sb.append("- 投递日：").append(app.getAppliedAt()).append("\n");
        sb.append("- 下次跟进：")
                .append(app.getNextFollowUpAt() != null ? app.getNextFollowUpAt() : "未设置")
                .append("\n");
        if (app.getSource() != null) {
            sb.append("- 渠道：").append(app.getSource()).append("\n");
        }
        sb.append("\n");

        Company company = app.getCompany();
        if (company != null) {
            sb.append("## 公司档案\n\n");
            sb.append("- 公司：").append(company.getName()).append("\n");
            if (company.getCareersUrl() != null) {
                sb.append("- 招聘页：").append(company.getCareersUrl()).append("\n");
            }
            if (company.getReferralCode() != null) {
                sb.append("- 内推码：").append(company.getReferralCode()).append("\n");
            }
            sb.append("\n");
        }

        appendSection(sb, "JD", app.getJdContent());
        appendSection(sb, "本岗位调研笔记", app.getCompanyNotes());
        appendSection(sb, "准备清单", app.getPrepChecklist());

        sb.append("## 面试复盘\n\n");
        List<InterviewNote> notes =
                interviewNoteRepository.findByApplicationIdOrderByInterviewDateDesc(app.getId());
        if (notes.isEmpty()) {
            sb.append("（暂无）\n");
        } else {
            for (InterviewNote note : notes) {
                sb.append("### ")
                        .append(note.getInterviewDate())
                        .append(note.getRoundLabel() != null ? " · " + note.getRoundLabel() : "")
                        .append("\n\n");
                if (note.getQuestionsAsked() != null) {
                    sb.append("**问题：**\n").append(note.getQuestionsAsked()).append("\n\n");
                }
                if (note.getSelfAssessment() != null) {
                    sb.append("**自评：**\n").append(note.getSelfAssessment()).append("\n\n");
                }
                if (note.getImprovements() != null) {
                    sb.append("**改进：**\n").append(note.getImprovements()).append("\n\n");
                }
            }
        }
        return sb.toString();
    }

    public String buildFilename(JobApplication app) {
        String company = sanitize(app.getCompanyName());
        String position = sanitize(app.getPositionTitle());
        if (company.isBlank() && position.isBlank()) {
            return "offerflow-export.md";
        }
        return "offerflow-" + company + "-" + position + ".md";
    }

    private static void appendSection(StringBuilder sb, String title, String content) {
        if (content != null && !content.isBlank()) {
            sb.append("## ").append(title).append("\n\n").append(content).append("\n\n");
        }
    }

    private static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = value.replaceAll("[^\\p{L}\\p{N}]+", "-").replaceAll("^-|-$", "");
        return cleaned.length() > 50 ? cleaned.substring(0, 50) : cleaned;
    }
}
