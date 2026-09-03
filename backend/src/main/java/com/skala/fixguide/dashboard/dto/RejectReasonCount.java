package com.skala.fixguide.dashboard.dto;

/** 안전관리자 대시보드 "거절 사유 TOP5" 항목 (AC 7-3) */
public record RejectReasonCount(String category, Long count) {
}
