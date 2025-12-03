package com.moveo.crypto_advisor.preferences;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * Lists the allowed onboarding options so the frontend can render select inputs.
 */
@Data
@AllArgsConstructor
public class PreferencesOptionsResponse {
    private List<String> cryptoAssetSuggestions;
    private List<InvestorTypeOption> investorTypes;
    private List<ContentPreferenceOption> contentPreferences;
}
