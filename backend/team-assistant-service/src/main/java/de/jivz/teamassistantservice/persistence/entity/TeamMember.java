package de.jivz.teamassistantservice.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Team Member - член команды разработки
 */
@Entity
@Table(name = "team_members")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String fullName;

    @Column(length = 100)
    private String role; // developer, qa, pm, designer, devops

    @Column(length = 100)
    private String team; // backend, frontend, mobile, etc

    @Column(name = "is_active")
    private Boolean isActive = true;

    // Skills (JSON array or comma-separated)
    @Column(columnDefinition = "TEXT")
    private String skills; // Java, React, Python, etc

    // Preferences for AI assistant
    @Column(name = "preferred_language", length = 10)
    private String preferredLanguage = "en"; // en, ru, de

    @Column(name = "ai_enabled")
    private Boolean aiEnabled = true;

    // Statistics
    @Column(name = "total_tasks_completed")
    private Integer totalTasksCompleted = 0;

    @Column(name = "total_tasks_active")
    private Integer totalTasksActive = 0;

    // Timestamps
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    @PrePersist
    protected void onCreate() {
        if (isActive == null) isActive = true;
        if (aiEnabled == null) aiEnabled = true;
        if (totalTasksCompleted == null) totalTasksCompleted = 0;
        if (totalTasksActive == null) totalTasksActive = 0;
        if (preferredLanguage == null) preferredLanguage = "en";
    }
}