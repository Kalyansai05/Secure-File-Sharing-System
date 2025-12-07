package com.tsv.implementation.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "file_shares")
public class FileShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "file_id", nullable = false)
    private FileMetadata file;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    // AES key wrapped specifically for this recipient's RSA public key
    @Lob
    @Column(name = "recipient_wrapped_aes_key", columnDefinition = "LONGBLOB")
    private byte[] recipientWrappedAESKey;

    @Column(unique = true, nullable = false)
    private String shareToken; // JWT token for secure access

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime expiresAt;

    private Integer maxDownloads; // null = unlimited

    @Column(nullable = false)
    private Integer currentDownloads = 0;

    private String allowedIpAddress; // Optional: restrict to specific IP

    @Column(nullable = false)
    private boolean revoked = false;

    // Constructors
    public FileShare() {
        this.createdAt = LocalDateTime.now();
        this.currentDownloads = 0;
        this.revoked = false;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public FileMetadata getFile() { return file; }
    public void setFile(FileMetadata file) { this.file = file; }

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }

    public User getRecipient() { return recipient; }
    public void setRecipient(User recipient) { this.recipient = recipient; }

    public byte[] getRecipientWrappedAESKey() { return recipientWrappedAESKey; }
    public void setRecipientWrappedAESKey(byte[] recipientWrappedAESKey) {
        this.recipientWrappedAESKey = recipientWrappedAESKey;
    }

    public String getShareToken() { return shareToken; }
    public void setShareToken(String shareToken) { this.shareToken = shareToken; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public Integer getMaxDownloads() { return maxDownloads; }
    public void setMaxDownloads(Integer maxDownloads) { this.maxDownloads = maxDownloads; }

    public Integer getCurrentDownloads() { return currentDownloads; }
    public void setCurrentDownloads(Integer currentDownloads) {
        this.currentDownloads = currentDownloads;
    }

    public String getAllowedIpAddress() { return allowedIpAddress; }
    public void setAllowedIpAddress(String allowedIpAddress) {
        this.allowedIpAddress = allowedIpAddress;
    }

    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }
}
