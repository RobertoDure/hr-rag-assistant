package ie.com.rag.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private FilterChain filterChain;

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter(new RateLimitProperties(1, 1, 1));
    }

    private MockHttpServletRequest requestWithIp(final String ip) {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(ip);
        return request;
    }

    private MockHttpServletResponse newResponse() {
        return new MockHttpServletResponse();
    }

    @Test
    @DisplayName("Should allow requests within the token capacity")
    void doFilter_withinCapacity_allowsRequest() throws Exception {
        // Given
        final MockHttpServletRequest request = requestWithIp("10.0.0.1");
        final MockHttpServletResponse response = newResponse();

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    @DisplayName("Should reject requests once the bucket is exhausted")
    void doFilter_exceededCapacity_returns429() throws Exception {
        // Given
        final MockHttpServletRequest request = requestWithIp("10.0.0.1");
        final MockHttpServletResponse response = newResponse();

        // When
        filter.doFilter(request, response, filterChain);
        filter.doFilter(request, response, filterChain);

        // Then
        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(response.getContentAsString()).contains("Too many requests");
    }

    @Test
    @DisplayName("Should keep separate buckets for different clients")
    void doFilter_differentIps_independentBuckets() throws Exception {
        // Given
        final MockHttpServletRequest request1 = requestWithIp("10.0.0.1");
        final MockHttpServletResponse response1 = newResponse();
        filter.doFilter(request1, response1, filterChain); // consumes 10.0.0.1 bucket

        final MockHttpServletRequest request2 = requestWithIp("10.0.0.2");
        final MockHttpServletResponse response2 = newResponse();
        filter.doFilter(request2, response2, filterChain); // different bucket, should pass

        // Then
        assertThat(response1.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(response2.getStatus()).isEqualTo(HttpStatus.OK.value());
        verify(filterChain, times(2)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Should derive the client key from the X-Forwarded-For header")
    void doFilter_xForwardedFor_usedAsKey() throws Exception {
        // Given
        final MockHttpServletRequest request1 = requestWithIp("10.0.0.1");
        request1.addHeader("X-Forwarded-For", "203.0.113.5");
        filter.doFilter(request1, newResponse(), filterChain);

        // Same forwarded IP from a different remote address -> shares the bucket
        final MockHttpServletRequest request2 = requestWithIp("10.0.0.2");
        request2.addHeader("X-Forwarded-For", "203.0.113.5, 10.0.0.2");
        final MockHttpServletResponse response2 = newResponse();
        filter.doFilter(request2, response2, filterChain);

        // Then
        assertThat(response2.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        verify(filterChain, times(1)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Should allow clients without an X-Forwarded-For header their own bucket")
    void doFilter_noForwardedHeader_usesRemoteAddr() throws Exception {
        // Given
        final MockHttpServletRequest request = requestWithIp("192.168.1.10");
        final MockHttpServletResponse response = newResponse();

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        verify(filterChain).doFilter(request, response);
    }
}
