package com.millsystem.service;

import com.millsystem.dto.SuperAdminDto;
import com.millsystem.entity.User;
import com.millsystem.exception.ResourceNotFoundException;
import com.millsystem.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Super admin accounts, managed by DEV_ADMIN. Passwords are generated and returned only once. */
@Service
public class SuperAdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordGeneratorService passwordGenerator;

    public SuperAdminService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                             PasswordGeneratorService passwordGenerator) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordGenerator = passwordGenerator;
    }

    public List<SuperAdminDto> getAll() {
        return userRepository.findByRoleOrderByUsernameAsc(User.Role.SUPER_ADMIN).stream().map(SuperAdminDto::from).toList();
    }

    @Transactional
    public SuperAdminDto create(SuperAdminDto request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username '" + request.getUsername() + "' is already taken");
        }
        String rawPassword = passwordGenerator.generate();
        User user = new User();
        user.setUsername(request.getUsername());
        user.setFullName(request.getFullName() != null ? request.getFullName() : request.getUsername());
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(User.Role.SUPER_ADMIN);
        user.setActive(true);
        userRepository.save(user);

        SuperAdminDto dto = SuperAdminDto.from(user);
        dto.setGeneratedPassword(rawPassword);
        return dto;
    }

    @Transactional
    public SuperAdminDto toggleStatus(Long id) {
        User user = find(id);
        user.setActive(!user.isActive());
        return SuperAdminDto.from(userRepository.save(user));
    }

    @Transactional
    public String resetPassword(Long id) {
        User user = find(id);
        String rawPassword = passwordGenerator.generate();
        user.setPassword(passwordEncoder.encode(rawPassword));
        userRepository.save(user);
        return rawPassword;
    }

    private User find(Long id) {
        return userRepository.findById(id)
                .filter(u -> u.getRole() == User.Role.SUPER_ADMIN)
                .orElseThrow(() -> new ResourceNotFoundException("Super admin not found: " + id));
    }
}
