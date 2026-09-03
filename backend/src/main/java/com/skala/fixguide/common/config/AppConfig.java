package com.skala.fixguide.common.config;

import com.skala.fixguide.auth.jwt.JwtProperties;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class AppConfig {

    /** KST 고정 Clock. 테스트에서 고정 시각으로 바꿔 끼울 수 있도록 빈으로 분리한다. */
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }

    /**
     * JPA Auditing 이 BaseTimeEntity 의 OffsetDateTime 필드를 그대로 채우도록 직접 제공한다.
     * (기본 제공자는 LocalDateTime 계열을 넘겨서 변환에 기대게 된다)
     */
    @Bean
    public DateTimeProvider auditingDateTimeProvider(Clock clock) {
        return () -> Optional.of(OffsetDateTime.now(clock));
    }
}
