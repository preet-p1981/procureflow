package com.procureflow.shared.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.procureflow.shared.domain.CommonError;
import com.procureflow.shared.domain.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void businessRuleBecomes422WithErrorCode() throws Exception {
        mvc.perform(get("/test/business"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("CURRENCY_MISMATCH"))
                .andExpect(jsonPath("$.detail").value("Currency mismatch: INR vs USD"));
    }

    @Test
    void notFoundBecomes404() throws Exception {
        mvc.perform(get("/test/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void unexpectedErrorBecomes500WithoutLeakingDetails() throws Exception {
        mvc.perform(get("/test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"));
    }

    @RestController
    static class ThrowingController {
        @GetMapping("/test/business")
        String business() {
            throw new DomainException(CommonError.CURRENCY_MISMATCH, "Currency mismatch: INR vs USD");
        }

        @GetMapping("/test/missing")
        String missing() {
            throw new DomainException(CommonError.RESOURCE_NOT_FOUND, "Vendor 42 not found");
        }

        @GetMapping("/test/boom")
        String boom() {
            throw new IllegalStateException("secret internal detail");
        }
    }
}