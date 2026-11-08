package com.millsystem.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * A new mill and its first admin account (super admin). Mandatory: mill name, location, contact mobile, admin username
 * and admin full name. Email is optional but must be valid. Keep in sync with frontend shared/utils/millValidation.js.
 */
public class CreateMillRequest {
    @NotBlank(message = "Mill name is required")
    @Size(min = 2, max = 100, message = "Mill name must be 2-100 characters")
    private String name;

    @NotBlank(message = "Location is required")
    @Size(min = 2, max = 200, message = "Location must be 2-200 characters")
    private String location;

    @NotBlank(message = "Contact phone is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit mobile number")
    private String contactPhone;

    @Email(message = "Enter a valid email address")
    @Size(max = 100, message = "Email must be at most 100 characters")
    private String contactEmail;

    @NotBlank(message = "Admin username is required")
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]{2,49}$", message = "Admin username must be 3-50 characters: letters, digits, . _ -")
    private String adminUsername;

    @NotBlank(message = "Admin full name is required")
    @Size(min = 2, max = 100, message = "Admin full name must be 2-100 characters")
    @Pattern(regexp = "^[\\p{L}\\p{M}][\\p{L}\\p{M} .'-]*$", message = "Admin full name can contain only letters, spaces and . ' -")
    private String adminFullName;

    private static String clean(String s) {
        if (s == null) return null;
        String t = s.trim().replaceAll("\\s+", " ");
        return t.isEmpty() ? null : t;
    }

    /** Accepts +91 / 91 / 0 prefixes, spaces and dashes, and keeps the 10-digit number; anything else fails @Pattern. */
    private static String phone(String raw) {
        if (raw == null) return null;
        String d = raw.replaceAll("[\\s\\-()]", "");
        if (d.startsWith("+91")) d = d.substring(3);
        else if (d.length() == 12 && d.startsWith("91")) d = d.substring(2);
        else if (d.length() == 11 && d.startsWith("0")) d = d.substring(1);
        return d.isEmpty() ? null : d;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = clean(name); }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = clean(location); }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = phone(contactPhone); }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail == null || contactEmail.isBlank() ? null : contactEmail.trim(); }
    public String getAdminUsername() { return adminUsername; }
    public void setAdminUsername(String adminUsername) { this.adminUsername = adminUsername == null ? null : adminUsername.trim(); }
    public String getAdminFullName() { return adminFullName; }
    public void setAdminFullName(String adminFullName) { this.adminFullName = clean(adminFullName); }
}
