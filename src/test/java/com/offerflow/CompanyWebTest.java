package com.offerflow;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CompanyWebTest {

    @Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @Test
    void listPageLoads() throws Exception {
        mockMvc.perform(get("/companies"))
                .andExpect(status().isOk())
                .andExpect(view().name("companies/list"))
                .andExpect(content().string(containsString("目标公司")));
    }

    @Test
    void newFormShowsFields() throws Exception {
        mockMvc.perform(get("/companies/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("companies/form"))
                .andExpect(content().string(containsString("招聘官网")))
                .andExpect(content().string(containsString("内推码")));
    }

    @Test
    void createAndDetailShowsCareersLink() throws Exception {
        String redirectUrl = mockMvc.perform(post("/companies")
                        .param("name", "Acme Tech")
                        .param("industry", "互联网")
                        .param("careersUrl", "https://jobs.acme.example.com"))
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getResponse()
                .getRedirectedUrl();

        mockMvc.perform(get(redirectUrl))
                .andExpect(status().isOk())
                .andExpect(view().name("companies/detail"))
                .andExpect(content().string(containsString("Acme Tech")))
                .andExpect(content().string(containsString("https://jobs.acme.example.com")))
                .andExpect(content().string(containsString("招聘页")));
    }

    @Test
    void navIncludesCompaniesLink() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("目标公司")));
    }
}
