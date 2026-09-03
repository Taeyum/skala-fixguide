package com.skala.argos.common;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/** 명세 1장: 시각 포맷 ISO 8601 · KST 오프셋 포함 */
public final class KstTime {

    public static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private KstTime() {
    }

    public static OffsetDateTime now() {
        return OffsetDateTime.now(KST);
    }

    public static OffsetDateTime of(Instant instant) {
        return instant == null ? null : instant.atZone(KST).toOffsetDateTime();
    }
}
