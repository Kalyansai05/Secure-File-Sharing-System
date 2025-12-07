package com.tsv.implementation.controller;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import com.tsv.implementation.model.FileMetadata;
import com.tsv.implementation.service.FileService;
import com.tsv.implementation.dao.UserRepository;
import com.tsv.implementation.model.User;


import java.util.List;


@Controller
@RequestMapping("/dashboard")
public class DashboardController {
    @Autowired
    UserRepository userRepo;

    @Autowired
    FileService fileService;

    @GetMapping
    public String displayDashboard(Model model, HttpSession session) {  // ✅ ADD HttpSession parameter

        // ✅ NEW: Check if there's a pending share token from before login
        String pendingToken = (String) session.getAttribute("pendingShareToken");
        if (pendingToken != null) {
            System.out.println("🔄 Redirecting to pending share download: " + pendingToken);
            session.removeAttribute("pendingShareToken");
            return "redirect:/share/download?token=" + pendingToken;
        }

        // Original logic
        SecurityContext securityContext = SecurityContextHolder.getContext();
        UserDetails user = (UserDetails) securityContext.getAuthentication().getPrincipal();
        User users = userRepo.findByEmail(user.getUsername());

        if(users.isActive()) {
            model.addAttribute("userDetails", users.getName());
            List<FileMetadata> files = fileService.getUserFiles(users);
            model.addAttribute("files", files);
            return "dashboard";
        } else {
            return "redirect:/login/otpVerification?error";
        }
    }


}