package com.millsystem.repository;

import com.millsystem.entity.ExportShipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExportShipmentRepository extends JpaRepository<ExportShipment, Long> {
    List<ExportShipment> findByOrderIdAndMillIdOrderByShipmentDateDesc(Long orderId, Long millId);
    List<ExportShipment> findByMillIdOrderByShipmentDateAsc(Long millId);
    List<ExportShipment> findByMillIdAndShipmentDateBetweenOrderByShipmentDateAsc(Long millId, LocalDate from, LocalDate to);

    @Query("SELECT COALESCE(SUM(s.quintalsExported), 0) FROM ExportShipment s WHERE s.mill.id = :millId")
    BigDecimal sumExportedByMillId(Long millId);

    @Query("SELECT COALESCE(SUM(s.quintalsExported), 0) FROM ExportShipment s WHERE s.order.id = :orderId")
    BigDecimal sumExportedByOrderId(Long orderId);
}
