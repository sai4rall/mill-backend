package com.millsystem.repository;

import com.millsystem.entity.ExportOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExportOrderRepository extends JpaRepository<ExportOrder, Long> {
    List<ExportOrder> findByMillIdOrderByOrderDateDesc(Long millId);
    Optional<ExportOrder> findByIdAndMillId(Long id, Long millId);
}
