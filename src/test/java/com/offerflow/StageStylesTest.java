package com.offerflow;

import com.offerflow.model.ApplicationStage;
import com.offerflow.web.StageStyles;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StageStylesTest {

    @Test
    void appliedUsesBlueDot() {
        assertEquals("stage-dot--blue", StageStyles.dotClass(ApplicationStage.APPLIED));
    }

    @Test
    void techInterviewUsesOrangeDot() {
        assertEquals("stage-dot--orange", StageStyles.dotClass(ApplicationStage.TECH_INTERVIEW));
    }

    @Test
    void offerUsesGreenDot() {
        assertEquals("stage-dot--green", StageStyles.dotClass(ApplicationStage.OFFER));
    }

    @Test
    void rejectedUsesGrayDot() {
        assertEquals("stage-dot--gray", StageStyles.dotClass(ApplicationStage.REJECTED));
    }
}
