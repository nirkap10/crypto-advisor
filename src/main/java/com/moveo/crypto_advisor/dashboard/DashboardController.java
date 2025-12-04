package com.moveo.crypto_advisor.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * Exposes dashboard snapshot retrieval and feedback endpoints.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Get (or create) today's dashboard snapshot for the given user.
     */
    @GetMapping("/today")
    public ResponseEntity<DashboardSnapshotResponse> today(
            Principal principal,
            @RequestParam(defaultValue = "false") boolean refresh
    ) {
        DashboardSnapshotResponse response = dashboardService.getToday(principal.getName(), refresh);
        return ResponseEntity.ok(response);
    }

    /**
     * Submit feedback for a snapshot section.
     */
    @PostMapping("/{snapshotId}/feedback")
    public ResponseEntity<DashboardFeedbackResponse> vote(
            @PathVariable Long snapshotId,
            @RequestBody DashboardFeedbackRequest request
    ) {
        DashboardFeedbackResponse response = dashboardService.vote(snapshotId, request.getSection(), request.getVote(), request.getContentId());
        return ResponseEntity.ok(response);
    }
}
