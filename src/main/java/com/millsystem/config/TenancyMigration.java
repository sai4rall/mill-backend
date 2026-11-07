package com.millsystem.config;

import com.millsystem.entity.Mill;
import com.millsystem.entity.MoistureDeduction;
import com.millsystem.entity.User;
import com.millsystem.repository.MillRepository;
import com.millsystem.repository.MoistureDeductionRepository;
import com.millsystem.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * One-time upgrade of data created before each super admin became a separate company. Runs at startup after the
 * seed accounts exist and does nothing once the data is migrated.
 * <ul>
 *   <li>Mills without an owner: with exactly one super admin they are given to that super admin (it created them);
 *       with several, they stay unassigned (invisible to every super admin) until a dev admin assigns them.</li>
 *   <li>The old shared moisture table becomes each super admin's company default (copied to those without one),
 *       so existing prices do not change, and the shared table is removed.</li>
 * </ul>
 */
@Component
public class TenancyMigration {

    private static final Logger log = LoggerFactory.getLogger(TenancyMigration.class);

    private final MillRepository mills;
    private final UserRepository users;
    private final MoistureDeductionRepository moisture;

    public TenancyMigration(MillRepository mills, UserRepository users, MoistureDeductionRepository moisture) {
        this.mills = mills;
        this.users = users;
        this.moisture = moisture;
    }

    @Transactional
    public void run() {
        List<User> supers = users.findByRoleOrderByUsernameAsc(User.Role.SUPER_ADMIN);

        List<Mill> unowned = mills.findByOwnerIsNull();
        if (!unowned.isEmpty()) {
            if (supers.size() == 1) {
                unowned.forEach(m -> m.setOwner(supers.get(0)));
                mills.saveAll(unowned);
                log.info("Assigned {} mills without an owner to super admin {}", unowned.size(), supers.get(0).getUsername());
            } else {
                log.warn("{} mills have no owning super admin and are hidden from all super admins. Assign them in the Dev Console.", unowned.size());
            }
        }

        List<MoistureDeduction> legacy = moisture.findByMillIsNullAndOwnerIdIsNullOrderByFromPctAsc();
        if (!legacy.isEmpty() && !supers.isEmpty()) {
            for (User s : supers) {
                if (!moisture.findByMillIsNullAndOwnerIdOrderByFromPctAsc(s.getId()).isEmpty()) continue;
                for (MoistureDeduction l : legacy) {
                    MoistureDeduction copy = new MoistureDeduction();
                    copy.setOwnerId(s.getId());
                    copy.setFromPct(l.getFromPct());
                    copy.setToPct(l.getToPct());
                    copy.setDeductionPct(l.getDeductionPct());
                    moisture.save(copy);
                }
            }
            moisture.deleteLegacyGlobal();
            log.info("Moved the shared moisture table ({} ranges) to each super admin's company default", legacy.size());
        }
    }
}
