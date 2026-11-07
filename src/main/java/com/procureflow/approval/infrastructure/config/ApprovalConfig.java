package com.procureflow.approval.infrastructure.config;

import com.procureflow.approval.domain.AmountThresholdRule;
import com.procureflow.approval.domain.ApprovalPolicy;
import com.procureflow.approval.domain.ApprovalRule;
import com.procureflow.approval.domain.DepartmentManagerRule;
import com.procureflow.shared.domain.Money;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;

@Configuration
public class ApprovalConfig {

    @Bean
    ApprovalRule departmentManagerRule(){
        return new DepartmentManagerRule();
    }

    @Bean
    ApprovalRule amountThresholdRule(
            @Value("${procureflow.approval.currency:INR}") String currency,
            @Value("${procureflow.approval.finance-threshold:100000}") BigDecimal financeThreshold,
            @Value("${procureflow.approval.executive-threshold:500000}") BigDecimal executiveThreshold) {
        Currency c = Currency.getInstance(currency);
        return new AmountThresholdRule(new Money(financeThreshold, c), new Money(executiveThreshold, c));

    }
        // Spring hands in EVERY ApprovalRule bean: a new rule class with @Bean is picked up with no other change. */
        @Bean
        ApprovalPolicy approvalPolicy(List<ApprovalRule> rules) {
            return new ApprovalPolicy(rules);
        }
    }

