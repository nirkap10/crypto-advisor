package com.moveo.crypto_advisor.integrations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

/**
 * Thin client around Hugging Face Inference API for text generation.
 */
@Service
public class HuggingFaceClient {

    private final RestTemplate restTemplate;

    @Value("${huggingface.api.token}")
    private String apiToken;

    @Value("${huggingface.model-id:mistralai/Mistral-7B-Instruct-v0.3}")
    private String modelId;

    public HuggingFaceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Generates text from the provided prompt using the configured model.
     * Returns empty on failure instead of throwing.
     */
    public Optional<String> generate(String prompt) {
        if (apiToken == null || apiToken.isBlank()) {
            return Optional.empty();
        }

        String url = "https://api-inference.huggingface.co/models/" + modelId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiToken);

        Map<String, Object> body = Map.of(
                "inputs", prompt,
                "parameters", Map.of(
                        "max_new_tokens", 160,
                        "temperature", 0.7
                )
        );

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            );
            if (response.getStatusCode().is2xxSuccessful()) {
                return Optional.ofNullable(response.getBody());
            }
        } catch (RestClientException ignored) {
            // fall through to empty
        }
        return Optional.empty();
    }
}
