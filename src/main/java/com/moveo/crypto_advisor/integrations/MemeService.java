package com.moveo.crypto_advisor.integrations;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Supplies a meme payload, preferring Reddit (r/cryptomemes) and falling back to a static list.
 */
@Service
public class MemeService {

    private static final String REDDIT_URL = "https://www.reddit.com/r/cryptomemes/top.json?limit=50&t=day";
    private static final String MEME_API_URL = "https://meme-api.com/gimme/cryptomemes";

    private final RestTemplate restTemplate;

    // Simple text-based fallbacks (SVG data URIs) to avoid broken external links.
    private final List<Map<String, String>> fallbackMemes = List.of(
            Map.of("title", "HODL vibes", "url", "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='800' height='480'><rect width='100%' height='100%' fill='%232563eb'/><text x='50%' y='50%' fill='white' font-size='48' font-family='Segoe UI, Arial' text-anchor='middle'>HODL vibes</text></svg>"),
            Map.of("title", "Charts at 3am", "url", "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='800' height='480'><rect width='100%' height='100%' fill='%230f172a'/><text x='50%' y='50%' fill='%23e2e8f0' font-size='44' font-family='Segoe UI, Arial' text-anchor='middle'>Charts at 3am</text></svg>"),
            Map.of("title", "Buy the dip?", "url", "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='800' height='480'><rect width='100%' height='100%' fill='%23f59e0b'/><text x='50%' y='50%' fill='%230b1b3d' font-size='44' font-family='Segoe UI, Arial' text-anchor='middle'>Buy the dip?</text></svg>"),
            Map.of("title", "gm frens", "url", "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='800' height='480'><rect width='100%' height='100%' fill='%23f8fafc'/><text x='50%' y='50%' fill='%230f172a' font-size='44' font-family='Segoe UI, Arial' text-anchor='middle'>gm frens</text></svg>"),
            Map.of("title", "Bear to Bull", "url", "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='800' height='480'><rect width='100%' height='100%' fill='%2316a34a'/><text x='50%' y='50%' fill='white' font-size='44' font-family='Segoe UI, Arial' text-anchor='middle'>Bear to Bull</text></svg>")
    );

    public MemeService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Map<String, Object> getMeme() {
        return fetchFromMemeApi()
                .or(() -> fetchFromReddit())
                .orElseGet(this::randomFallback);
    }

    private Optional<Map<String, Object>> fetchFromMemeApi() {
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    MEME_API_URL,
                    HttpMethod.GET,
                    null,
                    Map.class
            );
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return Optional.empty();
            }
            //noinspection unchecked
            Map<String, Object> body = response.getBody();
            Object url = body.get("url");
            Object title = body.get("title");
            if (url instanceof String u && isImageUrl(u)) {
                return Optional.of(Map.of(
                        "title", title != null ? title.toString() : "Meme",
                        "url", u,
                        "servedAt", LocalDateTime.now().toString(),
                        "source", "meme-api"
                ));
            }
        } catch (RestClientException ignored) {
            // fall back
        }
        return Optional.empty();
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
        int day = LocalDate.now().getDayOfYear();
        int idx = day % fallbackMemes.size();
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
