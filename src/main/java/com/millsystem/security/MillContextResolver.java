package com.millsystem.security;

import com.millsystem.exception.ResourceNotFoundException;
import com.millsystem.repository.MillRepository;
import com.millsystem.entity.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class MillContextResolver {

    private final MillRepository mills;

    public MillContextResolver(MillRepository mills) {
        this.mills = mills;
    }

    /**
     * Resolves the effective millId for the current request.
     * - SUPER_ADMIN: reads from X-Mill-Id header (required — throws if absent); only mills of their own company
     *   (another company's mill is reported as not found); read-only (GET/HEAD)
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
            requireOwned(principal, headerMillId);
            // Super admins oversee mills (stock, statements, exports) but do not record a mill's operations.
            if (!isReadOnlyRequest()) {
                throw new AccessDeniedException("Super admins can view mill data but cannot change it");
            }
            return headerMillId;
        }
        return principal.getMillId();
    }

    /** Each super admin is a separate company: a mill they do not own does not exist for them. */
    public void requireOwned(UserPrincipal principal, Long millId) {
        if (!mills.existsByIdAndOwnerId(millId, principal.getId())) {
            throw new ResourceNotFoundException("Mill not found: " + millId);
        }
    }

    private static boolean isReadOnlyRequest() {
        var attrs = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        if (!(attrs instanceof org.springframework.web.context.request.ServletRequestAttributes sra)) return true;
        String method = sra.getRequest().getMethod();
        return "GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method);
    }
}
