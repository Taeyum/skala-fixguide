package com.skala.fixguide.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skala.fixguide.agent.repository.AgentResultRepository;
import com.skala.fixguide.agent.repository.AgentRunRepository;
import com.skala.fixguide.agent.repository.AgentStepRepository;
import com.skala.fixguide.approval.repository.ApprovalRepository;
import com.skala.fixguide.user.entity.Role;
import com.skala.fixguide.user.entity.User;
import com.skala.fixguide.user.repository.UserRepository;
import com.skala.fixguide.workrequest.repository.WorkRequestPhotoRepository;
import com.skala.fixguide.workrequest.repository.WorkRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** MockMvc 통합 테스트 공통 설정. 시드는 끄고 테스트마다 필요한 데이터만 만든다. */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
public abstract class IntegrationTestSupport {

    protected static final String PASSWORD = "Passw0rd!23";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected WorkRequestRepository workRequestRepository;

    @Autowired
    protected ApprovalRepository approvalRepository;

    @Autowired
    protected WorkRequestPhotoRepository workRequestPhotoRepository;

    @Autowired
    protected AgentRunRepository agentRunRepository;

    @Autowired
    protected AgentStepRepository agentStepRepository;

    @Autowired
    protected AgentResultRepository agentResultRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @BeforeEach
    void clearAll() {
        agentResultRepository.deleteAll();
        agentStepRepository.deleteAll();
        agentRunRepository.deleteAll();
        workRequestPhotoRepository.deleteAll();
        approvalRepository.deleteAll();
        workRequestRepository.deleteAll();
        userRepository.deleteAll();
    }

    protected User createUser(String name, String email, Role role) {
        return userRepository.save(User.builder()
                .name(name)
                .email(email)
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .role(role)
                .build());
    }
}
