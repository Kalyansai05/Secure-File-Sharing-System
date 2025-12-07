package com.tsv.implementation.controller;

import com.tsv.implementation.dto.UserRegisteredDTO;
import com.tsv.implementation.model.User;
import com.tsv.implementation.service.DefaultUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/registration")
public class RegistrationController {

    @Autowired
    private DefaultUserService userService;

    @ModelAttribute("user")
    public UserRegisteredDTO userRegistrationDto() {
        return new UserRegisteredDTO();
    }

    @GetMapping
    public String showRegistrationForm() {
        return "register";
    }

    @PostMapping
    public String registerUserAccount(@ModelAttribute("user") UserRegisteredDTO registrationDto,
                                      HttpServletRequest request) {
        try {
            System.out.println("=== REGISTRATION STARTED ===");
            System.out.println(" Email: " + registrationDto.getEmail_id());


            User user = userService.save(registrationDto);
            System.out.println(" User saved to database");


            String otpResult = userService.generateOtp(user);
            System.out.println("OTP generation result: " + otpResult);

            if ("success".equals(otpResult)) {

                request.getSession().setAttribute("pendingUser", user.getEmail());
                request.getSession().setAttribute("pendingPassword", registrationDto.getPassword());

                System.out.println(" Session created with pending user data");
                System.out.println(" Redirecting to OTP verification...");


                return "redirect:/otp-verify";
            } else {
                System.err.println(" OTP generation failed");
                return "redirect:/registration?error=otp";
            }

        } catch (Exception e) {
            System.err.println(" Registration error: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/registration?error";
        }
    }
}
