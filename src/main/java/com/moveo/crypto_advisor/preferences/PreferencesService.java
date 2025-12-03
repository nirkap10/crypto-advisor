package com.moveo.crypto_advisor.preferences;

import com.moveo.crypto_advisor.user.User;
import com.moveo.crypto_advisor.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PreferencesService {

    private final PreferencesRepository preferencesRepository;
    private final UserRepository userRepository;

    public PreferencesResponse saveForUser(String username, PreferencesRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        Preferences prefs = preferencesRepository.findByUser(user)
                .orElseGet(() -> {
                    Preferences p = new Preferences();
                    p.setUser(user);
                    return p;
                });

        prefs.setCryptoAssets(request.getCryptoAssets());
        prefs.setInvestorType(request.getInvestorType());
        prefs.setMarketNews(request.isMarketNews());
        prefs.setCharts(request.isCharts());
        prefs.setSocial(request.isSocial());
        prefs.setFun(request.isFun());

        Preferences saved = preferencesRepository.save(prefs);
        return toResponse(saved);
    }

    public Optional<PreferencesResponse> getForUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        return preferencesRepository.findByUser(user)
                .map(this::toResponse);
    }

    public boolean hasCompletedOnboarding(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        return preferencesRepository.existsByUser(user);
    }

    private PreferencesResponse toResponse(Preferences prefs) {
        PreferencesResponse res = new PreferencesResponse();
        res.setCryptoAssets(prefs.getCryptoAssets());
        res.setInvestorType(prefs.getInvestorType());
        res.setMarketNews(prefs.isMarketNews());
        res.setCharts(prefs.isCharts());
        res.setSocial(prefs.isSocial());
        res.setFun(prefs.isFun());
        res.setCompleted(true);
        return res;
    }
}
