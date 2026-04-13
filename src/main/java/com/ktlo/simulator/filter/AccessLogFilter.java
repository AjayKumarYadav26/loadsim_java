package com.ktlo.simulator.filter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter for logging HTTP access requests to access.log.
 * Captures request details and response times for all HTTP requests.
 */
@Slf4j(topic = "ACCESS_LOG")
@Component
@Order(1)
public class AccessLogFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Generate unique request ID
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("requestId", requestId);

        long startTime = System.currentTimeMillis();

        try {
            // Process request
            chain.doFilter(request, response);

        } finally {
            long duration = System.currentTimeMillis() - startTime;

            // Log access details
            String clientIp = getClientIp(httpRequest);
            String method = httpRequest.getMethod();
            String uri = httpRequest.getRequestURI();
            String queryString = httpRequest.getQueryString();
            int status = httpResponse.getStatus();

            String fullUri = queryString != null ? uri + "?" + queryString : uri;

            // Format: IP METHOD URI STATUS DURATION REQUEST_ID
            log.info("{} {} {} {} {}ms [{}]",
                    clientIp,
                    method,
                    fullUri,
                    status,
                    duration,
                    requestId);

            MDC.clear();
        }
    }

    /**
     * Extract client IP address from request.
     * Checks X-Forwarded-For header for proxy/load balancer scenarios.
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
