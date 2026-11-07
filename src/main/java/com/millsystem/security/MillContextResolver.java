package com.millsystem.security;

import com.millsystem.entity.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class MillContextResolver {

    /**
     * Resolves the effective millId for the current request.
     * - SUPER_ADMIN: reads from X-Mill-Id header (required — throws if absent); read-only (GET/HEAD)
     * - MILL_ADMIN / MILL_OPERATOR: always uses their own millId from the JWT
     * - DEV_ADMIN: never allowed to read or write mill data
     */
    public Long resolve(UserPrincipal principal, Long headerMillId) {
        if (principal.getRole() == User.Role.DEV_ADMIN) {
            throw new AccessDeniedException("Dev admin cannot access mill data");
        }
        if (principal.getRole() == User.Role.SUPER_ADMIN) {
            if (headerMillId == null) {
                throw new IllegalArgumentException("X-Mill-Id header is required for SUPER_ADMIN requests");
            }
            // Super admins oversee mills (stock, statements, exports) but do not record a mill's operations.
            if (!isReadOnlyRequest()) {
                throw new AccessDeniedException("Super admins can view mill data but cannot change it");
            }
            return headerMillId;
        }
        return principal.getMillId();
    }

    private static boolean isReadOnlyRequest() {
        var attrs = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        if (!(attrs instanceof org.springframework.web.context.request.ServletRequestAttributes sra)) return true;
        String method = sra.getRequest().getMethod();
        return "GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method);
    }
}
