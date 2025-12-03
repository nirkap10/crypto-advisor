package com.moveo.crypto_advisor.integrations;

import com.moveo.crypto_advisor.preferences.PreferencesResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates an AI insight message using Hugging Face; falls back to a static blurb on failure.
 */
@Service
@RequiredArgsConstructor
public class AIInsightService {

    private final HuggingFaceClient huggingFaceClient;

    public Map<String, Object> generateInsight(PreferencesResponse prefs) {
        String investorType = prefs != null ? prefs.getInvestorType() : null;
        List<String> assets = prefs != null ? prefs.getCryptoAssets() : null;
        String primaryAsset = (assets != null && !assets.isEmpty()) ? assets.getFirst() : "bitcoin";
        String persona = investorType != null ? investorType : "HODLER";

        String prompt = buildPrompt(persona, primaryAsset, assets);
        String summary = huggingFaceClient.generate(prompt)
                .flatMap(this::extractText)
                .orElseGet(() -> pickFallback(persona, primaryAsset));

        String headline = "Daily insight for a " + persona;
        return Map.of(
                "date", LocalDate.now().toString(),
                "headline", headline,
                "summary", summary,
                "assetFocus", primaryAsset,
                "source", "huggingface"
        );
    }

    private String buildPrompt(String persona, String primaryAsset, List<String> assets) {
        String assetList = (assets != null && !assets.isEmpty()) ? String.join(", ", assets) : primaryAsset;
        return """
                You are a concise crypto assistant. Provide one short market insight (max 80 words).
                Persona: %s
                Focus assets: %s
                Tone: practical, cautious, no investment advice language, no emojis.
                Output only the insight sentence(s), no preamble.
                """.formatted(persona, assetList);
    }

    private Optional<String> extractText(String hfResponse) {
        if (hfResponse == null || hfResponse.isBlank()) return Optional.empty();
        String trimmed = hfResponse.trim();
        // If it looks like JSON array of objects with "generated_text", try to pull it.
        if (trimmed.startsWith("[") && trimmed.contains("generated_text")) {
            int idx = trimmed.indexOf("generated_text");
            int colon = trimmed.indexOf(':', idx);
            if (colon > 0) {
                int quoteStart = trimmed.indexOf('"', colon);
                int quoteEnd = trimmed.indexOf('"', quoteStart + 1);
                if (quoteStart > 0 && quoteEnd > quoteStart) {
                    return Optional.of(trimmed.substring(quoteStart + 1, quoteEnd));
                }
            }
        }
        return Optional.of(trimmed);
    }

    private String pickFallback(String persona, String asset) {
        List<String> ideas = List.of(
                "Watch intraday volatility; momentum above the 20-day trend looks constructive.",
                "Range-trading regime; consider staggered buys near support.",
                "High funding rates suggest caution on leveraged longs.",
                "On-chain activity is rising; keep an eye on network fees.",
                "Liquidity pockets sit just above recent highs—potential squeeze fuel."
        );
        String base = ideas.get(ThreadLocalRandom.current().nextInt(ideas.size()));
        return persona + " note on " + asset + ": " + base;
    }
}
