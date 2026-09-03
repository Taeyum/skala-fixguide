package com.skala.fixguide.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.skala.fixguide.auth.token.InMemoryTokenBlacklistStore;
import com.skala.fixguide.auth.token.RedisTokenBlacklistStore;
import com.skala.fixguide.auth.token.TokenBlacklistStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * 블랙리스트 저장소가 설정값 하나로 갈아끼워지는지 확인한다.
 *
 * <p>Redis 인프라는 다른 팀원이 준비하므로, 이 테스트는 실제 연결이 아니라 "빈이 올바르게 선택되는가"
 * 만 본다. Lettuce 는 지연 연결이라 서버가 없어도 컨텍스트는 뜬다.
 */
class TokenBlacklistStoreSelectionTest {

    @Nested
    @ActiveProfiles("test")
    @SpringBootTest
    @DisplayName("기본값 · type=memory")
    class DefaultsToMemory {

        @Autowired
        private TokenBlacklistStore store;

        @Test
        @DisplayName("설정이 없거나 memory 면 인메모리 구현이 주입된다")
        void usesInMemoryStore() {
            assertThat(store).isInstanceOf(InMemoryTokenBlacklistStore.class);
        }
    }

    @Nested
    @ActiveProfiles("test")
    @SpringBootTest
    @TestPropertySource(properties = "app.auth.token-blacklist.type=redis")
    @DisplayName("type=redis")
    class SwitchesToRedis {

        @Autowired
        private TokenBlacklistStore store;

        @Test
        @DisplayName("설정을 redis 로 바꾸면 Redis 구현이 주입된다 — 환경변수 하나로 전환된다")
        void usesRedisStore() {
            assertThat(store).isInstanceOf(RedisTokenBlacklistStore.class);
        }
    }
}
