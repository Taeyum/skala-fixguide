package com.skala.fixguide.init;

import com.skala.fixguide.agent.entity.AgentCode;
import com.skala.fixguide.agent.entity.AiConfig;
import com.skala.fixguide.agent.repository.AiConfigRepository;
import com.skala.fixguide.approval.entity.Approval;
import com.skala.fixguide.approval.entity.ApprovalDecision;
import com.skala.fixguide.approval.repository.ApprovalRepository;
import com.skala.fixguide.user.entity.Role;
import com.skala.fixguide.user.entity.User;
import com.skala.fixguide.user.repository.UserRepository;
import com.skala.fixguide.workrequest.entity.ProductType;
import com.skala.fixguide.workrequest.entity.WorkRequest;
import com.skala.fixguide.workrequest.entity.WorkRequestStatus;
import com.skala.fixguide.workrequest.repository.WorkRequestRepository;
import com.skala.fixguide.workrequest.service.RequestNoGenerator;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원가입 화면이 이번 스코프에서 제외되어 로그인할 계정이 없다. FE 연동과 데모를 위해
 * 비어 있는 DB 에만 시드 계정·요청·승인 이력을 넣는다. (app.seed.enabled=false 로 끌 수 있음)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SeedDataInitializer implements ApplicationRunner {

    private static final String DEFAULT_PASSWORD = "Passw0rd!23";

    private final UserRepository userRepository;
    private final WorkRequestRepository workRequestRepository;
    private final ApprovalRepository approvalRepository;
    private final AiConfigRepository aiConfigRepository;
    private final RequestNoGenerator requestNoGenerator;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedAiConfigs();
        if (userRepository.count() > 0) {
            log.info("[seed] 이미 데이터가 있어 시드를 건너뜁니다.");
            return;
        }

        String hash = passwordEncoder.encode(DEFAULT_PASSWORD);
        User engineer = userRepository.save(User.builder()
                .name("이엔지")
                .email("engineer@fixguide.dev")
                .passwordHash(hash)
                .role(Role.ENGINEER)
                .build());
        User otherEngineer = userRepository.save(User.builder()
                .name("김현장")
                .email("engineer2@fixguide.dev")
                .passwordHash(hash)
                .role(Role.ENGINEER)
                .build());
        User safetyManager = userRepository.save(User.builder()
                .name("박안전")
                .email("safety@fixguide.dev")
                .passwordHash(hash)
                .role(Role.SAFETY_MANAGER)
                .build());

        OffsetDateTime now = OffsetDateTime.now(clock);

        WorkRequest draft = workRequestRepository.save(WorkRequest.builder()
                .requestNo(requestNoGenerator.next())
                .requester(engineer)
                .equipment("펌프 P-114")
                .line("A라인")
                .substance("H2SO4")
                .operatingCondition(Map.of("temperature", "80 ℃", "pressure", "2500 psi"))
                .productName("SS-8-VCR")
                .productType(ProductType.VALVE)
                .specJson(Map.of("pressureRating", "3000 psi"))
                .symptom("씰 누유 발생, 압력 유지 불가")
                .siteMemo("정기점검 중 확인")
                .status(WorkRequestStatus.DRAFT)
                .build());

        workRequestRepository.save(WorkRequest.builder()
                .requestNo(requestNoGenerator.next())
                .requester(engineer)
                .equipment("가스캐비닛 GC-02")
                .line("B라인")
                .substance("SiH4")
                .operatingCondition(Map.of("temperature", "25 ℃", "pressure", "150 psi"))
                .productName("VLV-SS316-1/4-NC")
                .productType(ProductType.VALVE)
                .specJson(Map.of("pressureRating", "3000 psi"))
                .symptom("가스 유량 이상, 밸브 누설 의심")
                .status(WorkRequestStatus.AI_RUNNING)
                .build());

        workRequestRepository.save(WorkRequest.builder()
                .requestNo(requestNoGenerator.next())
                .requester(engineer)
                .equipment("스크러버 SCR-01")
                .line("A라인")
                .substance("NH3")
                .operatingCondition(Map.of("temperature", "60 ℃", "pressure", "80 psi"))
                .productName("인라인 필터 FLT-2")
                .productType(ProductType.FILTER)
                .specJson(Map.of("substanceType", "NH3"))
                .status(WorkRequestStatus.AI_DONE)
                .build());

        WorkRequest pending = workRequestRepository.save(WorkRequest.builder()
                .requestNo(requestNoGenerator.next())
                .requester(engineer)
                .equipment("공정가스 밸브 V-7")
                .line("C라인")
                .substance("NH3")
                .operatingCondition(Map.of("temperature", "40 ℃", "pressure", "1200 psi"))
                .productName("REG-2S")
                .productType(ProductType.REGULATOR)
                .specJson(Map.of("pressureRating", "250 psi"))
                .engineerNote("압력 등급 상향 반영, 제38조 작업허가 필요 판단.")
                .status(WorkRequestStatus.PENDING)
                .submittedAt(now.minusHours(3))
                .build());

        WorkRequest rejected = workRequestRepository.save(WorkRequest.builder()
                .requestNo(requestNoGenerator.next())
                .requester(engineer)
                .equipment("펌프 P-208")
                .line("B라인")
                .substance("IPA")
                .operatingCondition(Map.of("temperature", "35 ℃", "pressure", "600 psi"))
                .productName("1/4 in VCR 피팅")
                .productType(ProductType.FITTING_TUBE)
                .specJson(Map.of("connectionStandard", "1/4 in VCR", "material", "SUS316L"))
                .engineerNote("동일 규격 교체 요청.")
                .status(WorkRequestStatus.REJECTED)
                .submittedAt(now.minusDays(1))
                .build());

        WorkRequest approved = workRequestRepository.save(WorkRequest.builder()
                .requestNo(requestNoGenerator.next())
                .requester(otherEngineer)
                .equipment("가스캐비닛 GC-05")
                .line("D라인")
                .substance("Cl2")
                .operatingCondition(Map.of("temperature", "25 ℃", "pressure", "200 psi"))
                .productName("씰킷 세트")
                .productType(ProductType.ETC)
                .specJson(Map.of("freeSpec", "씰킷 세트, 내열 200℃"))
                .engineerNote("소모품 정기 교체.")
                .status(WorkRequestStatus.APPROVED)
                .submittedAt(now.minusDays(2))
                .build());

        WorkRequest otherPending = workRequestRepository.save(WorkRequest.builder()
                .requestNo(requestNoGenerator.next())
                .requester(otherEngineer)
                .equipment("스크러버 SCR-03")
                .line("D라인")
                .substance("HF")
                .operatingCondition(Map.of("temperature", "70 ℃", "pressure", "90 psi"))
                .productName("FLT-HF-01")
                .productType(ProductType.FILTER)
                .specJson(Map.of("substanceType", "HF"))
                .engineerNote("차압 상승으로 교체 필요.")
                .status(WorkRequestStatus.PENDING)
                .submittedAt(now.minusHours(1))
                .build());

        // 승인 이력은 "오늘/이번 달" KPI 가 항상 값을 갖도록 실행 시각 기준 몇 분 전으로 넣는다.
        approvalRepository.saveAll(List.of(
                Approval.builder()
                        .workRequest(rejected)
                        .approver(safetyManager)
                        .decision(ApprovalDecision.REJECT)
                        .reason("규격 근거 자료 부족 — 호환표 첨부 요망")
                        .reasonCategory("규격 부적합")
                        .decidedAt(now.minusMinutes(50))
                        .build(),
                Approval.builder()
                        .workRequest(approved)
                        .approver(safetyManager)
                        .decision(ApprovalDecision.APPROVE)
                        .decidedAt(now.minusMinutes(40))
                        .build(),
                Approval.builder()
                        .workRequest(rejected)
                        .approver(safetyManager)
                        .decision(ApprovalDecision.REJECT)
                        .reason("적용 법령 조문이 누락되었습니다.")
                        .reasonCategory("법령 미충족")
                        .decidedAt(now.minusMinutes(30))
                        .build(),
                Approval.builder()
                        .workRequest(approved)
                        .approver(safetyManager)
                        .decision(ApprovalDecision.REJECT)
                        .reason("위험성평가서 서명란이 비어 있습니다.")
                        .reasonCategory("안전서류 미흡")
                        .decidedAt(now.minusMinutes(20))
                        .build(),
                Approval.builder()
                        .workRequest(rejected)
                        .approver(safetyManager)
                        .decision(ApprovalDecision.REJECT)
                        .reason("호환표 근거가 여전히 부족합니다.")
                        .reasonCategory("규격 부적합")
                        .decidedAt(now.minusMinutes(10))
                        .build()));

        log.info(
                "[seed] 완료 — users={}, workRequests={}, approvals={} (draft={}, pending={}, otherPending={})",
                userRepository.count(),
                workRequestRepository.count(),
                approvalRepository.count(),
                draft.getId(),
                pending.getId(),
                otherPending.getId());
    }

    /** ERD 8. ai_configs — A1·A2·A3 각각 provider=MOCK 으로. 실제 LLM 전환은 provider 값 변경으로 (AI-Ready) */
    private void seedAiConfigs() {
        if (aiConfigRepository.count() > 0) {
            return;
        }
        for (AgentCode code : AgentCode.values()) {
            aiConfigRepository.save(AiConfig.builder()
                    .agentCode(code)
                    .provider("MOCK")
                    .promptVersion("v1.0")
                    .egressAllowed(false)
                    .active(true)
                    .build());
        }
        log.info("[seed] ai_configs 생성 — A1·A2·A3 (provider=MOCK)");
    }
}
