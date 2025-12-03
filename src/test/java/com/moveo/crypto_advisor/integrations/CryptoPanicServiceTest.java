package com.moveo.crypto_advisor.integrations;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;

/**
 * Lightweight test to ensure CryptoPanicService builds the expected URL and parses the response.
 */
class CryptoPanicServiceTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private CryptoPanicService service;

    @BeforeEach
    void setup() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        service = new CryptoPanicService(restTemplate);
        // Inject configuration normally provided by @Value
        ReflectionTestUtils.setField(service, "apiKey", "test-token");
        ReflectionTestUtils.setField(service, "baseUrl", "https://cryptopanic.com/api/v1");
    }

    @Test
    void fetchLatest_buildsUrlAndReturnsBody() {
        server.expect(requestTo("https://cryptopanic.com/api/v1/posts/?auth_token=test-token&kind=news"))
                .andExpect(method(GET))
                .andRespond(withSuccess("{\"results\":[]}", MediaType.APPLICATION_JSON));

        Map<String, Object> body = service.fetchLatest(Optional.of("news"));

        assertThat(body).containsKey("results");
        server.verify();
    }
}
