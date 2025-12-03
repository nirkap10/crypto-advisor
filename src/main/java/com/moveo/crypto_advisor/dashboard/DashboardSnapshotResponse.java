package com.moveo.crypto_advisor.dashboard;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.Map;

/**
 * API response shape for a dashboard snapshot plus latest votes per section.
 */
@Data
@Builder
public class DashboardSnapshotResponse {
    private Long id;
    private LocalDate snapshotDate;
    private Object marketNews;
    private Object coinPrices;
    private Object aiInsight;
    private Object meme;
    private Map<DashboardSection, Integer> votes;
}
