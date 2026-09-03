package com.skala.fixguide.workrequest.service;

import com.skala.fixguide.workrequest.repository.WorkRequestRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 업무 번호 WR-YYYYMMDD-NNN 채번 (ERD 변경 #1). 데모 규모 기준이라 동시성 제어는 하지 않는다. */
@Component
@RequiredArgsConstructor
public class RequestNoGenerator {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final WorkRequestRepository workRequestRepository;
    private final Clock clock;

    public String next() {
        String prefix = "WR-" + LocalDate.now(clock).format(DATE) + "-";
        int next = workRequestRepository.findTopByRequestNoStartingWithOrderByRequestNoDesc(prefix)
                .map(w -> Integer.parseInt(w.getRequestNo().substring(prefix.length())) + 1)
                .orElse(1);
        return prefix + String.format("%03d", next);
    }
}
