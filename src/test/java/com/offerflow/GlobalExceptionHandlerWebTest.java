package com.offerflow;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.offerflow.model.ApplicationStage;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GlobalExceptionHandlerWebTest {

    @Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @Test
    void invalidApplicationIdShowsNotFoundPage() throws Exception {
        mockMvc.perform(get("/applications/not-a-number"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"));
    }

    @Test
    void missingApplicationShowsNotFoundPage() throws Exception {
        mockMvc.perform(get("/applications/999999"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"));
    }

    @Test
    void detailPageRendersAfterCreate() throws Exception {
        MockHttpServletResponse createResponse = mockMvc.perform(post("/applications")
                        .param("companyName", "Acme")
                        .param("positionTitle", "Dev")
                        .param("stage", ApplicationStage.APPLIED.name())
                        .param("appliedAt", LocalDate.of(2026, 7, 10).toString()))
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getResponse();

        mockMvc.perform(get(createResponse.getRedirectedUrl()))
                .andExpect(status().isOk())
                .andExpect(view().name("applications/detail"))
                .andExpect(content().string(containsString("Acme")));
    }

    @Test
    void listPageShowsStageFilterLabels() throws Exception {
        mockMvc.perform(get("/applications"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("已投递")))
                .andExpect(content().string(containsString("简历筛选")));
    }

    @Test
    void newFormShowsStageOptions() throws Exception {
        mockMvc.perform(get("/applications/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("已投递")))
                .andExpect(content().string(containsString("技术面试")));
    }
}
