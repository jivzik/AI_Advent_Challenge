package de.jivz.supportservice.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity for B2B WebShop customers
 */
@Entity
@Table(
        name = "support_users",
        indexes = {
                @Index(name = "idx_support_users_email", columnList = "email"),
                @Index(name = "idx_support_users_inn", columnList = "company_inn"),
                @Index(name = "idx_support_users_active", columnList = "is_active"),
                @Index(name = "idx_support_users_open_tickets", columnList = "open_tickets")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private java.util.UUID id;

    // Company information
    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "company_inn", nullable = false, unique = true, length = 12)
    private String companyInn;

    @Column(name = "company_ogrn", length = 13)
    private String companyOgrn;

    // Contact information
    @Column(nullable = false, unique = true)
    private String email;

    @Column(length = 20)
    private String phone;

    // User details
    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(length = 50)
    @Builder.Default
    private String role = "user"; // admin, manager, user, viewer

    // Account status
    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "verification_date")
    private LocalDateTime verificationDate;

    // Business metrics
    @Column(name = "total_orders")
    @Builder.Default
    private Integer totalOrders = 0;

    @Column(name = "total_spent", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalSpent = BigDecimal.ZERO;

    @Column(name = "loyalty_tier", length = 20)
    @Builder.Default
    private String loyaltyTier = "bronze"; // bronze, silver, gold, platinum

    // Support metrics
    @Column(name = "total_tickets")
    @Builder.Default
    private Integer totalTickets = 0;

    @Column(name = "open_tickets")
    @Builder.Default
    private Integer openTickets = 0;

    // Timestamps
    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}