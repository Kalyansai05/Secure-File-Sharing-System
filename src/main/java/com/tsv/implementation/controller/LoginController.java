package com.tsv.implementation.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.tsv.implementation.dao.UserRepository;
import com.tsv.implementation.dto.UserLoginDTO;
import com.tsv.implementation.model.User;
import com.tsv.implementation.service.DefaultUserService;



@Controller
@RequestMapping("/login")
public class LoginController {
    @Autowired
    private DefaultUserService userService;

    @Autowired
    UserRepository userRepo;

    @ModelAttribute("user")
    public UserLoginDTO userLoginDTO() {
        return new UserLoginDTO();
    }

    @GetMapping
    public String login() {
        return "login";
    }

    @PostMapping
    public void loginUser(@ModelAttribute("user")
                          UserLoginDTO userLoginDTO) {
        System.out.println("UserDTO" + userLoginDTO);
        userService.loadUserByUsername(userLoginDTO.getUsername());
    }

    @GetMapping("/otpVerification")
    public String otpSent(Model model, UserLoginDTO userLoginDTO) {
        model.addAttribute("otpValue", userLoginDTO);
        return "otpScreen";

    }

    @PostMapping("/otpVerification")
    public String otpVerification(@ModelAttribute("otpValue") UserLoginDTO userLoginDTO) {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        UserDetails userDetails = (UserDetails) securityContext.getAuthentication().getPrincipal();
        User users = userRepo.findByEmail(userDetails.getUsername());


        if (users == null || users.getOtp() != userLoginDTO.getOtp()) {
            System.err.println("DEBUG: OTP mismatch or user not found. Redirecting to error.");
            return "redirect:/login/otpVerification?error";
        }


        users.setOtpVerified(true);
        userRepo.save(users);


        try {

            UserDetails updatedUserDetails = userService.loadUserByUsername(users.getEmail());


            UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(
                    updatedUserDetails,
                    securityContext.getAuthentication().getCredentials(),
                    updatedUserDetails.getAuthorities()
            );


            SecurityContextHolder.getContext().setAuthentication(newAuth);
            System.out.println("DEBUG: Security Context successfully updated. Redirecting to face-check.");
            return "redirect:/face-check";
        } catch (Exception e) {
            System.err.println("Error updating Security Context: " + e.getMessage());
            e.printStackTrace();

        }


        return "redirect:/face-check";
    }
}
