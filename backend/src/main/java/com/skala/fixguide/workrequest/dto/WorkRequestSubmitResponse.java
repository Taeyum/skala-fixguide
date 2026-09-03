package com.skala.fixguide.workrequest.dto;

import com.skala.fixguide.workrequest.entity.WorkRequestStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record WorkRequestSubmitResponse(UUID workRequestId, WorkRequestStatus status, OffsetDateTime submittedAt) {
}
