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

    private final UsernamePolicy usernamePolicy;

    private final MillRepository millRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordGeneratorService passwordGenerator;

    public MillService(MillRepository millRepository, UserRepository userRepository,
                       PasswordEncoder passwordEncoder, PasswordGeneratorService passwordGenerator, UsernamePolicy usernamePolicy) {
        this.usernamePolicy = usernamePolicy;
        this.millRepository = millRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordGenerator = passwordGenerator;
    }

    /** The mills owned by this super admin (company). Other companies' mills are never listed. */
    @Transactional(readOnly = true)
    public List<MillDto> getAllMills(Long ownerId) {
        return millRepository.findByOwnerIdOrderByNameAsc(ownerId).stream().map(mill -> {
            String adminUsername = userRepository.findMillAdmin(mill.getId())
                    .map(User::getUsername).orElse(null);
            return MillDto.from(mill, adminUsername);
        }).collect(Collectors.toList());
    }

    @Transactional
    public CreateMillResponse createMill(Long ownerId, CreateMillRequest request) {
        Mill mill = new Mill();
        mill.setOwner(userRepository.getReferenceById(ownerId));
        String adminUsername = usernamePolicy.claim(request.getAdminUsername());
        mill.setName(request.getName());
        mill.setLocation(request.getLocation());
        mill.setContactPhone(request.getContactPhone());
        mill.setContactEmail(request.getContactEmail());
        mill = millRepository.save(mill);

        String rawPassword = passwordGenerator.generate();
        User admin = new User();
        admin.setMill(mill);
        admin.setUsername(adminUsername);
        admin.setPassword(passwordEncoder.encode(rawPassword));
        admin.setRole(User.Role.MILL_ADMIN);
        admin.setFullName(request.getAdminFullName() != null && !request.getAdminFullName().isBlank() ? request.getAdminFullName().trim() : adminUsername);
        admin.setActive(true);
        userRepository.save(admin);

        return new CreateMillResponse(mill.getId(), mill.getName(), admin.getUsername(), rawPassword,
                "Mill created successfully. Save the password — it will not be shown again.");
    }

    @Transactional
    public MillDto updateMill(Long ownerId, Long id, CreateMillRequest request) {
        Mill mill = findOwned(ownerId, id);
        mill.setName(request.getName());
        if (request.getLocation() != null) mill.setLocation(request.getLocation());
        if (request.getContactPhone() != null) mill.setContactPhone(request.getContactPhone());
        if (request.getContactEmail() != null) mill.setContactEmail(request.getContactEmail());
        mill = millRepository.save(mill);
        String adminUsername = userRepository.findMillAdmin(id).map(User::getUsername).orElse(null);
        return MillDto.from(mill, adminUsername);
    }

    @Transactional
    public void toggleStatus(Long ownerId, Long id) {
        Mill mill = findOwned(ownerId, id);
        mill.setStatus(mill.getStatus() == Mill.MillStatus.ACTIVE ? Mill.MillStatus.INACTIVE : Mill.MillStatus.ACTIVE);
        millRepository.save(mill);
    }

    @Transactional
    public String resetAdminPassword(Long ownerId, Long millId) {
        findOwned(ownerId, millId);
        User admin = userRepository.findMillAdmin(millId)
                .orElseThrow(() -> new ResourceNotFoundException("Mill admin not found for mill: " + millId));
        String rawPassword = passwordGenerator.generate();
        admin.setPassword(passwordEncoder.encode(rawPassword));
        userRepository.save(admin);
        return rawPassword;
    }

    /** Another company's mill is reported as not found, so its existence is not revealed. */
    private Mill findOwned(Long ownerId, Long id) {
        return millRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Mill not found: " + id));
    }
}
