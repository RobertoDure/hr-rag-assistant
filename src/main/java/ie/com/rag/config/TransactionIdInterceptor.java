package ie.com.rag.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class TransactionIdInterceptor implements Filter {

    public static final String TRANSACTION_ID_KEY = "transactionId";

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        String transactionId = httpServletRequest.getHeader(TRANSACTION_ID_KEY);
        if (transactionId == null || transactionId.isEmpty()) {
            transactionId = UUID.randomUUID().toString();
        }

        MDC.put(TRANSACTION_ID_KEY, transactionId);

        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        httpServletResponse.setHeader(TRANSACTION_ID_KEY, transactionId);

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}

