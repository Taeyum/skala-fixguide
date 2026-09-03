package com.skala.fixguide.auth.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml 의 app.jwt.* 설정.
 *
 * @param secret HS256 서명 키 (Base64 아님, 최소 32바이트)
 * @param expiresInSeconds 액세스 토큰 만료(초) — 로그인 응답의 expiresIn 으로 그대로 내려간다
 * @param issuer 토큰 발급자
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long expiresInSeconds, String issuer) {
}
