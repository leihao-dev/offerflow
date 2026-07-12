package com.offerflow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InterviewSearchWebTest {

    @Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @Test
    void searchPageFindsDebriefContent() throws Exception {
        String redirectUrl = mockMvc.perform(post("/applications")
                        .param("companyName", "搜索测试公司")
                        .param("positionTitle", "Java")
                        .param("stage", "TECH_INTERVIEW")
                        .param("appliedAt", "2026-07-12"))
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getResponse()
                .getRedirectedUrl();
        String appId = redirectUrl.replace("/applications/", "");

        mockMvc.perform(post("/applications/" + appId + "/interviews")
                        .param("interviewDate", "2026-07-12")
                        .param("questionsAsked", "UniqueKeyword线程池XYZ"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/interviews/search").param("q", "UniqueKeyword线程池"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("搜索测试公司")));
    }

    @Test
    void recentDebriefsShownWithoutQuery() throws Exception {
        String redirectUrl = mockMvc.perform(post("/applications")
                        .param("companyName", "BrowseVisibleCo")
                        .param("positionTitle", "Java")
                        .param("stage", "APPLIED")
                        .param("appliedAt", "2026-07-12"))
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getResponse()
                .getRedirectedUrl();
        String appId = redirectUrl.replace("/applications/", "");

        mockMvc.perform(post("/applications/" + appId + "/interviews")
                        .param("interviewDate", "2026-07-10")
                        .param("questionsAsked", "browse test content"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/interviews/search"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("BrowseVisibleCo")))
                .andExpect(content().string(containsString("显示最近 50 条")));
    }

    @Test
    void detailLinkContainsNoteAnchor() throws Exception {
        String redirectUrl = mockMvc.perform(post("/applications")
                        .param("companyName", "AnchorCo")
                        .param("positionTitle", "Java")
                        .param("stage", "APPLIED")
                        .param("appliedAt", "2026-07-12"))
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getResponse()
                .getRedirectedUrl();
        String appId = redirectUrl.replace("/applications/", "");

        mockMvc.perform(post("/applications/" + appId + "/interviews")
                        .param("interviewDate", "2026-07-10")
                        .param("questionsAsked", "anchor test"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/interviews/search"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("#note-")))
                .andExpect(content().string(containsString("/interviews/")))
                .andExpect(content().string(containsString("/edit")));
    }
}
