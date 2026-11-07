package com.millsystem.service;

import com.millsystem.dto.UserAccountDto;
import com.millsystem.entity.Mill;
import com.millsystem.entity.User;
import com.millsystem.exception.ConflictException;
import com.millsystem.exception.ResourceNotFoundException;
import com.millsystem.repository.MillRepository;
import com.millsystem.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dev admin powers over every account and mill: reset any password, deactivate or delete a mill, delete a super admin.
 * Resetting a password stamps passwordChangedAt, so the user's existing sessions stop working at once; deactivating a
 * mill blocks its users on their next request (JwtAuthFilter checks the mill status).
 */
@Service
public class DevAccountService {

    private static final Logger log = LoggerFactory.getLogger(DevAccountService.class);

    // Everything that belongs to a mill, children before parents (foreign keys).
    private static final List<String[]> MILL_DATA = List.of(
            new String[]{"batch purchases", "DELETE FROM mill_batch_purchases WHERE batch_id IN (SELECT id FROM mill_batches WHERE mill_id = ?)"},
            new String[]{"payments", "DELETE FROM payments WHERE mill_id = ?"},
            new String[]{"batches", "DELETE FROM mill_batches WHERE mill_id = ?"},
            new String[]{"purchases", "DELETE FROM paddy_purchases WHERE mill_id = ?"},
            new String[]{"shipments", "DELETE FROM export_shipments WHERE mill_id = ?"},
            new String[]{"orders", "DELETE FROM export_orders WHERE mill_id = ?"},
            new String[]{"customers", "DELETE FROM customers WHERE mill_id = ?"},
            new String[]{"rejected lorries", "DELETE FROM rejected_lorries WHERE mill_id = ?"},
            new String[]{"farmer photos", "DELETE FROM farmer_photos WHERE farmer_id IN (SELECT id FROM farmers WHERE mill_id = ?)"},
            new String[]{"farmers", "DELETE FROM farmers WHERE mill_id = ?"},
            new String[]{"truck locations", "DELETE FROM truck_locations WHERE mill_id = ?"},
            new String[]{"otps", "DELETE FROM phone_otps WHERE mill_id = ?"},
            new String[]{"feature overrides", "DELETE FROM mill_feature_overrides WHERE mill_id = ?"},
            new String[]{"logos", "DELETE FROM mill_logos WHERE mill_id = ?"},
            new String[]{"moisture ranges", "DELETE FROM moisture_deductions WHERE mill_id = ?"},
            new String[]{"users", "DELETE FROM users WHERE mill_id = ?"});

    private final UserRepository users;
    private final MillRepository mills;
    private final PasswordEncoder passwordEncoder;
    private final PasswordGeneratorService passwordGenerator;
    private final JdbcTemplate jdbc;

    public DevAccountService(UserRepository users, MillRepository mills, PasswordEncoder passwordEncoder,
                             PasswordGeneratorService passwordGenerator, JdbcTemplate jdbc) {
        this.users = users;
        this.mills = mills;
        this.passwordEncoder = passwordEncoder;
        this.passwordGenerator = passwordGenerator;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<UserAccountDto> getAllUsers() {
        return users.findAll(Sort.by("username")).stream().map(UserAccountDto::from).toList();
    }

    /** New random password for any account except the dev admin's own (that needs the current password: Change password). */
    @Transactional
    public String resetPassword(Long actorId, Long userId) {
        if (actorId.equals(userId)) {
            throw new IllegalArgumentException("Use Change password to change your own password");
        }
        User user = users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        String raw = passwordGenerator.generate();
        user.setPassword(passwordEncoder.encode(raw));
        users.save(user);
        log.info("Dev admin {} reset the password of {} ({})", actorId, user.getUsername(), user.getRole());
        return raw;
    }

    @Transactional
    public Mill setMillStatus(Long millId, Mill.MillStatus status) {
        Mill mill = findMill(millId);
        mill.setStatus(status);
        return mills.save(mill);
    }

    /**
     * Permanently deletes a mill and all of its data and users. The caller must repeat the mill's name in
     * {@code confirmName}, so a stray request cannot wipe a mill. Returns the number of rows removed per kind.
     */
    @Transactional
    public Map<String, Integer> deleteMill(Long millId, String confirmName) {
        Mill mill = findMill(millId);
        if (confirmName == null || !confirmName.trim().equals(mill.getName().trim())) {
            throw new IllegalArgumentException("Type the mill name exactly to confirm deletion");
        }
        Map<String, Integer> removed = new LinkedHashMap<>();
        for (String[] step : MILL_DATA) removed.put(step[0], jdbc.update(step[1], millId));
        mills.delete(mill);
        removed.put("mill", 1);
        log.warn("Mill {} '{}' deleted by dev admin: {}", millId, mill.getName(), removed);
        return removed;
    }

    /** Moves a mill to another company (super admin). Used for mills without an owner, or to transfer one. */
    @Transactional
    public Mill assignOwner(Long millId, Long ownerId) {
        Mill mill = findMill(millId);
        User owner = findSuperAdmin(ownerId);
        mill.setOwner(owner);
        log.warn("Mill {} '{}' assigned to super admin {}", millId, mill.getName(), owner.getUsername());
        return mills.save(mill);
    }

    /**
     * Deletes a super admin (a company). Refused while they still own mills, so no company's mills are left without
     * an owner: delete or reassign those mills first. Their company default moisture table goes with them.
     */
    @Transactional
    public void deleteSuperAdmin(Long id) {
        User user = findSuperAdmin(id);
        long owned = mills.countByOwnerId(id);
        if (owned > 0) {
            throw new ConflictException(user.getUsername() + " still owns " + owned + (owned == 1 ? " mill" : " mills")
                    + ". Delete or reassign them first.");
        }
        jdbc.update("DELETE FROM moisture_deductions WHERE mill_id IS NULL AND owner_id = ?", id);
        users.delete(user);
        log.warn("Super admin {} deleted by dev admin", user.getUsername());
    }

    private User findSuperAdmin(Long id) {
        return users.findById(id).filter(u -> u.getRole() == User.Role.SUPER_ADMIN)
                .orElseThrow(() -> new ResourceNotFoundException("Super admin not found: " + id));
    }

    private Mill findMill(Long id) {
        return mills.findById(id).orElseThrow(() -> new ResourceNotFoundException("Mill not found: " + id));
    }
}
