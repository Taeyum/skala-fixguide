package com.skala.fixguide;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class FixGuideApplication {

    /** API 명세서 시각 규약(ISO 8601 + KST 오프셋)을 맞추기 위해 JVM 기본 시간대를 고정한다. */
    @PostConstruct
    void setDefaultTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }

    public static void main(String[] args) {
        SpringApplication.run(FixGuideApplication.class, args);
    }
}
