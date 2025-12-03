package com.moveo.crypto_advisor.preferences;

import com.moveo.crypto_advisor.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PreferencesRepository extends JpaRepository<Preferences, Long> {

    // Spring Data will implement this method by naming convention:
    // SELECT * FROM preferences WHERE user = ?;

    Optional<Preferences> findByUser(User user);

    // Returns true if there is a preferences row for this user (used to know if onboarding is done)
    boolean existsByUser(User user);
}
