package com.skala.fixguide.auth.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skala.fixguide.common.error.ErrorCode;
import com.skala.fixguide.common.error.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** 역할이 맞지 않는 경우 403 FORBIDDEN_ROLE 로 응답한다. */
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request, HttpServletResponse response, AccessDeniedException e)
            throws IOException {
        response.setStatus(ErrorCode.FORBIDDEN_ROLE.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(
                response.getWriter(),
                ErrorResponse.of(
                        ErrorCode.FORBIDDEN_ROLE,
                        ErrorCode.FORBIDDEN_ROLE.getDefaultMessage(),
                        request.getRequestURI()));
    }
}
