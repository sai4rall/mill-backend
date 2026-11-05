package com.millsystem.feature;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.millsystem.entity.User;
import com.millsystem.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

/**
 * Rejects API calls to modules that are disabled for the caller's mill (403).
 * SUPER_ADMIN is never blocked: they support every mill. DEV_ADMIN is kept out of mill data elsewhere. Unknown/unseeded features count as enabled.
 */
@Component
public class FeatureAccessInterceptor implements HandlerInterceptor {

    private final FeatureAccessRules rules;
    private final FeatureFlagService flags;
    private final ObjectMapper json;

    public FeatureAccessInterceptor(FeatureAccessRules rules, FeatureFlagService flags, ObjectMapper json) {
        this.rules = rules;
        this.flags = flags;
        this.json = json;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)
                || principal.getRole() == User.Role.SUPER_ADMIN || principal.getRole() == User.Role.DEV_ADMIN) {
            return true;
        }
        String path = request.getRequestURI().substring(request.getContextPath().length());
        var rule = rules.find(request.getMethod(), path);
        if (rule.isEmpty()) return true;

        Map<String, Boolean> enabled = flags.enabledMap(principal.getMillId());
        boolean allowed = rule.get().anyOf().stream().anyMatch(k -> enabled.getOrDefault(k.key(), true));
        if (allowed) return true;

        FeatureKey module = rule.get().anyOf().iterator().next();
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        json.writeValue(response.getWriter(), Map.of(
                "error", "The " + module.label() + " module is not enabled for your mill",
                "code", "FEATURE_DISABLED"));
        return false;
    }
}
