package ie.com.rag.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter to intercept HTTP requests and responses to manage transaction IDs.
 * It checks for an existing transaction ID in the request headers; if absent, it generates a new one.
 * The transaction ID is added to both the request context (MDC) and the response headers.
 */
@Component
public class TransactionIdInterceptor implements Filter {

    public static final String TRANSACTION_ID_KEY = "transactionId";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    /**
     * Intercepts HTTP requests and responses to manage transaction IDs.
     *
     * @param request  the incoming ServletRequest
     * @param response the outgoing ServletResponse
     * @param chain    the FilterChain to pass the request and response to the next filter
     * @throws IOException      if an I/O error occurs during processing
     * @throws ServletException if a servlet error occurs during processing
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        String transactionId = httpServletRequest.getHeader(TRANSACTION_ID_KEY);
        if (transactionId == null || transactionId.isEmpty()) {
            transactionId = UUID.randomUUID().toString();
        }

        // Also add it to MDC so it appears automatically in log messages
        MDC.put("transactionId", transactionId);
        // Add transaction ID to response header
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        httpServletResponse.setHeader(TRANSACTION_ID_KEY, transactionId);

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    /**
     * Cleans up resources when the filter is destroyed.
     */
    @Override
    public void destroy() {
        MDC.clear();
    }
}

