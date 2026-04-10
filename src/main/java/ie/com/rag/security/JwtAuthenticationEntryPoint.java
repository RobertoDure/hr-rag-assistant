package ie.com.rag.security;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.MediaType;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.ServletException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ie.com.rag.exception.ErrorResponse;

/** JWT Authentication Entry Point to handle authentication errors **/
@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /**
     * Commence method to handle unauthorized access attempts
     * If a user tries to access a secured REST resource without
     * supplying any credentials, this method will be triggered.
     * Sending a 401 Unauthorized response.
     * @param request
     * @param response
     * @param authException
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException authException
    ) throws IOException {
        // Build error response
        ErrorResponse errorResponse = ErrorResponse.builder()
            .status(HttpServletResponse.SC_UNAUTHORIZED)
            .error("Unauthorized")
            .message("Authentication is required to access this resource.")
            .path(request.getRequestURI())
            .build();

        // Set response properties
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        // Write error response as JSON
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(response.getOutputStream(), errorResponse);
    }
}









