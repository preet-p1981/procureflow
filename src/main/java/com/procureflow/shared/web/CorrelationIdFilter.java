package com.procureflow.shared.web;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    //accept only safe values , so a client cant inject junk into logs
    private static final Pattern VALID = Pattern.compile("^[A-Za-z0-9-]{8,64}$");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException , IOException{
        String id = request.getHeader(HEADER);
        if(id == null || !VALID.matcher(id).matches()){
            id = UUID.randomUUID().toString();
        }

        MDC.put(MDC_KEY,id);
        response.setHeader(HEADER,id);
        try{
            chain.doFilter(request,response);
        }finally {
            MDC.remove(MDC_KEY);  //threads reused so always clean up

        }
    }

}