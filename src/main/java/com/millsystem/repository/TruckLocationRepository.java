package com.millsystem.repository;

import com.millsystem.entity.TruckLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TruckLocationRepository extends JpaRepository<TruckLocation, Long> {

    List<TruckLocation> findByMillIdAndTruckNumberAndRecordedAtBetweenOrderByRecordedAtAsc(
            Long millId, String truckNumber, LocalDateTime from, LocalDateTime to);

    List<TruckLocation> findByMillIdAndRecordedAtBetweenOrderByRecordedAtAsc(
            Long millId, LocalDateTime from, LocalDateTime to);

    // Latest location per truck for a mill
    @Query("""
            SELECT t FROM TruckLocation t
            WHERE t.mill.id = :millId
              AND t.recordedAt = (
                SELECT MAX(t2.recordedAt) FROM TruckLocation t2
                WHERE t2.mill.id = :millId AND t2.truckNumber = t.truckNumber
              )
            ORDER BY t.truckNumber
            """)
    List<TruckLocation> findLatestPerTruckByMillId(Long millId);

    Optional<TruckLocation> findTopByMillIdAndTruckNumberOrderByRecordedAtDesc(Long millId, String truckNumber);

    // Distinct truck numbers seen in a mill from purchases or GPS pings
    @Query("SELECT DISTINCT t.truckNumber FROM TruckLocation t WHERE t.mill.id = :millId ORDER BY t.truckNumber")
    List<String> findDistinctTruckNumbersByMillId(Long millId);
}
