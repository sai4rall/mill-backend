package com.millsystem.repository;

import com.millsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    List<User> findByMillIdAndRole(Long millId, User.Role role);
    List<User> findByMillId(Long millId);

    default Optional<User> findMillAdmin(Long millId) {
        return findByMillIdAndRole(millId, User.Role.MILL_ADMIN).stream().findFirst();
    }
}
