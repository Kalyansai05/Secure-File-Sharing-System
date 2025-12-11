package com.tsv.implementation.config;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import com.tsv.implementation.dao.UserRepository;
import com.tsv.implementation.model.User;

@Component
public class AuthFilter implements Filter {

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Autowired
    UserRepository userRepo;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String requestURI = httpRequest.getRequestURI();

        // ✅ UPDATED: Add home page and static resources to public endpoints
        boolean isPublicEndpoint = pathMatcher.match("/", requestURI) ||
                pathMatcher.match("/home", requestURI) ||
                pathMatcher.match("/registration/**", requestURI) ||
                pathMatcher.match("/login", requestURI) ||
                pathMatcher.match("/login/**", requestURI) ||
                pathMatcher.match("/logout", requestURI) ||
                pathMatcher.match("/otp-verify", requestURI) ||
                pathMatcher.match("/verify-otp", requestURI) ||
                pathMatcher.match("/share/download/**", requestURI) ||
                pathMatcher.match("/css/**", requestURI) ||
                pathMatcher.match("/js/**", requestURI) ||
                pathMatcher.match("/images/**", requestURI) ||
                pathMatcher.match("/favicon.ico", requestURI);

        if (isPublicEndpoint) {
            chain.doFilter(request, response);
            return;
        }

        // ✅ PRESERVED: All original authentication and status checking logic
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Object principal = securityContext.getAuthentication();

        if (principal == null || !(securityContext.getAuthentication().getPrincipal() instanceof UserDetails)) {
            httpResponse.sendRedirect("/login");
            return;
        }

        // 3. FETCH USER STATUS (Now that we guarantee it's a UserDetails object)
        UserDetails userDetails = (UserDetails) securityContext.getAuthentication().getPrincipal();
        String email = userDetails.getUsername();

        // Retrieve the full custom User object from the database
        User users = userRepo.findByEmail(email);
        if (users == null) {
            httpResponse.sendRedirect("/login");
            return;
        }

        // 4. CORE LOGIC: Check Status Sequentially (Loop-Free Logic)
        final String FACE_CHECK_URI = "/face-check";
        final String FACE_API_URI = "/api/face-verify";
        final String OTP_VERIFICATION_URI = "/login/otpVerification";

        boolean isOnFaceCheckPage = requestURI.equals(FACE_CHECK_URI) || requestURI.startsWith(FACE_API_URI);
        boolean isOnOtpPage = requestURI.startsWith(OTP_VERIFICATION_URI);

        if (users.isActive()) {
            chain.doFilter(request, response);
            return;
        }

        if (users.isOtpVerified()) {
            if (isOnFaceCheckPage) {
                // Correct step. Allow access to the Face Check page/API.
                chain.doFilter(request, response);
                return;
            }
            // Incorrect step (trying to access /dashboard or /home). Force to Face Check.
            httpResponse.sendRedirect(FACE_CHECK_URI);
            return;
        }

        if (isOnOtpPage) {
            // Correct step. Allow access to the OTP page.
            chain.doFilter(request, response);
            return;
        }

        httpResponse.sendRedirect(OTP_VERIFICATION_URI);
    }
}
