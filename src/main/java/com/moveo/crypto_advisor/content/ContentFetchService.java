package com.moveo.crypto_advisor.content;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moveo.crypto_advisor.integrations.AssetIdResolver;
import com.moveo.crypto_advisor.integrations.CoinGeckoService;
import com.moveo.crypto_advisor.integrations.CryptoPanicService;
import com.moveo.crypto_advisor.integrations.MemeService;
import com.moveo.crypto_advisor.integrations.AIInsightService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Fetches external content once per day and stores it for reuse across users.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContentFetchService {

    private final CryptoPanicService cryptoPanicService;
    private final CoinGeckoService coinGeckoService;
    private final MemeService memeService;
    private final AIInsightService aiInsightService;
    private final AssetIdResolver assetIdResolver;
    private final ContentRepository contentRepository;
    private final ObjectMapper objectMapper;

    /**
     * Trigger a refresh at startup so there is data on day one (idempotent per day).
     */
    @PostConstruct
    public void preloadOnStartup() {
        try {
            refreshDailyContent();
        } catch (Exception e) {
            log.warn("Failed to refresh content on startup", e);
        }
    }

    /**
     * Run shortly after midnight UTC to refresh daily content.
     */
    @Scheduled(cron = "0 5 0 * * *", zone = "UTC")
    public void refreshDailyContent() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant now = Instant.now();
        Instant startOfDayUtc = today.atStartOfDay(ZoneOffset.UTC).toInstant();

        List<String> coinGeckoIds = assetIdResolver.getSupportedCoinGeckoIds();
        if (coinGeckoIds.isEmpty()) {
            log.warn("No supported CoinGecko ids found; skipping daily content refresh");
            return;
        }

        Map<String, Object> prices = coinGeckoService.fetchSimplePrices(coinGeckoIds, "usd");
        Map<String, Object> news = cryptoPanicService.fetchLatest(Optional.empty());
        Map<String, Object> meme = memeService.getMeme();
        for (String assetId : coinGeckoIds) {
            Object pricePayload = prices.get(assetId);
            persistIfMissing(ContentType.PRICE, assetId, pricePayload, now, startOfDayUtc);
            persistIfMissing(ContentType.NEWS, assetId, news, now, startOfDayUtc);
            persistIfMissing(ContentType.MEME, assetId, meme, now, startOfDayUtc);

            Map<String, Object> aiContext = Map.of(
                    "price", pricePayload,
                    "news", news,
                    "meme", meme
            );
            Map<String, Object> aiInsight = aiInsightService.generateInsightForAsset(assetId, aiContext);
            persistIfMissing(ContentType.AI_INSIGHT, assetId, aiInsight, now, startOfDayUtc);
        }
    }

    private void persistIfMissing(ContentType type, String assetId, Object payload, Instant timestamp, Instant startOfDayUtc) {
        if (payload == null) {
            return;
        }

        if (type == ContentType.NEWS && payload instanceof Map<?, ?> map && map.containsKey("error")) {
            log.warn("Skipping news persistence for {} due to error payload", assetId);
            return;
        }

        boolean existsToday = contentRepository.existsByTypeAndCryptoAssetAndTimestampAfter(type, assetId, startOfDayUtc);

        // Allow replacing today's MEME if the stored one is a fallback and we have a new candidate.
        if (existsToday && type == ContentType.MEME) {
            contentRepository.findTopByTypeAndCryptoAssetOrderByTimestampDesc(type, assetId)
                    .ifPresent(existing -> {
                        if (isFallbackMeme(existing.getContent()) && isValidMemePayload(payload)) {
                            existing.setContent(writeJson(payload));
                            existing.setTimestamp(timestamp);
                            contentRepository.save(existing);
                        }
                    });
            return;
        }

        // Always replace today's AI insight with the latest generated content.
        if (existsToday && type == ContentType.AI_INSIGHT) {
            contentRepository.findTopByTypeAndCryptoAssetOrderByTimestampDesc(type, assetId)
                    .ifPresent(existing -> {
                        existing.setContent(writeJson(payload));
                        existing.setTimestamp(timestamp);
                        contentRepository.save(existing);
                    });
            return;
        }

        if (existsToday) {
            return;
        }

        Content content = new Content();
        content.setType(type);
        content.setCryptoAsset(assetId);
        content.setContent(writeJson(payload));
        content.setTimestamp(timestamp);
        contentRepository.save(content);
    }

    @SuppressWarnings("unchecked")
    private boolean isFallbackMeme(String contentJson) {
        try {
            Map<String, Object> data = objectMapper.readValue(contentJson, Map.class);
            Object source = data.get("source");
            Object url = data.get("url");
            return "fallback".equals(source) || (url instanceof String u && u.startsWith("data:image/svg"));
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isValidMemePayload(Object payload) {
        if (payload instanceof Map<?, ?> map) {
            Object url = map.get("url");
            return url != null && url.toString().length() > 5;
        }
        return false;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize content payload", e);
        }
    }
}
