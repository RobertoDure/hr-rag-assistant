package ie.com.rag.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionIdInterceptorTest {

    private static final String TX_KEY = TransactionIdInterceptor.TRANSACTION_ID_KEY;

    @Mock
    private FilterChain filterChain;

    private final TransactionIdInterceptor interceptor = new TransactionIdInterceptor();

    @Test
    @DisplayName("Should propagate a client-provided transaction ID and expose it via MDC")
    void doFilter_headerPresent_propagatesTransactionId() throws Exception {
        // Given
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TX_KEY, "client-tx-123");
        final MockHttpServletResponse response = new MockHttpServletResponse();

        final String[] mdcDuringChain = new String[1];
        doAnswer(invocation -> {
            mdcDuringChain[0] = MDC.get(TX_KEY);
            return null;
        }).when(filterChain).doFilter(any(), any());

        // When
        interceptor.doFilter(request, response, filterChain);

        // Then
        assertThat(response.getHeader(TX_KEY)).isEqualTo("client-tx-123");
        assertThat(mdcDuringChain[0]).isEqualTo("client-tx-123");
        assertThat(MDC.get(TX_KEY)).isNull(); // cleared after the chain
        verify(filterChain).doFilter(any(), any());
    }

    @Test
    @DisplayName("Should generate a transaction ID when the client sends none")
    void doFilter_headerMissing_generatesTransactionId() throws Exception {
        // Given
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();

        final String[] mdcDuringChain = new String[1];
        doAnswer(invocation -> {
            mdcDuringChain[0] = MDC.get(TX_KEY);
            return null;
        }).when(filterChain).doFilter(any(), any());

        // When
        interceptor.doFilter(request, response, filterChain);

        // Then
        final String headerValue = response.getHeader(TX_KEY);
        assertThat(headerValue).isNotBlank();
        assertThat(UUID.fromString(headerValue)).isNotNull(); // valid UUID
        assertThat(mdcDuringChain[0]).isEqualTo(headerValue);
        assertThat(MDC.get(TX_KEY)).isNull();
    }

    @Test
    @DisplayName("Should clear MDC even when the filter chain throws")
    void doFilter_chainThrows_mdcCleared() throws Exception {
        // Given
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TX_KEY, "tx-1");
        final MockHttpServletResponse response = new MockHttpServletResponse();
        doThrow(new RuntimeException("downstream failure")).when(filterChain).doFilter(any(), any());

        // When / Then
        assertThatThrownBy(() -> interceptor.doFilter(request, response, filterChain))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("downstream failure");
        assertThat(MDC.get(TX_KEY)).isNull();
    }
}
