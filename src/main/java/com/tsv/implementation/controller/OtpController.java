package com.tsv.implementation.controller;

import com.tsv.implementation.dao.UserRepository;
import com.tsv.implementation.dto.OtpForm;
import com.tsv.implementation.model.User;
import com.tsv.implementation.service.CryptoService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.security.KeyPair;
import java.time.LocalDateTime;

@Controller
public class OtpController {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private CryptoService cryptoService;

    @GetMapping("/otp-verify")
    public String showOtpVerifyPage(HttpServletRequest request, Model model) {
        // Check if this is for registration or login
        String pendingUser = (String) request.getSession().getAttribute("pendingUser");
        String username = (String) request.getSession().getAttribute("username");

        if (pendingUser != null) {
            System.out.println(" OTP page: Registration flow for " + pendingUser);
            model.addAttribute("email", pendingUser);
        } else if (username != null) {
            System.out.println(" OTP page: Login flow for " + username);
            model.addAttribute("email", username);
        } else {
            System.err.println("⚠ No user in session, redirecting to login");
            return "redirect:/login";
        }


        model.addAttribute("otpValue", new OtpForm());

        return "otpScreen";
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(@ModelAttribute("otpValue") OtpForm otpForm,
                            HttpServletRequest request,
                            Model model) {

        String pendingUser = (String) request.getSession().getAttribute("pendingUser");
        String pendingPassword = (String) request.getSession().getAttribute("pendingPassword");

        String username;
        boolean isRegistration = false;

        if (pendingUser != null) {

            username = pendingUser;
            isRegistration = true;
            System.out.println("🔐 OTP Verification: REGISTRATION flow for " + username);
        } else {

            username = (String) request.getSession().getAttribute("username");
            System.out.println(" OTP Verification: LOGIN flow for " + username);
        }

        if (username == null) {
            System.err.println(" No username in session");
            return "redirect:/login?error";
        }

        User user = userRepo.findByEmail(username);


        int otp;
        try {
            otp = Integer.parseInt(otpForm.getOtp());
        } catch (NumberFormatException e) {
            System.err.println(" Invalid OTP format");
            model.addAttribute("error", "Invalid OTP format");
            model.addAttribute("otpValue", new OtpForm());
            model.addAttribute("email", username);
            return "otpScreen";
        }

        if (user != null && user.getOtp() == otp) {
            System.out.println("OTP verified successfully");

            if (isRegistration) {

                try {
                    System.out.println("🔑 Generating RSA keys for new user...");


                    KeyPair keyPair = cryptoService.generateRSAKeyPair();


                    byte[] encryptedPrivateKey = cryptoService.encryptPrivateKey(
                            keyPair.getPrivate(),
                            pendingPassword
                    );


                    user.setRsaPublicKey(keyPair.getPublic().getEncoded());
                    user.setRsaPrivateKeyEncrypted(encryptedPrivateKey);
                    user.setKeyGeneratedAt(LocalDateTime.now());


                    user.setOtpVerified(true);
                    user.setActive(false);

                    userRepo.save(user);


                    request.getSession().removeAttribute("pendingUser");
                    request.getSession().removeAttribute("pendingPassword");

                    System.out.println(" RSA keys generated and stored!");
                    System.out.println("   Public Key: " + (user.getRsaPublicKey() != null ? "Generated" : "NULL"));
                    System.out.println("   Private Key: " + (user.getRsaPrivateKeyEncrypted() != null ? "Generated" : "NULL"));
                    System.out.println(" Redirecting to login...");

                    return "redirect:/login?registered=success";

                } catch (Exception e) {
                    System.err.println(" Key generation error: " + e.getMessage());
                    e.printStackTrace();
                    model.addAttribute("error", "Key generation failed. Please try again.");
                    model.addAttribute("otpValue", new OtpForm());
                    model.addAttribute("email", username);
                    return "otpScreen";
                }

            }  else {
            // LOGIN FLOW: Mark OTP verified and redirect to face check
            user.setOtpVerified(true);
            userRepo.save(user);


            request.getSession().removeAttribute("username");
            request.getSession().removeAttribute("loginOtpFlow");

            System.out.println(" OTP verified for login");
            System.out.println(" Redirecting to face verification...");

            return "redirect:/face-check";
        }

    }

        System.err.println(" Invalid OTP entered");
        model.addAttribute("error", "Invalid OTP. Please try again.");
        model.addAttribute("otpValue", new OtpForm());
        model.addAttribute("email", username);
        return "otpScreen";
    }
}
