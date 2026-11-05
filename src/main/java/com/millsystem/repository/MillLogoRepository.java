package com.millsystem.repository;

import com.millsystem.entity.MillLogo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MillLogoRepository extends JpaRepository<MillLogo, Long> {
}
