package com.moveo.crypto_advisor.content;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface ContentRepository extends JpaRepository<Content, Long> {
    Optional<Content> findTopByTypeAndCryptoAssetOrderByTimestampDesc(ContentType type, String cryptoAsset);
    Optional<Content> findTopByTypeOrderByTimestampDesc(ContentType type);

    boolean existsByTypeAndCryptoAssetAndTimestampAfter(ContentType type, String cryptoAsset, Instant timestamp);
}
