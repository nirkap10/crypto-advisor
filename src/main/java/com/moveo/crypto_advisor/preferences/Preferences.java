package com.moveo.crypto_advisor.preferences;

import com.moveo.crypto_advisor.user.User;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;
// @Data = Lombok generates getters, setters, toString, equals, hashCode automatically
@Data
// @Entity = JPA annotation to make this class a JPA entity
@Entity
// @Table = JPA annotation to map this class to a database table
@Table(name = "preferences")
public class Preferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    // @ElementCollection = this is not a separate entity, but a collection of simple values (Strings)
    // @CollectionTable = defines the table that stores the collection ("crypto_assets")
    // joinColumns = foreign key column in "crypto_assets" that points to this Preferences row
    // @Column(name = "asset") = column name inside "crypto_assets" table that holds each string
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
