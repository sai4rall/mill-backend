package com.millsystem.feature;

/**
 * Every module that can be switched on/off. The key is what is stored in the {@code features} table
 * and what the frontend uses to look a flag up. New keys are seeded automatically on startup
 * (shown + enabled), so adding a constant here is enough to make a module controllable.
 */
public enum FeatureKey {
    DASHBOARD("dashboard", "Dashboard"),
    FARMERS("farmers", "Farmers"),
    PURCHASES("purchases", "Purchases"),
    MILL_OPERATIONS("mill_operations", "Mill Operations"),
    EXPORT_ORDERS("export_orders", "Export Orders"),
    SHIPMENTS("shipments", "Shipments"),
    TRUCK_TRACKING("truck_tracking", "Truck Tracking"),
    STOCK_REPORT("stock_report", "Stock Report"),
    OPERATORS("operators", "Operators"),
    // Moisture % on purchases and the moisture-based price deduction (plus its settings screens).
    MOISTURE_DEDUCTION("moisture_deduction", "Moisture Deduction"),
    // Lab tests, quality standards, batch-wise quality history and certificates.
    QUALITY_CONTROL("quality_control", "Quality Control");

    private final String key;
    private final String label;

    FeatureKey(String key, String label) {
        this.key = key;
        this.label = label;
    }

    public String key() { return key; }
    public String label() { return label; }
}
