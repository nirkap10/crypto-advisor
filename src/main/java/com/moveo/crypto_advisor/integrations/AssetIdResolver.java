package com.moveo.crypto_advisor.integrations;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Resolves user-facing tickers to third-party IDs (e.g., CoinGecko ids).
 */
@Component
public class AssetIdResolver {

    private static final List<String> SUPPORTED_TICKERS = List.of(
            "BTC", "ETH", "USDT", "USDC", "BNB", "XRP", "SOL", "DOT", "ADA", "DOGE"
    );

    private static final Map<String, String> TICKER_TO_COINGECKO = SUPPORTED_TICKERS.stream()
            .collect(Collectors.toUnmodifiableMap(
                    ticker -> ticker,
                    ticker -> switch (ticker) {
                        case "BTC" -> "bitcoin";
                        case "ETH" -> "ethereum";
                        case "USDT" -> "tether";
                        case "USDC" -> "usd-coin";
                        case "BNB" -> "binancecoin";
                        case "XRP" -> "ripple";
                        case "SOL" -> "solana";
                        case "DOT" -> "polkadot";
                        case "ADA" -> "cardano";
                        case "DOGE" -> "dogecoin";
                        default -> null;
                    }
            ));

    /**
     * Translate a ticker (case-insensitive) to the CoinGecko asset id; returns null if unknown.
     */
    public String toCoinGeckoId(String ticker) {
        if (ticker == null) {
            return null;
        }
        return TICKER_TO_COINGECKO.get(ticker.toUpperCase(Locale.ROOT));
    }

    public List<String> getSupportedTickers() {
        return SUPPORTED_TICKERS;
    }

    public List<String> getSupportedCoinGeckoIds() {
        return SUPPORTED_TICKERS.stream()
                .map(this::toCoinGeckoId)
                .filter(Objects::nonNull)
                .toList();
    }
}
