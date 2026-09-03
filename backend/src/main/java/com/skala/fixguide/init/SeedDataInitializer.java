package com.skala.fixguide.init;

import com.skala.fixguide.agent.entity.AgentCode;
import com.skala.fixguide.agent.entity.AgentResult;
import com.skala.fixguide.agent.entity.AgentRun;
import com.skala.fixguide.agent.entity.AgentStep;
import com.skala.fixguide.agent.entity.AiConfig;
import com.skala.fixguide.agent.repository.AgentResultRepository;
import com.skala.fixguide.agent.repository.AgentRunRepository;
import com.skala.fixguide.agent.repository.AgentStepRepository;
import com.skala.fixguide.agent.repository.AiConfigRepository;
import com.skala.fixguide.agent.service.MockAgentEngine;
import com.skala.fixguide.approval.entity.Approval;
import com.skala.fixguide.approval.entity.ApprovalDecision;
import com.skala.fixguide.approval.repository.ApprovalRepository;
import com.skala.fixguide.user.entity.Role;
import com.skala.fixguide.user.entity.User;
import com.skala.fixguide.user.repository.UserRepository;
import com.skala.fixguide.workrequest.entity.ProductType;
import com.skala.fixguide.workrequest.entity.WorkRequest;
import com.skala.fixguide.workrequest.entity.WorkRequestPhoto;
import com.skala.fixguide.workrequest.entity.WorkRequestStatus;
import com.skala.fixguide.workrequest.repository.WorkRequestPhotoRepository;
import com.skala.fixguide.workrequest.repository.WorkRequestRepository;
import com.skala.fixguide.workrequest.service.RequestNoGenerator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * FE 연동·데모·수동 테스트용 시드. 비어 있는 DB 에만 계정·요청·AI 실행 이력·결과·사진·승인 이력을 넣는다.
 * 명세서 16개 API 를 별도 준비 없이 바로 호출해 볼 수 있도록 모든 상태의 요청을 하나 이상 만든다.
 * (app.seed.enabled=false 로 끌 수 있음. 다시 넣으려면 docker compose down -v 후 재기동)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SeedDataInitializer implements ApplicationRunner {

    private static final String DEFAULT_PASSWORD = "Passw0rd!23";

    /** 1x1 PNG. 사진 업로드·목록·정적 서빙을 시드만으로 확인할 수 있게 실제 파일을 하나 써 둔다. */
    private static final byte[] SAMPLE_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAACQd1PeAAAADElEQVR4nGP4z8AAAAMBAQBb1S7RAAAAAElFTkSuQmCC");

    private final UserRepository userRepository;
    private final WorkRequestRepository workRequestRepository;
    private final ApprovalRepository approvalRepository;
    private final AiConfigRepository aiConfigRepository;
    private final AgentRunRepository agentRunRepository;
    private final AgentStepRepository agentStepRepository;
    private final AgentResultRepository agentResultRepository;
    private final WorkRequestPhotoRepository photoRepository;
    private final MockAgentEngine engine;
    private final RequestNoGenerator requestNoGenerator;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

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

        WorkRequest aiRunning = workRequestRepository.save(WorkRequest.builder()
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

        WorkRequest aiDone = workRequestRepository.save(WorkRequest.builder()
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

        // AI 실행 이력 — AI_RUNNING 은 폴링(API 12)을 이어서 해볼 수 있게 1/3 만 완료, 나머지는 3종 모두 완료
        seedAgentRun(aiRunning, 1, now.minusMinutes(2));
        seedAgentRun(aiDone, 3, now.minusMinutes(30));
        seedAgentRun(pending, 3, now.minusHours(4));
        seedAgentRun(rejected, 3, now.minusDays(1).minusHours(1));
        seedAgentRun(approved, 3, now.minusDays(2).minusHours(1));
        seedAgentRun(otherPending, 3, now.minusHours(2));

        // 제품 사진 — DRAFT 요청에 2장 (API 10 목록 · /api/v1/files/** 정적 서빙 확인용)
        seedPhotos(draft, now.minusMinutes(5));

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
                "[seed] 완료 — users={}, workRequests={}, agentRuns={}, photos={}, approvals={}",
                userRepository.count(),
                workRequestRepository.count(),
                agentRunRepository.count(),
                photoRepository.count(),
                approvalRepository.count());
        log.info("[seed] 요청 id — draft={}, aiRunning={}, aiDone={}, pending={}, rejected={}, approved={}",
                draft.getId(), aiRunning.getId(), aiDone.getId(), pending.getId(), rejected.getId(), approved.getId());
    }

    /**
     * AI 검증 run 1건 + step 3개 + 완료된 step 수만큼 결과. doneSteps 가 3 이면 run 도 DONE.
     * 실제 API 12(폴링)가 만드는 데이터와 같은 모양이라 상세(API 7)·제출(API 14) 검증을 그대로 통과한다.
     */
    private void seedAgentRun(WorkRequest wr, int doneSteps, OffsetDateTime startedAt) {
        AgentRun run = agentRunRepository.save(AgentRun.builder()
                .workRequest(wr)
                .startedAt(startedAt)
                .inputSnapshot(Map.of(
                        "workRequestId", wr.getId().toString(),
                        "equipment", String.valueOf(wr.getEquipment()),
                        "productName", String.valueOf(wr.getProductName())))
                .aiConfig(aiConfigRepository.findByAgentCodeAndActiveTrue(AgentCode.A1).orElse(null))
                .build());

        List<AgentStep> steps = new ArrayList<>();
        AgentCode[] codes = AgentCode.values();
        for (int i = 0; i < codes.length; i++) {
            AgentStep step = AgentStep.builder().run(run).agentCode(codes[i]).build();
            OffsetDateTime stepTime = startedAt.plusSeconds(10L * (i + 1));
            if (i < doneSteps) {
                step.done(stepTime, engine.doneMessage(codes[i], wr));
                agentResultRepository.save(AgentResult.builder()
                        .run(run)
                        .agentCode(codes[i])
                        .payloadJson(engine.payload(codes[i], wr))
                        .build());
            } else if (i == doneSteps) {
                step.start(stepTime, engine.runningMessage(codes[i]));
            }
            steps.add(step);
        }
        agentStepRepository.saveAll(steps);
        if (doneSteps >= codes.length) {
            run.finish(startedAt.plusSeconds(40));
        }
    }

    private void seedPhotos(WorkRequest wr, OffsetDateTime uploadedAt) {
        for (String name : List.of("valve-front.png", "valve-side.png")) {
            UUID photoId = UUID.randomUUID();
            String storageKey = wr.getId() + "/" + photoId + ".png";
            try {
                Path target = Paths.get(uploadDir).toAbsolutePath().resolve(storageKey);
                Files.createDirectories(target.getParent());
                Files.write(target, SAMPLE_PNG);
            } catch (IOException e) {
                log.warn("[seed] 샘플 사진 파일을 쓰지 못했습니다: {}", e.getMessage());
                return;
            }
            photoRepository.save(WorkRequestPhoto.builder()
                    .id(photoId)
                    .workRequest(wr)
                    .fileName(name)
                    .storageKey(storageKey)
                    .thumbnailKey(storageKey)
                    .size(SAMPLE_PNG.length)
                    .uploadedAt(uploadedAt)
                    .build());
        }
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
