package com.millsystem.service;

import com.millsystem.dto.OperatorDto;
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
public class OperatorService {

    private final UserRepository userRepository;
    private final MillRepository millRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordGeneratorService passwordGenerator;

    public OperatorService(UserRepository userRepository, MillRepository millRepository,
                           PasswordEncoder passwordEncoder, PasswordGeneratorService passwordGenerator) {
        this.userRepository = userRepository;
        this.millRepository = millRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordGenerator = passwordGenerator;
    }

    public List<OperatorDto> getOperators(Long millId) {
        return userRepository.findByMillIdAndRole(millId, User.Role.MILL_OPERATOR)
                .stream().map(OperatorDto::from).collect(Collectors.toList());
    }

    @Transactional
    public OperatorDto createOperator(Long millId, OperatorDto request) {
        Mill mill = millRepository.findById(millId)
                .orElseThrow(() -> new ResourceNotFoundException("Mill not found: " + millId));
        String rawPassword = passwordGenerator.generate();
        User operator = new User();
        operator.setMill(mill);
        operator.setUsername(request.getUsername());
        operator.setPassword(passwordEncoder.encode(rawPassword));
        operator.setRole(User.Role.MILL_OPERATOR);
        operator.setFullName(request.getFullName());
        operator.setActive(true);
        userRepository.save(operator);

        OperatorDto dto = OperatorDto.from(operator);
        dto.setGeneratedPassword(rawPassword);
        return dto;
    }

    @Transactional
    public void toggleStatus(Long millId, Long operatorId) {
        User operator = userRepository.findById(operatorId)
                .filter(u -> u.getMill() != null && u.getMill().getId().equals(millId))
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found"));
        operator.setActive(!operator.isActive());
        userRepository.save(operator);
    }

    @Transactional
    public String resetPassword(Long millId, Long operatorId) {
        User operator = userRepository.findById(operatorId)
                .filter(u -> u.getMill() != null && u.getMill().getId().equals(millId))
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found"));
        String rawPassword = passwordGenerator.generate();
        operator.setPassword(passwordEncoder.encode(rawPassword));
        userRepository.save(operator);
        return rawPassword;
    }
}
