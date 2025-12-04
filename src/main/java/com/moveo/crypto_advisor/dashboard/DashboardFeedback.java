package com.moveo.crypto_advisor.dashboard;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import com.moveo.crypto_advisor.content.Content;

/**
 * Records user feedback (thumbs) for a specific section of a snapshot.
 * Append-only so we keep a history of feedback changes.
 */
@Getter
@Setter
@Entity
@Table(name = "dashboard_feedback")
public class DashboardFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "snapshot_id")
    private DashboardSnapshot snapshot;

    @ManyToOne
    @JoinColumn(name = "content_id")
    private Content content;

    // Section being voted on
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private DashboardSection section;

    // Vote scale: -1 (down), 0 (neutral), 1 (up)
    private int vote;

    @Column(name = "created_at")
    private Instant createdAt;
}
