package com.offerflow.web;

import com.offerflow.model.ApplicationStage;
import java.util.LinkedHashMap;
import java.util.Map;

public final class StageLabels {

    private static final Map<ApplicationStage, String> LABELS = new LinkedHashMap<>();

    static {
        LABELS.put(ApplicationStage.APPLIED, "已投递");
        LABELS.put(ApplicationStage.SCREENING, "简历筛选");
        LABELS.put(ApplicationStage.TECH_INTERVIEW, "技术面试");
        LABELS.put(ApplicationStage.FINAL_INTERVIEW, "终面");
        LABELS.put(ApplicationStage.OFFER, "Offer");
        LABELS.put(ApplicationStage.REJECTED, "已拒绝");
        LABELS.put(ApplicationStage.WITHDRAWN, "已撤回");
    }

    private StageLabels() {}

    public static String label(ApplicationStage stage) {
        if (stage == null) {
            return "未设置";
        }
        return LABELS.getOrDefault(stage, stage.name());
    }

    public static Map<ApplicationStage, String> all() {
        return LABELS;
    }
}
