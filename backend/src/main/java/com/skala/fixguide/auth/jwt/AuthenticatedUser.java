package com.skala.fixguide.auth.jwt;

import com.skala.fixguide.user.entity.Role;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * SecurityContext 에 담기는 인증 주체. 컨트롤러에서는
 * {@code @AuthenticationPrincipal AuthenticatedUser me} 로 주입받는다.
 *
 * @param tokenId JWT 의 jti — 로그아웃 시 이 값으로 토큰을 블랙리스트에 올린다
 * @param expiresAt 토큰 만료 시각 — 블랙리스트 TTL 계산에 쓴다
 */
public record AuthenticatedUser(
        UUID userId, String name, Role role, String tokenId, Instant expiresAt) {

    public Collection<? extends GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    /** 만료까지 남은 시간. 이미 지났으면 0을 돌려준다. */
    public Duration remainingValidity(Instant now) {
        if (expiresAt == null) {
            return Duration.ZERO;
        }
        Duration remaining = Duration.between(now, expiresAt);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }
}
