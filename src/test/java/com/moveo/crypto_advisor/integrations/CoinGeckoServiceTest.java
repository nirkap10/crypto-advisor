package com.moveo.crypto_advisor.integrations;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Lightweight test to ensure CoinGeckoService builds the expected URL and parses the response.
 */
class CoinGeckoServiceTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private CoinGeckoService service;

    @BeforeEach
    void setup() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        service = new CoinGeckoService(restTemplate);
        // Inject configuration normally provided by @Value
        ReflectionTestUtils.setField(service, "apiKey", "demo-key");
        ReflectionTestUtils.setField(service, "baseUrl", "https://api.coingecko.com/api/v3");
    }

    @Test
    void fetchSimplePrices_buildsUrlAndReturnsBody() {
        server.expect(requestTo("https://api.coingecko.com/api/v3/simple/price?x_cg_demo_api_key=demo-key&ids=bitcoin,ethereum&vs_currencies=usd"))
                .andExpect(method(GET))
                .andRespond(withSuccess("{\"bitcoin\":{\"usd\":100},\"ethereum\":{\"usd\":10}}", MediaType.APPLICATION_JSON));

        Map<String, Object> body = service.fetchSimplePrices(List.of("bitcoin", "ethereum"), "usd");

        assertThat(body).containsKeys("bitcoin", "ethereum");
        server.verify();
    }
}
