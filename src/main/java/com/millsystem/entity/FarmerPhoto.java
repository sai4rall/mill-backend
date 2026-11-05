package com.millsystem.entity;

import jakarta.persistence.*;

/**
 * Farmer photo bytes, kept in their own table so listing farmers never loads image data.
 * Shares its primary key with the farmer (one photo per farmer).
 */
@Entity
@Table(name = "farmer_photos")
public class FarmerPhoto {

    @Id
    @Column(name = "farmer_id")
    private Long farmerId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "farmer_id")
    private Farmer farmer;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Lob
    // 16 MB length makes Hibernate pick MEDIUMBLOB on MySQL (plain BLOB on other databases).
    @Column(name = "data", nullable = false, length = 16_777_215)
    private byte[] data;

    public Long getFarmerId() { return farmerId; }
    public Farmer getFarmer() { return farmer; }
    public void setFarmer(Farmer farmer) { this.farmer = farmer; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }
}
