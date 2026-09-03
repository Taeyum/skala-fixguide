package com.skala.fixguide.auth.jwt;

import com.skala.fixguide.common.error.ApiException;
import com.skala.fixguide.common.error.ErrorCode;
import com.skala.fixguide.user.entity.Role;
import com.skala.fixguide.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

/**
 * JWT 발급·검증 담당. 토큰에는 사용자 식별자(sub)와 role, name 만 담는다.
 *
 * <p>jti 는 로그아웃 블랙리스트의 키로 쓰인다. 토큰마다 새로 발급되므로 같은 사용자가 여러 기기에서
 * 로그인해도 로그아웃한 기기의 토큰만 무효화된다.
 */
@Component
public class JwtTokenProvider {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_NAME = "name";

    private final SecretKey key;
    private final JwtProperties properties;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(properties.issuer())
                .subject(user.getId().toString())
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_NAME, user.getName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(properties.expiresInSeconds())))
                .signWith(key)
                .compact();
    }

    public long getExpiresInSeconds() {
        return properties.expiresInSeconds();
    }

    /**
     * 토큰을 파싱해 인증 주체를 만든다.
     *
     * @throws ApiException TOKEN_EXPIRED / TOKEN_INVALID
     */
    public AuthenticatedUser parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Date expiration = claims.getExpiration();
            return new AuthenticatedUser(
                    UUID.fromString(claims.getSubject()),
                    claims.get(CLAIM_NAME, String.class),
                    Role.valueOf(claims.get(CLAIM_ROLE, String.class)),
                    claims.getId(),
                    expiration == null ? null : expiration.toInstant());
        } catch (ExpiredJwtException e) {
            throw new ApiException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            throw new ApiException(ErrorCode.TOKEN_INVALID);
        }
    }
}
