package com.tsv.implementation.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "files")
public class FileMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String originalFilename;
    private String encryptedFilename;
    private Long fileSize;
    private String mimeType;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    @Lob
    private byte[] encryptedAESKey;

    @Lob
    private byte[] gcmIV;

    private LocalDateTime uploadedAt;

    // Constructors
    public FileMetadata() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getEncryptedFilename() { return encryptedFilename; }
    public void setEncryptedFilename(String encryptedFilename) {
        this.encryptedFilename = encryptedFilename;
    }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }

    public byte[] getEncryptedAESKey() { return encryptedAESKey; }
    public void setEncryptedAESKey(byte[] encryptedAESKey) {
        this.encryptedAESKey = encryptedAESKey;
    }

    public byte[] getGcmIV() { return gcmIV; }
    public void setGcmIV(byte[] gcmIV) { this.gcmIV = gcmIV; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}
