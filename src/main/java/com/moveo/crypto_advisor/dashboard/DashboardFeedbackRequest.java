package com.moveo.crypto_advisor.dashboard;

import lombok.Data;

/**
 * Request payload for a dashboard vote.
 */
@Data
public class DashboardFeedbackRequest {
    private DashboardSection section;
    private int vote; // -1, 0, or 1
}
