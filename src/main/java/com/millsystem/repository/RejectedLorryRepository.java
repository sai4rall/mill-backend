package com.millsystem.repository;

import com.millsystem.entity.RejectedLorry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RejectedLorryRepository extends JpaRepository<RejectedLorry, Long> {
    List<RejectedLorry> findByMillIdOrderByRejectionDateDescIdDesc(Long millId);
    Optional<RejectedLorry> findByIdAndMillId(Long id, Long millId);
}
