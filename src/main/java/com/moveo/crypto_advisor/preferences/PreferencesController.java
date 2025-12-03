package com.moveo.crypto_advisor.preferences;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/preferences")
@RequiredArgsConstructor
public class PreferencesController {

    private final PreferencesService preferencesService;

    // GET /api/preferences/me?username=someUser
    @GetMapping("/me")
    public ResponseEntity<PreferencesResponse> getMyPreferences(
            @RequestParam String username
    ) {
        Optional<PreferencesResponse> prefs =
                preferencesService.getForUser(username);

        return prefs
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    // POST /api/preferences/me?username=someUser
    @PostMapping("/me")
    public ResponseEntity<PreferencesResponse> saveMyPreferences(
            @RequestParam String username,
            @RequestBody PreferencesRequest request
    ) {
        PreferencesResponse response =
                preferencesService.saveForUser(username, request);

        return ResponseEntity.ok(response);
    }
}
