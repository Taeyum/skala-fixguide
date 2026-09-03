package com.skala.fixguide.dashboard.controller;

import com.skala.fixguide.auth.jwt.AuthenticatedUser;
import com.skala.fixguide.dashboard.dto.DashboardSummaryResponse;
import com.skala.fixguide.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Dashboard", description = "메인화면 KPI (화면 WRA_E_01 · WRA_S_01)")
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "역할별 대시보드 요약", description = "role 쿼리는 토큰 역할과 일치해야 한다.")
    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> summary(
            @AuthenticationPrincipal AuthenticatedUser me, @RequestParam(name = "role") String role) {
        return ResponseEntity.ok(dashboardService.summary(me, role));
    }
}
