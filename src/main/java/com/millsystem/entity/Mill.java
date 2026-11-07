package com.millsystem.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "mills")
public class Mill {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String location;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "contact_email")
    private String contactEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MillStatus status = MillStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Set when a logo is uploaded (null = no logo). Also used by clients as a cache-busting version.
    @Column(name = "logo_updated_at")
    private LocalDateTime logoUpdatedAt;

    /** SHA-256 (hex) of the mill's GPS device key; the key itself is shown once when generated and never stored. */
    @Column(name = "gps_key_hash", length = 64, unique = true)
    private String gpsKeyHash;

    @Column(name = "gps_key_created_at")
    private LocalDateTime gpsKeyCreatedAt;

    public String getGpsKeyHash() { return gpsKeyHash; }
    public void setGpsKeyHash(String gpsKeyHash) { this.gpsKeyHash = gpsKeyHash; }
    public LocalDateTime getGpsKeyCreatedAt() { return gpsKeyCreatedAt; }
    public void setGpsKeyCreatedAt(LocalDateTime gpsKeyCreatedAt) { this.gpsKeyCreatedAt = gpsKeyCreatedAt; }

    public LocalDateTime getLogoUpdatedAt() { return logoUpdatedAt; }
    public void setLogoUpdatedAt(LocalDateTime logoUpdatedAt) { this.logoUpdatedAt = logoUpdatedAt; }

    /**
     * The super admin (company) that owns this mill. Each super admin is a separate company: they see and manage
     * only the mills they own. Null only for mills created before ownership existed, until a dev admin assigns one.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }

    public enum MillStatus { ACTIVE, INACTIVE }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public MillStatus getStatus() { return status; }
    public void setStatus(MillStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
