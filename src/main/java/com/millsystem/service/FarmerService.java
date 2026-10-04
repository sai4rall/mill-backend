package com.millsystem.service;

import com.millsystem.dto.FarmerDto;
import com.millsystem.entity.Farmer;
import com.millsystem.entity.Mill;
import com.millsystem.exception.ResourceNotFoundException;
import com.millsystem.repository.FarmerRepository;
import com.millsystem.repository.MillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FarmerService {

    private final FarmerRepository farmerRepository;
    private final MillRepository millRepository;

    public FarmerService(FarmerRepository farmerRepository, MillRepository millRepository) {
        this.farmerRepository = farmerRepository;
        this.millRepository = millRepository;
    }

    public List<FarmerDto> getAll(Long millId) {
        return farmerRepository.findByMillIdOrderByNameAsc(millId)
                .stream().map(FarmerDto::from).collect(Collectors.toList());
    }

    public FarmerDto getById(Long id, Long millId) {
        return farmerRepository.findByIdAndMillId(id, millId)
                .map(FarmerDto::from)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer not found: " + id));
    }

    @Transactional
    public FarmerDto create(FarmerDto dto, Long millId) {
        Mill mill = millRepository.findById(millId)
                .orElseThrow(() -> new ResourceNotFoundException("Mill not found"));
        Farmer farmer = new Farmer();
        farmer.setMill(mill);
        applyFields(farmer, dto);
        return FarmerDto.from(farmerRepository.save(farmer));
    }

    @Transactional
    public FarmerDto update(Long id, FarmerDto dto, Long millId) {
        Farmer farmer = farmerRepository.findByIdAndMillId(id, millId)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer not found: " + id));
        applyFields(farmer, dto);
        return FarmerDto.from(farmerRepository.save(farmer));
    }

    @Transactional
    public void delete(Long id, Long millId) {
        Farmer farmer = farmerRepository.findByIdAndMillId(id, millId)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer not found: " + id));
        farmerRepository.delete(farmer);
    }

    private void applyFields(Farmer farmer, FarmerDto dto) {
        farmer.setName(dto.getName()); farmer.setPhone(dto.getPhone());
        farmer.setAddress(dto.getAddress()); farmer.setBankAccount(dto.getBankAccount());
        farmer.setBankName(dto.getBankName()); farmer.setIfscCode(dto.getIfscCode());
    }
}
