package com.skala.argos.config;

import com.skala.argos.domain.AgentCode;
import com.skala.argos.domain.AiConfig;
import com.skala.argos.domain.User;
import com.skala.argos.domain.UserRole;
import com.skala.argos.repository.AiConfigRepository;
import com.skala.argos.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 데모용 시드 데이터. 회원가입(auth 파트) 합류 전까지 X-User-Id 헤더에 아래 고정 ID를 사용한다.
 * - 엔지니어:    00000000-0000-0000-0000-000000000001
 * - 안전관리자:  00000000-0000-0000-0000-000000000002
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    public static final UUID ENGINEER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID MANAGER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private final UserRepository userRepository;
    private final AiConfigRepository aiConfigRepository;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            userRepository.save(new User(ENGINEER_ID, "이엔지", "engineer@fixguide.local",
                    "{seed}", UserRole.ENGINEER));
            userRepository.save(new User(MANAGER_ID, "박안전", "safety@fixguide.local",
                    "{seed}", UserRole.SAFETY_MANAGER));
            log.info("시드 사용자 생성 — 엔지니어 {}, 안전관리자 {}", ENGINEER_ID, MANAGER_ID);
        }
        if (aiConfigRepository.count() == 0) {
            for (AgentCode code : AgentCode.values()) {
                AiConfig config = new AiConfig();
                config.setId(UUID.randomUUID());
                config.setAgentCode(code);
                config.setProvider("MOCK");   // 실제 LLM 전환은 provider 값 변경으로 (ERD 8장)
                config.setPromptVersion("v1.0");
                config.setEgressAllowed(false);
                config.setActive(true);
                aiConfigRepository.save(config);
            }
            log.info("시드 ai_configs 생성 — A1·A2·A3 (provider=MOCK)");
        }
    }
}
