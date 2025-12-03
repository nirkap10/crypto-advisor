package com.moveo.crypto_advisor.integrations;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CryptoPanicService {

    // HTTP client injected by Spring
    private final RestTemplate restTemplate;

    // Configured via application properties; defaults can be overridden with env vars
    @Value("${cryptopanic.api.key}")
    private String apiKey;

    @Value("${cryptopanic.api.base-url}")
    private String baseUrl;

    /**
     * Fetch latest news posts from CryptoPanic. Optionally filter by kind (e.g., "news" or "media").
     * Returns the raw JSON as a Map so the frontend can shape it as needed.
     */
    public Map<String, Object> fetchLatest(Optional<String> kind) {
        URI uri = UriComponentsBuilder
                .fromHttpUrl(baseUrl)
                // Use trailing slash because CryptoPanic API expects /posts/ rather than /posts
                .path("/posts/")
                .queryParam("auth_token", apiKey)
                .queryParamIfPresent("kind", kind)
                .build(true) // keep encoded query params intact
                .toUri();

        try {
            ResponseEntity<Map> response = restTemplate.exchange(uri, HttpMethod.GET, null, Map.class);
            //noinspection unchecked
            return response.getBody();
        } catch (RestClientException ex) {
            // Bubble up a minimal error payload instead of throwing to the caller
            return Map.of(
                    "error", "Failed to fetch CryptoPanic posts",
                    "details", ex.getMessage()
            );
        }
    }
}
