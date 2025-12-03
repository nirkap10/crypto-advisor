package com.moveo.crypto_advisor.dashboard;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * JPA access for snapshot feedback.
 */
public interface DashboardFeedbackRepository extends JpaRepository<DashboardFeedback, Long> {

    Optional<DashboardFeedback> findTopBySnapshotAndSectionOrderByCreatedAtDesc(DashboardSnapshot snapshot, DashboardSection section);
}
