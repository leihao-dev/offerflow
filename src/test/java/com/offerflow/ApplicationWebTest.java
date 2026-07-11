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

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApplicationWebTest {

    @Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @Test
    void detailShowsPrepTemplateCard() throws Exception {
        String redirectUrl = mockMvc.perform(post("/applications")
                        .param("companyName", "测试公司")
                        .param("positionTitle", "Java 后端")
                        .param("stage", "APPLIED")
                        .param("appliedAt", "2026-07-12"))
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getResponse()
                .getRedirectedUrl();

        mockMvc.perform(get(redirectUrl))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Java 后端 · 准备模板")))
                .andExpect(content().string(containsString("填充准备清单")));
    }

    @Test
    void applyTemplateFillsEmptyPrepChecklist() throws Exception {
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

        mockMvc.perform(post("/applications/" + appId + "/apply-template")
                        .param("template", InterviewTemplateService.JAVA_BACKEND))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute(FlashMessages.SUCCESS, containsString("已填充准备清单")));

        mockMvc.perform(get("/applications/" + appId))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("JVM")));
    }

    @Test
    void applyTemplateSkipsWhenPrepExists() throws Exception {
        String redirectUrl = mockMvc.perform(post("/applications")
                        .param("companyName", "测试公司")
                        .param("positionTitle", "Java 后端")
                        .param("stage", "APPLIED")
                        .param("appliedAt", "2026-07-12")
                        .param("prepChecklist", "已有清单"))
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getResponse()
                .getRedirectedUrl();

        String appId = redirectUrl.replace("/applications/", "");

        mockMvc.perform(post("/applications/" + appId + "/apply-template")
                        .param("template", InterviewTemplateService.JAVA_BACKEND))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute(FlashMessages.SUCCESS, containsString("未覆盖")));
    }

    @Test
    void listSearchByCompanyName() throws Exception {
        mockMvc.perform(post("/applications")
                        .param("companyName", "美团")
                        .param("positionTitle", "Java")
                        .param("stage", "APPLIED")
                        .param("appliedAt", "2026-07-12"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/applications").param("q", "美团"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("美团")))
                .andExpect(content().string(containsString("type=\"search\"")));
    }
}
