package com.millsystem.repository;

import com.millsystem.entity.MachineDowntime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MachineDowntimeRepository extends JpaRepository<MachineDowntime, Long> {
    List<MachineDowntime> findByMillIdOrderByDowntimeDateDescStartTimeDesc(Long millId);
    List<MachineDowntime> findByMillIdAndDowntimeDateBetween(Long millId, LocalDate from, LocalDate to);
    Optional<MachineDowntime> findByIdAndMillId(Long id, Long millId);
}
