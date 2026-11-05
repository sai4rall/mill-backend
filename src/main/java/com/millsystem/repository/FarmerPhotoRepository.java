package com.millsystem.repository;

import com.millsystem.entity.FarmerPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FarmerPhotoRepository extends JpaRepository<FarmerPhoto, Long> {
}
