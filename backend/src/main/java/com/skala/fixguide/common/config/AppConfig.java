package com.skala.fixguide.common.config;

import com.skala.fixguide.auth.jwt.JwtProperties;
import java.time.Clock;
import java.time.ZoneId;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class AppConfig {

    /** KST 고정 Clock. 테스트에서 고정 시각으로 바꿔 끼울 수 있도록 빈으로 분리한다. */
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }
}
