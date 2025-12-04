package com.moveo.crypto_advisor.dashboard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moveo.crypto_advisor.content.Content;
import com.moveo.crypto_advisor.content.ContentRepository;
import com.moveo.crypto_advisor.content.ContentType;
import com.moveo.crypto_advisor.integrations.AssetIdResolver;
import com.moveo.crypto_advisor.preferences.PreferencesResponse;
import com.moveo.crypto_advisor.preferences.PreferencesService;
import com.moveo.crypto_advisor.user.User;
import com.moveo.crypto_advisor.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
    private final AssetIdResolver assetIdResolver;
    private final ContentRepository contentRepository;
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
    public DashboardFeedbackResponse vote(Long snapshotId, DashboardSection section, int vote, Long contentId) {
        if (vote < -1 || vote > 1) {
            throw new IllegalArgumentException("Vote must be -1, 0, or 1");
        }

        DashboardSnapshot snapshot = snapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new RuntimeException("Snapshot not found: " + snapshotId));

        Content content = null;
        if (contentId != null) {
            content = contentRepository.findById(contentId)
                    .orElseThrow(() -> new RuntimeException("Content not found: " + contentId));
        }

        DashboardFeedback feedback = new DashboardFeedback();
        feedback.setSnapshot(snapshot);
        feedback.setContent(content);
        feedback.setSection(section);
        feedback.setVote(vote);
        feedback.setCreatedAt(Instant.now());

        DashboardFeedback saved = feedbackRepository.save(feedback);
        Long savedContentId = saved.getContent() != null ? saved.getContent().getId() : null;
        return new DashboardFeedbackResponse(saved.getId(), saved.getSnapshot().getId(), savedContentId, saved.getSection(), saved.getVote(), saved.getCreatedAt());
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
                .map(this::toCoinGeckoIds)
                .filter(list -> !list.isEmpty())
                .orElseGet(assetIdResolver::getSupportedCoinGeckoIds);

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Map<String, Object> newsByAsset = new LinkedHashMap<>();
        Map<String, Object> pricesByAsset = new LinkedHashMap<>();
        Map<String, Object> memesByAsset = new LinkedHashMap<>();
        Map<String, Object> aiByAsset = new LinkedHashMap<>();
        Map<String, Object> sharedNewsFallback = new LinkedHashMap<>();

        for (String asset : assets) {
            latestForToday(ContentType.NEWS, asset, today)
                    .ifPresent(c -> newsByAsset.put(asset, withContentId(c)));
            latestForToday(ContentType.PRICE, asset, today)
                    .ifPresent(c -> pricesByAsset.put(asset, withContentId(c)));
            latestForToday(ContentType.MEME, asset, today)
                    .ifPresent(c -> memesByAsset.put(asset, withContentId(c)));
            latestForToday(ContentType.AI_INSIGHT, asset, today)
                    .ifPresent(c -> aiByAsset.put(asset, withContentId(c)));
        }

        // If a coin has no news for today, fall back to the latest news for any coin.
        if (newsByAsset.isEmpty()) {
            latestAny(ContentType.NEWS).ifPresent(c -> {
                Object wrapped = withContentId(c);
                assets.forEach(asset -> sharedNewsFallback.put(asset, wrapped));
            });
        } else {
            latestAny(ContentType.NEWS).ifPresent(c -> sharedNewsFallback.put("fallback", withContentId(c)));
        }
        for (String asset : assets) {
            if (!newsByAsset.containsKey(asset) && !sharedNewsFallback.isEmpty()) {
                newsByAsset.put(asset, sharedNewsFallback.values().iterator().next());
            }
        }

        snapshot.setMarketNewsJson(writeJson(newsByAsset));
        snapshot.setCoinPricesJson(writeJson(pricesByAsset));
        snapshot.setAiInsightJson(writeJson(aiByAsset));
        snapshot.setMemeJson(writeJson(memesByAsset));
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

    // Translate user-selected tickers to CoinGecko ids, dropping unknowns.
    private List<String> toCoinGeckoIds(List<String> tickers) {
        return tickers.stream()
                .map(assetIdResolver::toCoinGeckoId)
                .filter(Objects::nonNull)
                .toList();
    }

    private Optional<Content> latestForToday(ContentType type, String assetId, LocalDate today) {
        return contentRepository.findTopByTypeAndCryptoAssetOrderByTimestampDesc(type, assetId)
                .filter(content -> content.getTimestamp() != null &&
                        content.getTimestamp().atZone(ZoneOffset.UTC).toLocalDate().equals(today));
    }

    private Optional<Content> latestAny(ContentType type) {
        return contentRepository.findTopByTypeOrderByTimestampDesc(type);
    }

    private Map<String, Object> withContentId(Content content) {
        Map<String, Object> data = readJson(content.getContent());
        Map<String, Object> wrapped = new LinkedHashMap<>();
        wrapped.put("contentId", content.getId());
        wrapped.put("data", data);
        return wrapped;
    }
}
