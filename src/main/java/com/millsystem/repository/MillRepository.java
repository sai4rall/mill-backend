package com.millsystem.repository;

import com.millsystem.entity.Mill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MillRepository extends JpaRepository<Mill, Long> {
}
