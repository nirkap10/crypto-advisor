package com.moveo.crypto_advisor.integrations;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Supplies a meme payload, preferring Reddit (r/cryptomemes) and falling back to a static list.
 */
@Service
public class MemeService {

    private static final String REDDIT_URL = "https://www.reddit.com/r/cryptomemes/top.json?limit=50&t=day";

    private final RestTemplate restTemplate;

    private final List<Map<String, String>> fallbackMemes = List.of(
            Map.of("title", "HODL vibes", "url", "https://i.imgur.com/0v6jJ7f.jpeg"),
            Map.of("title", "When the dip dips", "url", "https://i.imgur.com/1J8Zq0K.jpeg"),
            Map.of("title", "Charts at 3am", "url", "https://i.imgur.com/Us0TJzQ.jpeg"),
            Map.of("title", "Bear to Bull", "url", "https://i.imgur.com/2kDOsmN.png"),
            Map.of("title", "GM, anon", "url", "https://i.imgur.com/LrXoQ0k.jpeg")
    );

    public MemeService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Map<String, Object> getMeme() {
        return fetchFromReddit()
                .orElseGet(this::randomFallback);
    }

    private Optional<Map<String, Object>> fetchFromReddit() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "crypto-advisor/1.0 (demo)");

            ResponseEntity<Map> response = restTemplate.exchange(
                    REDDIT_URL,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return Optional.empty();
            }

            //noinspection unchecked
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            if (data == null) return Optional.empty();
            //noinspection unchecked
            List<Map<String, Object>> children = (List<Map<String, Object>>) data.get("children");
            if (children == null || children.isEmpty()) return Optional.empty();

            // Walk the list to find the first usable image
            for (Map<String, Object> child : children) {
                //noinspection unchecked
                Map<String, Object> postData = (Map<String, Object>) child.get("data");
                if (postData == null) continue;
                String title = asString(postData.get("title"));
                String url = extractImageUrl(postData);
                if (url == null) {
                    continue;
                }
                return Optional.of(Map.of(
                        "title", title,
                        "url", url,
                        "servedAt", LocalDateTime.now().toString(),
                        "source", "reddit"
                ));
            }
        } catch (RestClientException ignored) {
            // fall back
        }
        return Optional.empty();
    }

    private Map<String, Object> randomFallback() {
        int idx = ThreadLocalRandom.current().nextInt(fallbackMemes.size());
        Map<String, String> pick = fallbackMemes.get(idx);
        return Map.of(
                "title", pick.get("title"),
                "url", pick.get("url"),
                "servedAt", LocalDateTime.now().toString(),
                "source", "fallback"
        );
    }

    private String asString(Object o) {
        return o != null ? o.toString() : null;
    }

    /**
     * Extracts an image URL from a Reddit post data map.
     */
    @SuppressWarnings("unchecked")
    private String extractImageUrl(Map<String, Object> postData) {
        // Prefer direct url_overridden_by_dest if it looks like an image
        String direct = asString(postData.get("url_overridden_by_dest"));
        if (isImageUrl(direct)) {
            return direct;
        }
        // Fall back to preview -> images -> source.url (may contain HTML entities)
        Object previewObj = postData.get("preview");
        if (previewObj instanceof Map<?, ?> preview) {
            Object imagesObj = preview.get("images");
            if (imagesObj instanceof List<?> images && !images.isEmpty()) {
                Object first = images.get(0);
                if (first instanceof Map<?, ?> firstMap) {
                    Object sourceObj = firstMap.get("source");
                    if (sourceObj instanceof Map<?, ?> source) {
                        String url = asString(source.get("url"));
                        if (url != null) {
                            // Reddit encodes & as &amp; in preview URLs
                            url = url.replace("&amp;", "&");
                            if (isImageUrl(url)) {
                                return url;
                            }
                        }
                    }
                }
            }
        }
        // Try thumbnail if available
        String thumb = asString(postData.get("thumbnail"));
        if (isImageUrl(thumb)) {
            return thumb;
        }
        return null;
    }

    private boolean isImageUrl(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".gif")
                || lower.contains("i.redd.it") || lower.contains("i.imgur.com");
    }
}
