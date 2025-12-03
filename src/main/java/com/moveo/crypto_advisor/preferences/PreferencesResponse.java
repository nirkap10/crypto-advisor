package com.moveo.crypto_advisor.preferences;

import lombok.Data;
import java.util.List;

@Data
public class PreferencesResponse {
    private List<String> cryptoAssets;
    private String investorType;
    private boolean marketNews;
    private boolean charts;
    private boolean social;
    private boolean fun;
    private boolean completed;
}
