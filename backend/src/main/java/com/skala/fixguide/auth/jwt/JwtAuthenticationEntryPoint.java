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
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/** 토큰 없이 보호된 API 에 접근한 경우 401 TOKEN_INVALID 로 응답한다. */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException e)
            throws IOException {
        response.setStatus(ErrorCode.TOKEN_INVALID.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(
                response.getWriter(),
                ErrorResponse.of(
                        ErrorCode.TOKEN_INVALID,
                        "인증 토큰이 필요합니다.",
                        request.getRequestURI()));
    }
}
