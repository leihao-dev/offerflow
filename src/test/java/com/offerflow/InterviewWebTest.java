package com.offerflow;

import com.offerflow.service.InterviewTemplateService;
import com.offerflow.web.FlashMessages;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InterviewWebTest {

    @Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @Test
    void newInterviewWithTemplatePrefillsForm() throws Exception {
        String redirectUrl = mockMvc.perform(post("/applications")
                        .param("companyName", "测试公司")
                        .param("positionTitle", "Java 后端")
                        .param("stage", "APPLIED")
                        .param("appliedAt", "2026-07-12"))
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getResponse()
                .getRedirectedUrl();

        String appId = redirectUrl.replace("/applications/", "");

        mockMvc.perform(get("/applications/" + appId + "/interviews/new")
                        .param("template", InterviewTemplateService.JAVA_BACKEND))
                .andExpect(status().isOk())
                .andExpect(view().name("interviews/form"))
                .andExpect(content().string(containsString("已加载 Java 后端复盘框架")))
                .andExpect(content().string(containsString("项目深挖")));
    }

    @Test
    void newInterviewWithUnknownTemplateRedirectsWithError() throws Exception {
        String redirectUrl = mockMvc.perform(post("/applications")
                        .param("companyName", "测试公司")
                        .param("positionTitle", "Java 后端")
                        .param("stage", "APPLIED")
                        .param("appliedAt", "2026-07-12"))
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getResponse()
                .getRedirectedUrl();

        String appId = redirectUrl.replace("/applications/", "");

        mockMvc.perform(get("/applications/" + appId + "/interviews/new")
                        .param("template", "unknown-pack"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute(FlashMessages.ERROR, containsString("模板不存在")));
    }
}
