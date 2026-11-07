package com.millsystem.feature;

import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.millsystem.feature.FeatureKey.*;

/**
 * Which module(s) an API endpoint belongs to. A request is allowed if ANY of the listed modules is enabled.
 * Some list endpoints are shared because other screens read them as lookups
 * (e.g. the Purchases screen reads the farmer list), so they stay readable while any such screen is enabled;
 * every write still needs the owning module. First matching rule wins — keep specific rules first.
 * Endpoints with no rule (auth, admin, GPS ingest, feature flags) are never blocked.
 */
@Component
public class FeatureAccessRules {

    public record Rule(String method, String pattern, Set<FeatureKey> anyOf) {
        boolean matches(String httpMethod, String path, AntPathMatcher matcher) {
            return (method == null || method.equalsIgnoreCase(httpMethod)) && matcher.match(pattern, path);
        }
    }

    private static final List<Rule> RULES = List.of(
            new Rule("GET", "/api/farmers", Set.of(FARMERS, PURCHASES)),
            new Rule(null, "/api/farmers/**", Set.of(FARMERS)),

            new Rule("GET", "/api/purchases", Set.of(PURCHASES, MILL_OPERATIONS, DASHBOARD)),
            new Rule(null, "/api/purchases/**", Set.of(PURCHASES)),
            new Rule(null, "/api/payments/**", Set.of(PURCHASES)),
            new Rule(null, "/api/moisture-deductions/**", Set.of(MOISTURE_DEDUCTION)),

            new Rule(null, "/api/mill-batches/**", Set.of(MILL_OPERATIONS)),

            // Export Orders and Shipments are served by the same screen.
            new Rule("GET", "/api/orders", Set.of(EXPORT_ORDERS, SHIPMENTS, DASHBOARD)),
            new Rule(null, "/api/orders/**", Set.of(EXPORT_ORDERS, SHIPMENTS)),
            new Rule(null, "/api/customers/**", Set.of(EXPORT_ORDERS, SHIPMENTS)),
            new Rule(null, "/api/shipments/**", Set.of(EXPORT_ORDERS, SHIPMENTS)),

            new Rule("GET", "/api/stock/summary", Set.of(STOCK_REPORT, DASHBOARD)),
            new Rule(null, "/api/stock/**", Set.of(STOCK_REPORT)),

            new Rule(null, "/api/trucks/**", Set.of(TRUCK_TRACKING)),
            new Rule(null, "/api/mill/gps-key/**", Set.of(TRUCK_TRACKING)),
            new Rule(null, "/api/mill/operators/**", Set.of(OPERATORS))
    );

    private final AntPathMatcher matcher = new AntPathMatcher();

    public Optional<Rule> find(String httpMethod, String path) {
        return RULES.stream().filter(r -> r.matches(httpMethod, path, matcher)).findFirst();
    }
}
