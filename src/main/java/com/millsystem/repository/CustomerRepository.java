package com.millsystem.repository;

import com.millsystem.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    List<Customer> findByMillIdOrderByNameAsc(Long millId);
    Optional<Customer> findByIdAndMillId(Long id, Long millId);
}
