package com.skala.fixguide.auth.jwt;

import com.skala.fixguide.user.entity.Role;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * SecurityContext 에 담기는 인증 주체. 컨트롤러에서는
 * {@code @AuthenticationPrincipal AuthenticatedUser me} 로 주입받는다.
 */
public record AuthenticatedUser(UUID userId, String name, Role role) {

    public Collection<? extends GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}
