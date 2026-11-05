package com.millsystem.dto;

/**
 * Body for changing flags. Global update: a null field is left unchanged.
 * Mill override: a null field means "inherit the global value" (both null removes the override).
 */
public class FeatureFlagUpdateRequest {
    private Boolean showInMenu;
    private Boolean enabled;

    public Boolean getShowInMenu() { return showInMenu; }
    public void setShowInMenu(Boolean showInMenu) { this.showInMenu = showInMenu; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
