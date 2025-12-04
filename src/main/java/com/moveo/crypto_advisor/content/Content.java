package com.moveo.crypto_advisor.content;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Stores a single piece of fetched content (news, price, etc) for a crypto asset on a given day.
 */
@Getter
@Setter
@Entity
@Table(name = "content")
public class Content {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Category of the content (e.g., NEWS, PRICE)
    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private ContentType type;

    // CoinGecko-style asset id this content is associated with (e.g., "bitcoin")
    @Column(name = "crypto_asset", length = 128, nullable = false)
    private String cryptoAsset;

    // Raw payload stored as text/JSON
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    // When this content was fetched
    @Column(name = "created_at", nullable = false)
    private Instant timestamp;
}
