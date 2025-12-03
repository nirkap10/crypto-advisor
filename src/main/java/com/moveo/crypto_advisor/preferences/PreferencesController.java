package com.moveo.crypto_advisor.preferences;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Optional;

@RestController
@RequestMapping("/api/preferences")
@RequiredArgsConstructor
public class PreferencesController {

    private final PreferencesService preferencesService;

    /**
     * Returns the allowed onboarding options (investor types, content categories, and example assets).
     */
    @GetMapping("/options")
    public ResponseEntity<PreferencesOptionsResponse> options() {
        return ResponseEntity.ok(preferencesService.getOptions());
    }

    // GET /api/preferences/me?username=someUser
    @GetMapping("/me")
    // Username now taken from authenticated principal (Basic auth)
    public ResponseEntity<PreferencesResponse> getMyPreferences(Principal principal) {
        Optional<PreferencesResponse> prefs =
                preferencesService.getForUser(principal.getName());

        // if prefs is not found, return 204 No Content
        return prefs
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    // POST /api/preferences/me?username=someUser
    @PostMapping("/me")
    public ResponseEntity<PreferencesResponse> saveMyPreferences(
            // Authenticated principal provides username (Basic auth)
            Principal principal,
            // @RequestBody = spring parse down the json into the request object
            @RequestBody PreferencesRequest request
    ) {
        PreferencesResponse response =
                preferencesService.saveForUser(principal.getName(), request);

        return ResponseEntity.ok(response);
    }
}
