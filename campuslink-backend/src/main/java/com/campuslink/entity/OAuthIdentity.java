package com.campuslink.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
        name = "oauth_identities",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_oauth_identity_provider_subject",
                        columnNames = {"provider", "provider_user_id"}),
                @UniqueConstraint(name = "uk_oauth_identity_user_provider",
                        columnNames = {"user_id", "provider"})
        },
        indexes = @Index(name = "idx_oauth_identity_user_id", columnList = "user_id")
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString(onlyExplicitlyIncluded = true, callSuper = false)
public class OAuthIdentity extends BaseEntity {

    @ToString.Include
    @Column(name = "provider", nullable = false, length = 20)
    private String provider;

    @ToString.Include
    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_oauth_identity_user"))
    private User user;
}
