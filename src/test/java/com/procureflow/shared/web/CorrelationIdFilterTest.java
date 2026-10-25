package com.procureflow.shared.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class CorrelationIdFilterTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new PingController())
                .addFilters(new CorrelationIdFilter())
                .build();
    }

    @Test
    void generatesIdWhenNoneProvided() throws Exception {
        mvc.perform(get("/ping"))
                .andExpect(status().isOk())
                .andExpect(header().exists(CorrelationIdFilter.HEADER));
    }

    @Test
    void echoesValidIdFromClient() throws Exception {
        mvc.perform(get("/ping").header(CorrelationIdFilter.HEADER, "abc-12345678"))
                .andExpect(header().string(CorrelationIdFilter.HEADER, "abc-12345678"));
    }

    @Test
    void replacesUnsafeId() throws Exception {
        String result = mvc.perform(get("/ping").header(CorrelationIdFilter.HEADER, "bad id!"))
                .andReturn().getResponse().getHeader(CorrelationIdFilter.HEADER);
        assertThat(result).isNotEqualTo("bad id!").hasSize(36);   // a fresh UUID
    }

    @Test
    void clearsMdcAfterRequest() throws Exception {
        mvc.perform(get("/ping"));
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @RestController
    static class PingController {
        @GetMapping("/ping")
        String ping() {
            return "ok";
        }
    }
}