package com.millsystem.dto;

/** A module's flags — either the global values or the effective values for one mill. */
public class FeatureFlagDto {
    private String key;
    private String name;
    private String description;
    private boolean showInMenu;
    private boolean enabled;

    public FeatureFlagDto() {}

    public FeatureFlagDto(String key, String name, String description, boolean showInMenu, boolean enabled) {
        this.key = key; this.name = name; this.description = description;
        this.showInMenu = showInMenu; this.enabled = enabled;
    }

    public String getKey() { return key; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isShowInMenu() { return showInMenu; }
    public boolean isEnabled() { return enabled; }
}
