package com.millsystem.entity;

import jakarta.persistence.*;

/** Mill logo bytes, in their own table so loading mills never loads image data. One logo per mill. */
@Entity
@Table(name = "mill_logos")
public class MillLogo {

    @Id
    @Column(name = "mill_id")
    private Long millId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "mill_id")
    private Mill mill;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Lob
    // 16 MB length makes Hibernate pick MEDIUMBLOB on MySQL (plain BLOB on other databases).
    @Column(name = "data", nullable = false, length = 16_777_215)
    private byte[] data;

    public Long getMillId() { return millId; }
    public Mill getMill() { return mill; }
    public void setMill(Mill mill) { this.mill = mill; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }
}
