package com.millsystem.repository;

import com.millsystem.entity.Mill;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MillRepository extends JpaRepository<Mill, Long> {

    /**
     * Locks the mill row for the rest of the transaction. Stock-consuming writes (mill batches, shipments)
     * take this lock first so two concurrent requests cannot both spend the same paddy or rice.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Mill m WHERE m.id = :id")
    Optional<Mill> findByIdForUpdate(Long id);

    Optional<Mill> findByGpsKeyHash(String gpsKeyHash);

    // Company (super admin) scoping.
    List<Mill> findByOwnerIdOrderByNameAsc(Long ownerId);
    Optional<Mill> findByIdAndOwnerId(Long id, Long ownerId);
    boolean existsByIdAndOwnerId(Long id, Long ownerId);
    long countByOwnerId(Long ownerId);
    List<Mill> findByOwnerIsNull();
}
