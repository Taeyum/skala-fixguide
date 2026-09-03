package com.skala.fixguide.common.config;

import com.skala.fixguide.auth.token.InMemoryTokenBlacklistStore;
import com.skala.fixguide.auth.token.RedisTokenBlacklistStore;
import com.skala.fixguide.auth.token.TokenBlacklistStore;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 로그아웃 블랙리스트 저장소 선택.
 *
 * <p>{@code app.auth.token-blacklist.type} 이 {@code redis} 면 Redis 를, 그 외(기본값 포함)에는
 * 인메모리 구현을 쓴다. 기본값을 memory 로 둔 이유는 Redis 인프라가 준비되기 전에도 로컬 실행과
 * 테스트가 그대로 돌아가야 하기 때문이다. Redis 컨테이너가 올라오면 환경변수
 * {@code TOKEN_BLACKLIST_TYPE=redis} 하나로 전환된다.
 */
@Configuration
public class TokenBlacklistConfig {

    @Bean
    @ConditionalOnProperty(name = "app.auth.token-blacklist.type", havingValue = "redis")
    public TokenBlacklistStore redisTokenBlacklistStore(StringRedisTemplate redisTemplate) {
        return new RedisTokenBlacklistStore(redisTemplate);
    }

    @Bean
    @ConditionalOnProperty(name = "app.auth.token-blacklist.type", havingValue = "memory",
            matchIfMissing = true)
    public TokenBlacklistStore inMemoryTokenBlacklistStore(Clock clock) {
        return new InMemoryTokenBlacklistStore(clock);
    }
}
