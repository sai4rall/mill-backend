package com.millsystem.service;

import com.millsystem.dto.CreateMillRequest;
import com.millsystem.dto.CreateMillResponse;
import com.millsystem.dto.MillDto;
import com.millsystem.entity.Mill;
import com.millsystem.entity.User;
import com.millsystem.exception.ResourceNotFoundException;
import com.millsystem.repository.MillRepository;
import com.millsystem.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MillService {

    private final MillRepository millRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordGeneratorService passwordGenerator;

    public MillService(MillRepository millRepository, UserRepository userRepository,
                       PasswordEncoder passwordEncoder, PasswordGeneratorService passwordGenerator) {
        this.millRepository = millRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordGenerator = passwordGenerator;
    }

    public List<MillDto> getAllMills() {
        return millRepository.findAll().stream().map(mill -> {
            String adminUsername = userRepository.findMillAdmin(mill.getId())
                    .map(User::getUsername).orElse(null);
            return MillDto.from(mill, adminUsername);
        }).collect(Collectors.toList());
    }

    @Transactional
    public CreateMillResponse createMill(CreateMillRequest request) {
        Mill mill = new Mill();
        mill.setName(request.getName());
        mill.setLocation(request.getLocation());
        mill.setContactPhone(request.getContactPhone());
        mill.setContactEmail(request.getContactEmail());
        mill = millRepository.save(mill);

        String rawPassword = passwordGenerator.generate();
        User admin = new User();
        admin.setMill(mill);
        admin.setUsername(request.getAdminUsername());
        admin.setPassword(passwordEncoder.encode(rawPassword));
        admin.setRole(User.Role.MILL_ADMIN);
        admin.setFullName(request.getAdminFullName() != null ? request.getAdminFullName() : request.getAdminUsername());
        admin.setActive(true);
        userRepository.save(admin);

        return new CreateMillResponse(mill.getId(), mill.getName(), admin.getUsername(), rawPassword,
                "Mill created successfully. Save the password — it will not be shown again.");
    }

    @Transactional
    public MillDto updateMill(Long id, CreateMillRequest request) {
        Mill mill = millRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mill not found: " + id));
        mill.setName(request.getName());
        if (request.getLocation() != null) mill.setLocation(request.getLocation());
        if (request.getContactPhone() != null) mill.setContactPhone(request.getContactPhone());
        if (request.getContactEmail() != null) mill.setContactEmail(request.getContactEmail());
        mill = millRepository.save(mill);
        String adminUsername = userRepository.findMillAdmin(id).map(User::getUsername).orElse(null);
        return MillDto.from(mill, adminUsername);
    }

    @Transactional
    public void toggleStatus(Long id) {
        Mill mill = millRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mill not found: " + id));
        mill.setStatus(mill.getStatus() == Mill.MillStatus.ACTIVE ? Mill.MillStatus.INACTIVE : Mill.MillStatus.ACTIVE);
        millRepository.save(mill);
    }

    @Transactional
    public String resetAdminPassword(Long millId) {
        User admin = userRepository.findMillAdmin(millId)
                .orElseThrow(() -> new ResourceNotFoundException("Mill admin not found for mill: " + millId));
        String rawPassword = passwordGenerator.generate();
        admin.setPassword(passwordEncoder.encode(rawPassword));
        userRepository.save(admin);
        return rawPassword;
    }
}
