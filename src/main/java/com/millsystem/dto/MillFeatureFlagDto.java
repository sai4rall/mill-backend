package com.millsystem.dto;

/** One module for one mill: the global value, the mill's override (null = inherit) and the effective result. */
public class MillFeatureFlagDto {
    private final String key;
    private final String name;
    private final boolean globalShowInMenu;
    private final boolean globalEnabled;
    private final Boolean overrideShowInMenu;
    private final Boolean overrideEnabled;

    public MillFeatureFlagDto(String key, String name, boolean globalShowInMenu, boolean globalEnabled,
                              Boolean overrideShowInMenu, Boolean overrideEnabled) {
        this.key = key; this.name = name;
        this.globalShowInMenu = globalShowInMenu; this.globalEnabled = globalEnabled;
        this.overrideShowInMenu = overrideShowInMenu; this.overrideEnabled = overrideEnabled;
    }

    public String getKey() { return key; }
    public String getName() { return name; }
    public boolean isGlobalShowInMenu() { return globalShowInMenu; }
    public boolean isGlobalEnabled() { return globalEnabled; }
    public Boolean getOverrideShowInMenu() { return overrideShowInMenu; }
    public Boolean getOverrideEnabled() { return overrideEnabled; }
    public boolean isShowInMenu() { return overrideShowInMenu != null ? overrideShowInMenu : globalShowInMenu; }
    public boolean isEnabled() { return overrideEnabled != null ? overrideEnabled : globalEnabled; }
}
