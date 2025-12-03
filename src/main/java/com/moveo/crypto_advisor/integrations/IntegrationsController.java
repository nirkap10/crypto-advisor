package com.moveo.crypto_advisor.integrations;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST endpoints that expose the external integrations to the frontend.
 * - /news proxies CryptoPanic
 * - /prices proxies CoinGecko simple price API
 */
@RestController
@RequestMapping("/api/integrations")
@RequiredArgsConstructor
public class IntegrationsController {

    // Business-layer clients for third-party APIs
    private final CryptoPanicService cryptoPanicService;
    private final CoinGeckoService coinGeckoService;

    /**
     * Retrieve latest CryptoPanic posts. Optional "kind" filters by news/media.
     */
    @GetMapping("/news")
    public ResponseEntity<Map<String, Object>> latestNews(
            @RequestParam(required = false) String kind
    ) {
        Map<String, Object> payload = cryptoPanicService.fetchLatest(Optional.ofNullable(kind));
        return ResponseEntity.ok(payload);
    }

    /**
     * Fetch simple price data for a comma-separated list of coin IDs (CoinGecko ids).
     */
    @GetMapping("/prices")
    public ResponseEntity<Map<String, Object>> simplePrices(
            @RequestParam String ids,
            @RequestParam(defaultValue = "usd") String vsCurrency
    ) {
        List<String> coinIds = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        Map<String, Object> payload = coinGeckoService.fetchSimplePrices(coinIds, vsCurrency);
        return ResponseEntity.ok(payload);
    }
}
