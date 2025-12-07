package com.tsv.implementation.config;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.tsv.implementation.dao.UserRepository;
import com.tsv.implementation.model.User;
import com.tsv.implementation.service.DefaultUserService;

@Component
public class CustomSuccessHandler implements AuthenticationSuccessHandler{

    @Autowired
    UserRepository userRepo;

    @Autowired
    DefaultUserService userService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        String redirectUrl = null;
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String username = userDetails.getUsername();
        User user = userRepo.findByEmail(username);

        System.out.println("🔐 Login successful for: " + username);

        String output = userService.generateOtp(user);

        if ("success".equals(output)) {
            // Store username in session for OTP verification
            HttpSession session = request.getSession();
            session.setAttribute("username", username);
            session.setAttribute("loginOtpFlow", true);

            System.out.println("✅ Login OTP generated and session created for: " + username);

            redirectUrl = "/login/otpVerification";
        } else {
            System.out.println("❌ OTP generation failed for: " + username);
            redirectUrl = "/login?error";
        }

        new DefaultRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
