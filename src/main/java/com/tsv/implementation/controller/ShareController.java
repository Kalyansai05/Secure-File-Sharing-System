package com.tsv.implementation.controller;

import com.tsv.implementation.dao.FileShareRepository;
import com.tsv.implementation.dao.UserRepository;
import com.tsv.implementation.model.FileMetadata;
import com.tsv.implementation.model.FileShare;
import com.tsv.implementation.model.User;
import com.tsv.implementation.service.CryptoService;
import com.tsv.implementation.service.FileService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;


import javax.crypto.SecretKey;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.time.LocalDateTime;
import java.util.Optional;

@Controller
@RequestMapping("/share")
public class ShareController {

    @Autowired
    private FileShareRepository fileShareRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileService fileService;

    @Autowired
    private CryptoService cryptoService;

    @Value("${file.upload.dir:uploads}")
    private String uploadDir;

    @Value("${jwt.secret:MyVerySecureSecretKeyForJWT2048BitsMinimum}")
    private String jwtSecret;

    /**
     * 1) Owner-side: Show a simple "share" page for a file (optional UI)
     *    You can call this from the dashboard when owner clicks "Share".
     */
    @GetMapping("/manage/{fileId}")
    public String manageShares(@PathVariable Long fileId, Model model) {
        UserDetails principal = (UserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        User owner = userRepository.findByEmail(principal.getUsername());

        FileMetadata file = fileService.getUserFiles(owner).stream()
                .filter(f -> f.getId().equals(fileId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("File not found or not owned by user"));

        model.addAttribute("file", file);
        model.addAttribute("shares", fileService.getFileShares(fileId, owner));
        return "share-manage"; // You can create this Thymeleaf page later
    }

    /**
     * 2) Recipient-side: Landing page when clicking email link.
     *    URL example: /share/download?token=... (GET)
     *    Shows a page asking user to log in (if not) and then submit password to decrypt.
     */
    @GetMapping("/download")
    public String showDownloadPage(@RequestParam("token") String token,
                                   Model model,
                                   HttpSession session) {  // ✅ ADD HttpSession parameter
        try {
            // Validate token and share state
            FileShare share = validateShareToken(token);

            // ✅ NEW: Check if user is logged in
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

            // If not logged in or anonymous user
            if (!(principal instanceof UserDetails) || "anonymousUser".equals(principal.toString())) {
                // Store token in session for later use
                session.setAttribute("pendingShareToken", token);
                System.out.println("📌 User not logged in. Stored token in session. Redirecting to login...");
                return "redirect:/login";
            }

            // ✅ NEW: User is logged in - verify they are the correct recipient
            UserDetails userDetails = (UserDetails) principal;
            User currentUser = userRepository.findByEmail(userDetails.getUsername());

            if (currentUser.getId() != share.getRecipient().getId()) {
                model.addAttribute("error", "Access Denied: You are not the intended recipient. " +
                        "Please log in as " + share.getRecipient().getEmail());
                return "share-error";
            }

            // All checks passed - show download page
            model.addAttribute("fileName", share.getFile().getOriginalFilename());
            model.addAttribute("token", token);
            model.addAttribute("expiresAt", share.getExpiresAt());
            model.addAttribute("maxDownloads", share.getMaxDownloads());
            model.addAttribute("currentDownloads", share.getCurrentDownloads());

            // Clear token from session if it exists
            session.removeAttribute("pendingShareToken");

            System.out.println("✅ Showing download page for: " + share.getFile().getOriginalFilename());
            return "share-download";

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", e.getMessage());
            return "share-error";
        }
    }


    /**
     * 3) Recipient-side: POST to actually download shared file after:
     *    - Logged in
     *    - Passed password to unlock private key
     *    - Optionally re-passed OTP + Face (you already enforce this via AuthFilter)
     */
    @PostMapping("/download")
    public ResponseEntity<?> downloadSharedFile(@RequestParam("token") String token,
                                                @RequestParam("password") String password,
                                                HttpServletRequest request) {
        // a) Validate JWT + share status
        FileShare share = validateShareToken(token);

        // b) Ensure logged-in user is the intended recipient
        Object principalObj = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principalObj instanceof UserDetails)) {
            return ResponseEntity.status(401).body("Not authenticated");
        }
        UserDetails principal = (UserDetails) principalObj;
        User recipient = userRepository.findByEmail(principal.getUsername());

        if (share.getRecipient().getId() != recipient.getId()) {
            return ResponseEntity.status(403).body("You are not the intended recipient");
        }

        // c) Check download limits
        if (share.getMaxDownloads() != null &&
                share.getCurrentDownloads() >= share.getMaxDownloads()) {
            return ResponseEntity.status(403).body("Download limit reached");
        }

        // d) Optional: IP restriction
        if (share.getAllowedIpAddress() != null) {
            String clientIp = request.getRemoteAddr();
            if (!share.getAllowedIpAddress().equals(clientIp)) {
                return ResponseEntity.status(403).body("Access not allowed from this IP");
            }
        }

        try {
            // e) Decrypt recipient's private key using provided password
            PrivateKey recipientPrivateKey = cryptoService.decryptPrivateKey(
                    recipient.getRsaPrivateKeyEncrypted(),
                    password
            );

            // f) Unwrap AES key using FileShare.recipientWrappedAESKey
            SecretKey aesKey = cryptoService.unwrapAESKey(
                    share.getRecipientWrappedAESKey(),
                    recipientPrivateKey
            );

            // g) Read encrypted file bytes from disk
            Path path = Paths.get(uploadDir, share.getFile().getEncryptedFilename());
            byte[] encryptedBytes = Files.readAllBytes(path);

            // h) Decrypt with AES-GCM
            byte[] decryptedBytes = cryptoService.decryptFile(
                    encryptedBytes,
                    aesKey,
                    share.getFile().getGcmIV()
            );

            // i) Update download counter
            share.setCurrentDownloads(share.getCurrentDownloads() + 1);
            fileShareRepository.save(share);

            // j) Stream file to user
            ByteArrayResource resource = new ByteArrayResource(decryptedBytes);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + share.getFile().getOriginalFilename() + "\"")
                    .contentType(MediaType.parseMediaType(share.getFile().getMimeType()))
                    .body(resource);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Decryption failed: " + e.getMessage());
        }
    }

    // ================== HELPER METHODS ==================

    /**
     * Validate JWT token + FileShare state (not revoked, not expired).
     * Centralized so both GET and POST can use it.
     */
    private FileShare validateShareToken(String token) {
        // 1. Parse JWT
        Claims claims = Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token)
                .getBody();

        String recipientEmail = claims.getSubject();
        Long fileId = claims.get("fileId", Long.class);
        Integer ownerId = claims.get("ownerId", Integer.class);

        // 2. Fetch share record by token
        Optional<FileShare> optionalShare = fileShareRepository.findByShareToken(token);
        if (optionalShare.isEmpty()) {
            throw new IllegalArgumentException("Invalid share link");
        }

        FileShare share = optionalShare.get();

        // 3. Check revoked/expired
        if (share.isRevoked()) {
            throw new IllegalStateException("This share link has been revoked");
        }
        if (share.getExpiresAt() != null &&
                share.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("This share link has expired");
        }

        // 4. Ensure JWT claims match DB
        // 4. Ensure JWT claims match DB
        if (!share.getRecipient().getEmail().equals(recipientEmail) ||
                !share.getFile().getId().equals(fileId) ||          // ✅ Use .equals() for Long
                share.getOwner().getId() != ownerId) {              // ✅ Use != for int
            throw new IllegalStateException("Share token does not match stored data");
        }


        return share;
    }
}
