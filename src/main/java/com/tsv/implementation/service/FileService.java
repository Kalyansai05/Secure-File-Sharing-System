package com.tsv.implementation.service;

import com.tsv.implementation.dao.FileRepository;
import com.tsv.implementation.dao.FileShareRepository;
import com.tsv.implementation.dao.UserRepository;
import com.tsv.implementation.model.FileMetadata;
import com.tsv.implementation.model.FileShare;
import com.tsv.implementation.model.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.SecretKey;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class FileService {

    @Autowired
    private CryptoService cryptoService;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private FileShareRepository fileShareRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${file.upload.dir:uploads}")
    private String uploadDir;

    @Value("${jwt.secret:mySecretKeyForJWTTokenGeneration12345}")
    private String jwtSecret;

    @Value("${app.base.url:http://localhost:8080}")
    private String baseUrl;


    public FileMetadata uploadFile(MultipartFile file, User owner) throws Exception {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // Create upload directory
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate AES key and IV
        SecretKey aesKey = cryptoService.generateAESKey();
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);

        // Encrypt file
        byte[] fileBytes = file.getBytes();
        byte[] encryptedData = cryptoService.encryptFile(fileBytes, aesKey, iv);

        // Wrap AES key with owner's RSA public key
        byte[] wrappedKey = cryptoService.wrapAESKeyWithRSA(aesKey, owner.getRsaPublicKey());

        // Save encrypted file with UUID filename
        String encryptedFilename = UUID.randomUUID().toString();
        Path filePath = uploadPath.resolve(encryptedFilename);
        Files.write(filePath, encryptedData);

        // Create metadata
        FileMetadata metadata = new FileMetadata();
        metadata.setOriginalFilename(file.getOriginalFilename());
        metadata.setEncryptedFilename(encryptedFilename);
        metadata.setFileSize((long) encryptedData.length);
        metadata.setMimeType(file.getContentType());
        metadata.setOwner(owner);
        metadata.setEncryptedAESKey(wrappedKey);
        metadata.setGcmIV(iv);
        metadata.setUploadedAt(LocalDateTime.now());

        return fileRepository.save(metadata);
    }

    public FileShare shareFile(Long fileId, User owner, String recipientEmail,
                               String ownerPassword, Integer maxDownloads,
                               LocalDateTime expiresAt) throws Exception {

        // 1. Validate file exists and owner has access
        FileMetadata file = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));

        if (file.getOwner().getId() != owner.getId()) {
            throw new SecurityException("You don't own this file");
        }

        // 2. Find recipient user
        User recipient = userRepository.findByEmail(recipientEmail);
        if (recipient == null) {
            throw new IllegalArgumentException("Recipient not found: " + recipientEmail);
        }

        if (recipient.getRsaPublicKey() == null) {
            throw new IllegalStateException("Recipient has no encryption keys");
        }

        // 3. Decrypt owner's private key using their password
        PrivateKey ownerPrivateKey = cryptoService.decryptPrivateKey(
                owner.getRsaPrivateKeyEncrypted(),
                ownerPassword
        );

        // 4. Unwrap the file's AES key using owner's private key
        SecretKey fileAESKey = cryptoService.unwrapAESKey(
                file.getEncryptedAESKey(),
                ownerPrivateKey
        );

        // 5. Re-wrap the AES key with recipient's RSA public key
        byte[] recipientWrappedKey = cryptoService.wrapAESKeyWithRSA(
                fileAESKey,
                recipient.getRsaPublicKey()
        );

        // 6. Generate secure share token (JWT)
        String shareToken = generateShareToken(owner, recipient, fileId, expiresAt);

        // 7. Create FileShare record
        FileShare share = new FileShare();
        share.setFile(file);
        share.setOwner(owner);
        share.setRecipient(recipient);
        share.setRecipientWrappedAESKey(recipientWrappedKey);
        share.setShareToken(shareToken);
        share.setCreatedAt(LocalDateTime.now());
        share.setExpiresAt(expiresAt);
        share.setMaxDownloads(maxDownloads);

        fileShareRepository.save(share);

        // 8. Send email notification to recipient
        sendShareEmail(recipient, owner, file.getOriginalFilename(), shareToken);

        return share;
    }

    /**
     * Generate JWT token for secure share link.
     * Token contains recipient email, file ID, and expiration.
     */
    private String generateShareToken(User owner, User recipient, Long fileId,
                                      LocalDateTime expiresAt) {
        Date expiry = expiresAt != null
                ? Date.from(expiresAt.atZone(ZoneId.systemDefault()).toInstant())
                : new Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000); // 30 days default

        return Jwts.builder()
                .setSubject(recipient.getEmail())
                .claim("fileId", fileId)
                .claim("ownerId", owner.getId())
                .setIssuedAt(new Date())
                .setExpiration(expiry)
                .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .compact();
    }

    /**
     * Send email with secure share link to recipient.
     */
    private void sendShareEmail(User recipient, User owner, String fileName, String token) {
        try {
            String shareLink = baseUrl + "/share/download?token=" + token;

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("chintalakalyansai119@gmail.com");
            message.setTo(recipient.getEmail());
            message.setSubject(owner.getName() + " shared a file with you");
            message.setText(
                    "Hello " + recipient.getName() + ",\n\n" +
                            owner.getName() + " has shared the file \"" + fileName + "\" with you.\n\n" +
                            "Click here to download: " + shareLink + "\n\n" +
                            "Note: You must log in and verify your identity to access this file.\n\n" +
                            "Regards,\nSecure File Sharing System"
            );

            mailSender.send(message);
            System.out.println("✅ Share email sent to: " + recipient.getEmail());

        } catch (Exception e) {
            System.err.println("❌ Failed to send share email: " + e.getMessage());
            // Don't throw - share was created, email is secondary
        }
    }

    /**
     * Get all shares for a specific file (for owner to manage).
     */
    public List<FileShare> getFileShares(Long fileId, User owner) {
        FileMetadata file = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));
        return fileShareRepository.findByFileAndOwner(file, owner);
    }

    /**
     * Revoke access to a share (owner can cancel recipient's access).
     */
    public void revokeShare(Long shareId, User owner) {
        FileShare share = fileShareRepository.findById(shareId)
                .orElseThrow(() -> new IllegalArgumentException("Share not found"));
        if (share.getOwner().getId() != owner.getId()) {
            throw new SecurityException("You don't own this share");
        }
        share.setRevoked(true);
        fileShareRepository.save(share);
    }

    public List<FileMetadata> getUserFiles(User owner) {
        return fileRepository.findByOwnerOrderByUploadedAtDesc(owner);
    }
}

