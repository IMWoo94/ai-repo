package com.imwoo.airepo.wallet.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.imwoo.airepo.AiRepoApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = AiRepoApplication.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AuthTokenControllerTest {

    private final MockMvc mockMvc;

    @Autowired
    AuthTokenControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void issuesTokenForActiveMember() throws Exception {
        mockMvc.perform(post("/api/v1/auth/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"member-001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.memberId").value("member-001"));
    }

    @Test
    void returns404ForUnknownMember() throws Exception {
        mockMvc.perform(post("/api/v1/auth/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"ghost\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
    }
}
