package com.millsystem.repository;

import com.millsystem.entity.Farmer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FarmerRepository extends JpaRepository<Farmer, Long> {
    List<Farmer> findByMillIdOrderByNameAsc(Long millId);
    Optional<Farmer> findByIdAndMillId(Long id, Long millId);
}
