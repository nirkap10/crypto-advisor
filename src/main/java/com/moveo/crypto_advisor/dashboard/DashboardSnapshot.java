package com.moveo.crypto_advisor.dashboard;

import com.moveo.crypto_advisor.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Stores a daily dashboard snapshot per user, holding the content that was shown for that day.
 */
@Getter
@Setter
@Entity
@Table(
        name = "dashboard_snapshots",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "snapshot_date"})
)
public class DashboardSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Owner of this snapshot
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    // Which day this snapshot represents; used to ensure one per user per day
    @Column(name = "snapshot_date")
    private LocalDate snapshotDate;

    // Raw content blobs we returned to the user (JSON text for flexibility)
    @Column(columnDefinition = "TEXT")
    private String marketNewsJson;

    @Column(columnDefinition = "TEXT")
    private String coinPricesJson;

    @Column(columnDefinition = "TEXT")
    private String aiInsightJson;

    @Column(columnDefinition = "TEXT")
    private String memeJson;

    @Column(name = "created_at")
    private Instant createdAt;
}
