package com.moveo.crypto_advisor.preferences;

import com.moveo.crypto_advisor.user.User;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;
@Data
@Entity
@Table(name = "preferences")
public class Preferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ElementCollection
    @CollectionTable(
            name = "crypto_assets",
            joinColumns = @JoinColumn(name = "preferences_id")
    )
    @Column(name = "asset")
    private List<String> cryptoAssets;

    private String investorType;
    private boolean marketNews;
    private boolean charts;
    private boolean social;
    private boolean fun;
}
