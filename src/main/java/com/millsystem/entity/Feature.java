package com.millsystem.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/** Global (all mills) setting for one module. Mills may override it via {@link MillFeatureOverride}. */
@Entity
@Table(name = "features")
public class Feature {

    @Id
    @Column(name = "feature_key", length = 50)
    private String featureKey;

    @Column(nullable = false, length = 100)
    private String name;

    private String description;

    @ColumnDefault("true")
    @Column(name = "show_in_menu", nullable = false)
    private boolean showInMenu = true;

    @ColumnDefault("true")
    @Column(nullable = false)
    private boolean enabled = true;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public String getFeatureKey() { return featureKey; }
    public void setFeatureKey(String featureKey) { this.featureKey = featureKey; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isShowInMenu() { return showInMenu; }
    public void setShowInMenu(boolean showInMenu) { this.showInMenu = showInMenu; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
