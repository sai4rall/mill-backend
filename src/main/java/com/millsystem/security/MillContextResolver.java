package com.millsystem.security;

import com.millsystem.entity.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class MillContextResolver {

    /**
     * Resolves the effective millId for the current request.
     * - SUPER_ADMIN: reads from X-Mill-Id header (required — throws if absent)
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
            return headerMillId;
        }
        return principal.getMillId();
    }
}
