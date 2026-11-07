package com.millsystem.service;

import com.millsystem.repository.PaddyPurchaseRepository;
import com.millsystem.exception.ConflictException;
import com.millsystem.dto.FarmerDto;
import com.millsystem.entity.Farmer;
import com.millsystem.entity.FarmerPhoto;
import com.millsystem.entity.Mill;
import com.millsystem.exception.ResourceNotFoundException;
import com.millsystem.repository.FarmerPhotoRepository;
import com.millsystem.repository.FarmerRepository;
import com.millsystem.repository.MillRepository;
import com.millsystem.util.ImageTypes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FarmerService {

    private final PaddyPurchaseRepository purchaseRepository;

    private final FarmerRepository farmerRepository;
    private final FarmerPhotoRepository photoRepository;
    private final MillRepository millRepository;
    private final OtpService otpService;

    @Value("${app.farmer-photo.max-bytes:2097152}")
    private long maxPhotoBytes;

    public FarmerService(FarmerRepository farmerRepository, FarmerPhotoRepository photoRepository,
                         MillRepository millRepository, OtpService otpService, PaddyPurchaseRepository purchaseRepository) {
        this.purchaseRepository = purchaseRepository;
        this.farmerRepository = farmerRepository;
        this.photoRepository = photoRepository;
        this.millRepository = millRepository;
        this.otpService = otpService;
    }

    public List<FarmerDto> getAll(Long millId) {
        return farmerRepository.findByMillIdOrderByNameAsc(millId)
                .stream().map(FarmerDto::from).collect(Collectors.toList());
    }

    public FarmerDto getById(Long id, Long millId) {
        return FarmerDto.from(find(id, millId));
    }

    @Transactional
    public FarmerDto create(FarmerDto dto, Long millId) {
        Mill mill = millRepository.findById(millId)
                .orElseThrow(() -> new ResourceNotFoundException("Mill not found"));
        if (dto.getPhone() == null || dto.getPhone().isBlank()) {
            throw new IllegalArgumentException("Mobile number is required");
        }
        String phone = OtpService.normalizePhone(dto.getPhone());
        otpService.consumeToken(millId, phone, dto.getPhoneVerificationToken());

        Farmer farmer = new Farmer();
        farmer.setMill(mill);
        farmer.setPhone(phone);
        farmer.setPhoneVerified(true);
        applyFields(farmer, dto);
        return FarmerDto.from(farmerRepository.save(farmer));
    }

    @Transactional
    public FarmerDto update(Long id, FarmerDto dto, Long millId) {
        Farmer farmer = find(id, millId);
        // Re-verify only when the number actually changes. An unchanged number is kept as-is, even if it is
        // a legacy value that predates validation (so it is compared raw before normalising).
        String raw = dto.getPhone() == null ? "" : dto.getPhone().trim();
        String current = farmer.getPhone() == null ? "" : farmer.getPhone();
        if (!raw.equals(current)) {
            if (raw.isEmpty()) {
                throw new IllegalArgumentException("Mobile number is required");
            }
            String phone = OtpService.normalizePhone(raw);
            if (!phone.equals(current)) {
                otpService.consumeToken(millId, phone, dto.getPhoneVerificationToken());
                farmer.setPhone(phone);
                farmer.setPhoneVerified(true);
            }
        }
        applyFields(farmer, dto);
        return FarmerDto.from(farmerRepository.save(farmer));
    }

    @Transactional
    public void delete(Long id, Long millId) {
        Farmer farmer = find(id, millId);
        if (purchaseRepository.existsByFarmerId(farmer.getId())) {
            throw new ConflictException(farmer.getName() + " has purchases on record and cannot be deleted");
        }
        photoRepository.findById(farmer.getId()).ifPresent(photoRepository::delete);
        farmerRepository.delete(farmer);
    }

    @Transactional(readOnly = true)
    public FarmerPhoto getPhoto(Long id, Long millId) {
        Farmer farmer = find(id, millId);
        FarmerPhoto photo = photoRepository.findById(farmer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No photo for farmer: " + id));
        photo.getData(); // load the bytes inside the transaction
        return photo;
    }

    @Transactional
    public FarmerDto uploadPhoto(Long id, MultipartFile file, Long millId) {
        Farmer farmer = find(id, millId);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Photo file is required");
        }
        if (file.getSize() > maxPhotoBytes) {
            throw new IllegalArgumentException("Photo must be " + (maxPhotoBytes / 1024) + " KB or smaller");
        }
        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read the uploaded photo");
        }
        // Trust the file's actual bytes, not the client-supplied content type.
        String contentType = ImageTypes.detect(data);
        if (contentType == null) {
            throw new IllegalArgumentException("Photo must be a JPEG, PNG or WebP image");
        }

        FarmerPhoto photo = photoRepository.findById(farmer.getId()).orElseGet(() -> {
            FarmerPhoto p = new FarmerPhoto();
            p.setFarmer(farmer);
            return p;
        });
        photo.setContentType(contentType);
        photo.setData(data);
        photoRepository.save(photo);

        farmer.setPhotoUpdatedAt(LocalDateTime.now());
        return FarmerDto.from(farmerRepository.save(farmer));
    }

    @Transactional
    public void deletePhoto(Long id, Long millId) {
        Farmer farmer = find(id, millId);
        photoRepository.findById(farmer.getId()).ifPresent(photoRepository::delete);
        farmer.setPhotoUpdatedAt(null);
        farmerRepository.save(farmer);
    }

    private Farmer find(Long id, Long millId) {
        return farmerRepository.findByIdAndMillId(id, millId)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer not found: " + id));
    }

    private void applyFields(Farmer farmer, FarmerDto dto) {
        farmer.setName(dto.getName());
        farmer.setAddress(dto.getAddress()); farmer.setBankAccount(dto.getBankAccount());
        farmer.setBankName(dto.getBankName()); farmer.setIfscCode(dto.getIfscCode());
    }
}
