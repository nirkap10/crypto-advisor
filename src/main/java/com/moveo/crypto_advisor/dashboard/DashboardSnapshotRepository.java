package com.moveo.crypto_advisor.dashboard;

import com.moveo.crypto_advisor.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

/**
 * JPA access for dashboard snapshots.
 */
public interface DashboardSnapshotRepository extends JpaRepository<DashboardSnapshot, Long> {

    Optional<DashboardSnapshot> findByUserAndSnapshotDate(User user, LocalDate snapshotDate);
}
