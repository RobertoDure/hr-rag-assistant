package ie.com.rag.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class RateLimitFilter implements Filter {

    private final RateLimitProperties rateLimitProperties;
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    @Override
    public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse, final FilterChain filterChain)
        throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;

        String key = getClientIP(httpRequest);
        Bucket bucket = cache.computeIfAbsent(key, k -> createNewBucket());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(servletRequest, servletResponse);
        } else {
            log.warn("[HRRagWiser/RateLimitFilter] - Rate limit exceeded for IP: {}", key);
            httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"error\": \"Too many requests. Please try again later.\"}");
        }
    }

    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.builder()
            .capacity(rateLimitProperties.capacity())
            .refillIntervally(rateLimitProperties.refillTokens(), Duration.ofMinutes(rateLimitProperties.refillDurationMinutes()))
            .build();
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }

    private String getClientIP(final HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}

