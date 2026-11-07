package com.millsystem.security;

import com.millsystem.entity.Mill;
import com.millsystem.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public class UserPrincipal implements UserDetails {

    private final Long id;
    private final Long millId;
    private final String username;
    private final String password;
    private final User.Role role;
    private final boolean active;
    private final LocalDateTime passwordChangedAt;

    public UserPrincipal(User user) {
        this.id = user.getId();
        this.millId = user.getMill() != null ? user.getMill().getId() : null;
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.role = user.getRole();
        this.passwordChangedAt = user.getPasswordChangedAt();
        // A user is only usable if their own account is active and, for mill-scoped users, their mill is ACTIVE.
        this.active = user.isActive()
                && (user.getMill() == null || user.getMill().getStatus() == Mill.MillStatus.ACTIVE);
    }

    public Long getId() { return id; }
    public Long getMillId() { return millId; }
    public User.Role getRole() { return role; }
    public LocalDateTime getPasswordChangedAt() { return passwordChangedAt; }

    @Override public String getUsername() { return username; }
    @Override public String getPassword() { return password; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return active; }
}
