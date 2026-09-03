package com.skala.fixguide.auth.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skala.fixguide.auth.token.TokenBlacklistStore;
import com.skala.fixguide.common.error.ApiException;
import com.skala.fixguide.common.error.ErrorCode;
import com.skala.fixguide.common.error.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Authorization: Bearer {accessToken} 를 읽어 SecurityContext 를 채운다. */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;
    private final TokenBlacklistStore blacklistStore;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader(HEADER);
        if (header == null || !header.startsWith(PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            AuthenticatedUser principal = tokenProvider.parse(header.substring(PREFIX.length()).trim());
            if (blacklistStore.isBlacklisted(principal.tokenId())) {
                throw new ApiException(ErrorCode.TOKEN_REVOKED);
            }
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.authorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (ApiException e) {
            SecurityContextHolder.clearContext();
            writeError(response, e, request.getRequestURI());
            return;
        }

        chain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, ApiException e, String path) throws IOException {
        response.setStatus(e.getErrorCode().getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(
                response.getWriter(), ErrorResponse.of(e.getErrorCode(), e.getMessage(), path));
    }
}
