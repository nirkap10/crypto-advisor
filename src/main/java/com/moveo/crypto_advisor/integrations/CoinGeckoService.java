package com.moveo.crypto_advisor.integrations;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

@Service
@RequiredArgsConstructor
public class CoinGeckoService {

    // HTTP client injected by Spring
    private final RestTemplate restTemplate;

    // Configured via application properties; defaults can be overridden with env vars
    @Value("${coingecko.api.key}")
    private String apiKey;

    @Value("${coingecko.api.base-url}")
    private String baseUrl;

    /**
     * Fetch simple price data for a list of coin ids against a single fiat currency (e.g., "usd").
     * Returns the raw JSON as a Map so the frontend can shape it as needed.
     */
    public Map<String, Object> fetchSimplePrices(List<String> coinIds, String vsCurrency) {
        StringJoiner idJoiner = new StringJoiner(",");
        coinIds.forEach(idJoiner::add);

        URI uri = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/simple/price")
                .queryParam("x_cg_demo_api_key", apiKey)
                .queryParam("ids", idJoiner.toString())
                .queryParam("vs_currencies", vsCurrency)
                .build()
                .toUri();

        ResponseEntity<Map> response = restTemplate.exchange(uri, HttpMethod.GET, null, Map.class);
        //noinspection unchecked
        return response.getBody();
    }
}
