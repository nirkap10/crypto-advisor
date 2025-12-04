package com.moveo.crypto_advisor.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

/**
 * API response shape for submitted feedback.
 */
@Data
@AllArgsConstructor
public class DashboardFeedbackResponse {
    private Long id;
    private Long snapshotId;
    private Long contentId;
    private DashboardSection section;
    private int vote;
    private Instant createdAt;
}
