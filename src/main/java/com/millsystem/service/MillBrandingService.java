package com.millsystem.service;

import com.millsystem.dto.BrandingDto;
import com.millsystem.entity.Mill;
import com.millsystem.entity.MillLogo;
import com.millsystem.exception.ResourceNotFoundException;
import com.millsystem.repository.MillLogoRepository;
import com.millsystem.repository.MillRepository;
import com.millsystem.util.ImageTypes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/** Mill logos (stored in the database) and the branding shown in the app header. */
@Service
public class MillBrandingService {

    private final MillRepository millRepository;
    private final MillLogoRepository logoRepository;

    @Value("${app.mill-logo.max-bytes:1048576}")
    private long maxLogoBytes;

    public MillBrandingService(MillRepository millRepository, MillLogoRepository logoRepository) {
        this.millRepository = millRepository;
        this.logoRepository = logoRepository;
    }

    @Transactional(readOnly = true)
    public List<BrandingDto> getAllMills() {
        return millRepository.findAll().stream()
                .sorted(Comparator.comparing(Mill::getName, String.CASE_INSENSITIVE_ORDER))
                .map(BrandingDto::withOwner).toList();
    }

    @Transactional(readOnly = true)
    public BrandingDto getBranding(Long millId) {
        return BrandingDto.from(findMill(millId));
    }

    @Transactional(readOnly = true)
    public MillLogo getLogo(Long millId) {
        findMill(millId);
        MillLogo logo = logoRepository.findById(millId)
                .orElseThrow(() -> new ResourceNotFoundException("No logo for mill: " + millId));
        logo.getData(); // load the bytes inside the transaction
        return logo;
    }

    @Transactional
    public BrandingDto uploadLogo(Long millId, MultipartFile file) {
        Mill mill = findMill(millId);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Logo file is required");
        }
        if (file.getSize() > maxLogoBytes) {
            throw new IllegalArgumentException("Logo must be " + (maxLogoBytes / 1024) + " KB or smaller");
        }
        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read the uploaded logo");
        }
        String contentType = ImageTypes.detect(data);
        if (contentType == null) {
            throw new IllegalArgumentException("Logo must be a PNG, JPEG or WebP image");
        }

        MillLogo logo = logoRepository.findById(millId).orElseGet(() -> {
            MillLogo l = new MillLogo();
            l.setMill(mill);
            return l;
        });
        logo.setContentType(contentType);
        logo.setData(data);
        logoRepository.save(logo);

        mill.setLogoUpdatedAt(LocalDateTime.now());
        return BrandingDto.from(millRepository.save(mill));
    }

    @Transactional
    public void deleteLogo(Long millId) {
        Mill mill = findMill(millId);
        logoRepository.findById(millId).ifPresent(logoRepository::delete);
        mill.setLogoUpdatedAt(null);
        millRepository.save(mill);
    }

    private Mill findMill(Long millId) {
        return millRepository.findById(millId)
                .orElseThrow(() -> new ResourceNotFoundException("Mill not found: " + millId));
    }
}
