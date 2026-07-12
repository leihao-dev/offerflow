package com.offerflow.web;

import com.offerflow.model.ApplicationStage;

public final class StageStyles {

    private StageStyles() {}

    public static String dotClass(ApplicationStage stage) {
        if (stage == null) {
            return "stage-dot--gray";
        }
        return switch (stage) {
            case APPLIED, SCREENING -> "stage-dot--blue";
            case TECH_INTERVIEW, FINAL_INTERVIEW -> "stage-dot--orange";
            case OFFER -> "stage-dot--green";
            case REJECTED, WITHDRAWN -> "stage-dot--gray";
        };
    }
}
