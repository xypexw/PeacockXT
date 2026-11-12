package com.github.peacockxt.filter;
import org.springframework.core.annotation.Order;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(1)
public class MDCLogFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String MDC_TRACE_ID_KEY = "traceId";

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String MDC_USER_ID_KEY = "userId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Retrieve TraceId passed from APISIX/BFF; generate a new one if absent or blank
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }

        String userId = request.getHeader(USER_ID_HEADER);

        try {
            // 2. Populate the MDC context
            MDC.put(MDC_TRACE_ID_KEY, traceId);
            if (userId != null && !userId.isBlank()) {
                MDC.put(MDC_USER_ID_KEY, userId);
            }

            // 3. Attach TraceId to the response header so Frontend/Clients can trace errors
            response.addHeader(TRACE_ID_HEADER, traceId);

            // 4. Continue the filter chain to downstream controllers
            filterChain.doFilter(request, response);

        } finally {
            // 5. CRITICAL: Clean up MDC context when the request finishes.
            // Spring Boot uses thread pooling; failure to clear will leak stale context into subsequent requests using this thread.
            MDC.clear();
        }
    }
}