package com.moveo.crypto_advisor.dashboard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moveo.crypto_advisor.integrations.CoinGeckoService;
import com.moveo.crypto_advisor.integrations.CryptoPanicService;
import com.moveo.crypto_advisor.integrations.AIInsightService;
import com.moveo.crypto_advisor.integrations.MemeService;
import com.moveo.crypto_advisor.preferences.PreferencesResponse;
import com.moveo.crypto_advisor.preferences.PreferencesService;
import com.moveo.crypto_advisor.user.User;
import com.moveo.crypto_advisor.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/**
 * Coordinates creation and retrieval of daily dashboard snapshots and feedback recording.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardSnapshotRepository snapshotRepository;
    private final DashboardFeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final PreferencesService preferencesService;
    private final CryptoPanicService cryptoPanicService;
    private final CoinGeckoService coinGeckoService;
    private final AIInsightService aiInsightService;
    private final MemeService memeService;
    private final ObjectMapper objectMapper;

    /**
     * Retrieve today's snapshot for the user, creating one if missing.
     */
    public DashboardSnapshotResponse getToday(String username) {
        return getToday(username, false);
    }

    /**
     * Retrieve today's snapshot, optionally refreshing the content from external sources.
     */
    public DashboardSnapshotResponse getToday(String username, boolean refresh) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        LocalDate today = LocalDate.now();
        PreferencesResponse prefs = preferencesService.getForUser(user.getUsername())
                .orElseGet(PreferencesResponse::new);

        DashboardSnapshot snapshot = snapshotRepository
                .findByUserAndSnapshotDate(user, today)
                .orElseGet(() -> createSnapshot(user, today, prefs));

        if (refresh) {
            populateSnapshotContent(snapshot, prefs);
            snapshotRepository.save(snapshot);
        }

        return toResponse(snapshot);
    }

    /**
     * Record a vote for a snapshot section. Keeps history by inserting a new feedback row.
     */
    public DashboardFeedbackResponse vote(Long snapshotId, DashboardSection section, int vote) {
        if (vote < -1 || vote > 1) {
            throw new IllegalArgumentException("Vote must be -1, 0, or 1");
        }

        DashboardSnapshot snapshot = snapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new RuntimeException("Snapshot not found: " + snapshotId));

        DashboardFeedback feedback = new DashboardFeedback();
        feedback.setSnapshot(snapshot);
        feedback.setSection(section);
        feedback.setVote(vote);
        feedback.setCreatedAt(Instant.now());

        DashboardFeedback saved = feedbackRepository.save(feedback);
        return new DashboardFeedbackResponse(saved.getId(), saved.getSnapshot().getId(), saved.getSection(), saved.getVote(), saved.getCreatedAt());
    }

    // Create a new snapshot using preferences and current external data.
    private DashboardSnapshot createSnapshot(User user, LocalDate snapshotDate, PreferencesResponse prefs) {
        DashboardSnapshot snapshot = new DashboardSnapshot();
        snapshot.setUser(user);
        snapshot.setSnapshotDate(snapshotDate);
        populateSnapshotContent(snapshot, prefs);
        snapshot.setCreatedAt(Instant.now());

        return snapshotRepository.save(snapshot);
    }

    // Populate snapshot content from external sources based on preferences.
    private void populateSnapshotContent(DashboardSnapshot snapshot, PreferencesResponse prefs) {
        List<String> assets = Optional.ofNullable(prefs.getCryptoAssets())
                .filter(list -> !list.isEmpty())
                .orElse(List.of("bitcoin", "ethereum"));

        Map<String, Object> news = cryptoPanicService.fetchLatest(Optional.empty());
        Map<String, Object> prices = coinGeckoService.fetchSimplePrices(assets, "usd");
        Map<String, Object> aiInsight = aiInsightService.generateInsight(prefs);
        Map<String, Object> meme = memeService.getMeme();

        snapshot.setMarketNewsJson(writeJson(news));
        snapshot.setCoinPricesJson(writeJson(prices));
        snapshot.setAiInsightJson(writeJson(aiInsight));
        snapshot.setMemeJson(writeJson(meme));
    }

    // Map entity to response, parsing JSON blobs.
    private DashboardSnapshotResponse toResponse(DashboardSnapshot snapshot) {
        Map<DashboardSection, Integer> latestVotes = loadLatestVotes(snapshot);

        return DashboardSnapshotResponse.builder()
                .id(snapshot.getId())
                .snapshotDate(snapshot.getSnapshotDate())
                .marketNews(readJson(snapshot.getMarketNewsJson()))
                .coinPrices(readJson(snapshot.getCoinPricesJson()))
                .aiInsight(readJson(snapshot.getAiInsightJson()))
                .meme(readJson(snapshot.getMemeJson()))
                .votes(latestVotes)
                .build();
    }

    private Map<DashboardSection, Integer> loadLatestVotes(DashboardSnapshot snapshot) {
        Map<DashboardSection, Integer> votes = new EnumMap<>(DashboardSection.class);
        for (DashboardSection section : DashboardSection.values()) {
            feedbackRepository.findTopBySnapshotAndSectionOrderByCreatedAtDesc(snapshot, section)
                    .ifPresent(feedback -> votes.put(section, feedback.getVote()));
        }
        return votes;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize snapshot content", e);
        }
    }

    private Map<String, Object> readJson(String json) {
        if (json == null) {
            return Map.of();
        }
        try {
            //noinspection unchecked
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return Map.of("error", "Failed to parse stored content");
        }
    }
}
