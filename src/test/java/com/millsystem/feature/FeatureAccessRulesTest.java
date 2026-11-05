package com.millsystem.feature;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** Which module(s) guard each endpoint. */
class FeatureAccessRulesTest {

    private final FeatureAccessRules rules = new FeatureAccessRules();

    private Set<String> modulesFor(String method, String path) {
        return rules.find(method, path).map(r -> r.anyOf().stream().map(FeatureKey::key).collect(Collectors.toSet()))
                .orElse(Set.of());
    }

    private static Set<String> set(String csv) {
        return csv.isBlank() ? Set.of() : Arrays.stream(csv.split("\\|")).collect(Collectors.toSet());
    }

    @ParameterizedTest(name = "{0} {1} → {2}")
    @CsvSource({
            // Shared lookup lists: readable while any screen using them is enabled.
            "GET,    /api/farmers,                       farmers|purchases",
            "GET,    /api/purchases,                     purchases|mill_operations|dashboard",
            "GET,    /api/orders,                        export_orders|shipments|dashboard",
            "GET,    /api/stock/summary,                 stock_report|dashboard",
            // Writes and details need the owning module.
            "POST,   /api/farmers,                       farmers",
            "GET,    /api/farmers/5,                     farmers",
            "PUT,    /api/farmers/5/photo,               farmers",
            "POST,   /api/farmers/otp/send,              farmers",
            "POST,   /api/purchases,                     purchases",
            "GET,    /api/payments,                      purchases",
            "POST,   /api/payments/otp/send,             purchases",
            "GET,    /api/moisture-deductions,           moisture_deduction",
            "PUT,    /api/moisture-deductions,           moisture_deduction",
            "POST,   /api/mill-batches,                  mill_operations",
            "PUT,    /api/mill-batches/1/complete,       mill_operations",
            "POST,   /api/orders,                        export_orders|shipments",
            "GET,    /api/customers,                     export_orders|shipments",
            "POST,   /api/shipments,                     export_orders|shipments",
            "GET,    /api/stock/statement,               stock_report",
            "GET,    /api/stock/statement/export/excel,  stock_report",
            "GET,    /api/trucks/numbers,                truck_tracking",
            "GET,    /api/trucks/AP07CD1234/path,        truck_tracking",
            "GET,    /api/mill/operators,                operators",
            "POST,   /api/mill/operators/2/reset-password, operators",
            // Never blocked.
            "POST,   /api/auth/login,                    ''",
            "GET,    /api/features,                      ''",
            "GET,    /api/admin/features,                ''",
            "PUT,    /api/admin/moisture-deductions,     ''",
            "POST,   /api/gps/ping,                      ''",
    })
    void mapsEndpointsToModules(String method, String path, String expected) {
        assertThat(modulesFor(method, path)).isEqualTo(set(expected));
    }
}
