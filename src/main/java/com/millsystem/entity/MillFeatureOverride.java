package com.millsystem.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/**
 * Per-mill override of a {@link Feature}. A null column means "inherit the global value",
 * so a mill can e.g. override only {@code enabled} and keep the global menu setting.
 */
@Entity
@Table(name = "mill_feature_overrides",
        uniqueConstraints = @UniqueConstraint(name = "uk_mill_feature", columnNames = {"mill_id", "feature_key"}))
public class MillFeatureOverride {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mill_id", nullable = false)
    private Mill mill;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feature_key", nullable = false)
    private Feature feature;

    @Column(name = "show_in_menu")
    private Boolean showInMenu;

    private Boolean enabled;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public Mill getMill() { return mill; }
    public void setMill(Mill mill) { this.mill = mill; }
    public Feature getFeature() { return feature; }
    public void setFeature(Feature feature) { this.feature = feature; }
    public Boolean getShowInMenu() { return showInMenu; }
    public void setShowInMenu(Boolean showInMenu) { this.showInMenu = showInMenu; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
