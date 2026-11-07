package com.millsystem.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.ZoneId;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;
    private final TokenRevocationService revocationService;

    public JwtAuthFilter(JwtUtil jwtUtil, UserDetailsServiceImpl userDetailsService, TokenRevocationService revocationService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.revocationService = revocationService;
    }

    /**
     * Public endpoints ignore any Authorization header: a stale token left in a browser must never block signing in
     * (login), signing out (logout revokes it itself) or device pings (they use X-Device-Key).
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.equals("/api/auth/login") || path.equals("/api/auth/logout") || path.startsWith("/api/gps/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            Claims claims;
            try {
                claims = jwtUtil.extractAllClaims(token);
            } catch (io.jsonwebtoken.ExpiredJwtException e) {
                unauthorized(response, "Your session has expired. Please sign in again.");
                return;
            } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
                // A token was sent but it is not ours (tampered, wrong key, malformed): the client must sign in again.
                unauthorized(response, "Invalid session. Please sign in again.");
                return;
            }
            if (revocationService.isRevoked(claims)) {
                unauthorized(response, "You have been signed out. Please sign in again.");
                return;
            }
            String username = claims.getSubject();
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails;
                try {
                    userDetails = userDetailsService.loadUserByUsername(username);
                } catch (org.springframework.security.core.userdetails.UsernameNotFoundException e) {
                    unauthorized(response, "Invalid session. Please sign in again.");
                    return;
                }
                if (!userDetails.isEnabled()) {
                    // User or their mill was deactivated after the token was issued — force logout.
                    unauthorized(response, "Account or mill is inactive");
                    return;
                }
                if (issuedBeforePasswordChange(claims, userDetails)) {
                    // Password changed or reset after this token was issued: other sessions must sign in again.
                    unauthorized(response, "Your password was changed. Please sign in again.");
                    return;
                }
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        filterChain.doFilter(request, response);
    }

    private static void unauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }

    static boolean issuedBeforePasswordChange(Claims claims, UserDetails userDetails) {
        if (!(userDetails instanceof UserPrincipal p) || p.getPasswordChangedAt() == null || claims.getIssuedAt() == null) return false;
        long changedAt = p.getPasswordChangedAt().atZone(ZoneId.systemDefault()).toEpochSecond();
        long issuedAt = claims.getIssuedAt().toInstant().getEpochSecond();
        return issuedAt < changedAt;
    }
}
