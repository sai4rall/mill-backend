package com.millsystem.service;

import com.millsystem.exception.ConflictException;
import com.millsystem.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * One rule for every account username (mill admins, operators, super admins): trimmed, lower-case,
 * 3-50 characters of letters, digits, dot, underscore or hyphen, starting with a letter or digit.
 * Uniqueness is checked case-insensitively so "Ravi" and "ravi" cannot both exist.
 */
@Component
public class UsernamePolicy {

    private static final Pattern VALID = Pattern.compile("^[a-z0-9][a-z0-9._-]{2,49}$");
    public static final String RULE = "Username must be 3-50 characters: lower-case letters, digits, '.', '_' or '-', starting with a letter or digit";

    private final UserRepository userRepository;

    public UsernamePolicy(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public static String normalize(String raw) {
        return raw == null ? null : raw.trim().toLowerCase(Locale.ROOT);
    }

    /** Normalises, validates and checks the username is free; returns the value to store. */
    public String claim(String raw) {
        String username = normalize(raw);
        if (username == null || !VALID.matcher(username).matches()) throw new IllegalArgumentException(RULE);
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ConflictException("Username '" + username + "' is already taken");
        }
        return username;
    }
}
