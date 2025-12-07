package com.tsv.implementation.controller;

import com.tsv.implementation.dao.UserRepository;
import com.tsv.implementation.model.FileMetadata;
import com.tsv.implementation.model.User;
import com.tsv.implementation.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
@RequestMapping("/files")
public class FileController {

    @Autowired
    private FileService fileService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file, Model model) {
        try {
            UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext()
                    .getAuthentication().getPrincipal();
            User user = userRepository.findByEmail(userDetails.getUsername());

            FileMetadata uploadedFile = fileService.uploadFile(file, user);

            System.out.println("✅ File encrypted and uploaded: " + uploadedFile.getOriginalFilename());
            return "redirect:/dashboard?upload=success";

        } catch (Exception e) {
            System.err.println("Upload error: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/dashboard?upload=error";
        }
    }
    @PostMapping("/share")
    public String shareFile(@RequestParam("fileId") Long fileId,
                            @RequestParam("recipientEmail") String recipientEmail,
                            @RequestParam("ownerPassword") String ownerPassword,
                            @RequestParam(value = "maxDownloads", required = false) Integer maxDownloads,
                            @RequestParam(value = "expiresAt", required = false) String expiresAtStr,
                            Model model) {
        try {
            UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext()
                    .getAuthentication().getPrincipal();
            User owner = userRepository.findByEmail(userDetails.getUsername());

            java.time.LocalDateTime expiresAt = null;
            if (expiresAtStr != null && !expiresAtStr.isEmpty()) {
                expiresAt = java.time.LocalDateTime.parse(expiresAtStr);
            }

            fileService.shareFile(fileId, owner, recipientEmail, ownerPassword,
                    maxDownloads, expiresAt);

            // Redirect back to dashboard with success flag
            return "redirect:/dashboard?share=success";

        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/dashboard?share=error";
        }
    }
}
