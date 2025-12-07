package com.tsv.implementation.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDate; // ADD THIS IMPORT
import java.time.format.DateTimeFormatter; // ADD THIS IMPORT
import java.util.Base64;
import com.tsv.implementation.dao.RoleRepository;
import com.tsv.implementation.dao.UserRepository;
import com.tsv.implementation.dto.UserRegisteredDTO;
import com.tsv.implementation.model.Role;
import com.tsv.implementation.model.User;


@Service
public class DefaultUserServiceImpl implements DefaultUserService{
    @Autowired
    private UserRepository userRepo;

    @Autowired
    private RoleRepository roleRepo;

    @Autowired
    JavaMailSender javaMailSender;

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();



    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        User user = userRepo.findByEmail(email);
        if(user == null) {
            throw new UsernameNotFoundException("Invalid username or password.");
        }
        return new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPassword(), mapRolesToAuthorities(user.getRoles()));
    }

    private Collection<? extends GrantedAuthority> mapRolesToAuthorities(Set<Role> roles){
        return roles.stream().map(role -> new SimpleGrantedAuthority(role.getRole())).collect(Collectors.toList());
    }

    @Override
    public User save(UserRegisteredDTO userRegisteredDTO) {
        Role role = roleRepo.findByRole("USER");

        User user = new User();
        user.setEmail(userRegisteredDTO.getEmail_id());
        user.setName(userRegisteredDTO.getName());
        user.setPassword(passwordEncoder.encode(userRegisteredDTO.getPassword()));
        user.setMobileNumber(userRegisteredDTO.getMobileNumber());


        String dobString = userRegisteredDTO.getDateOfBirth();
        if (dobString != null && !dobString.isEmpty()) {
            try {

                LocalDate dob = LocalDate.parse(dobString, DateTimeFormatter.ISO_LOCAL_DATE);
                user.setDateOfBirth(dob);
            } catch (Exception e) {
                System.err.println("Error parsing Date of Birth: " + e.getMessage());
                // Handle parsing error (e.g., log, or set DOB to null)
            }
        }

        String base64Image = userRegisteredDTO.getFaceTemplateBase64();
        if (base64Image != null && !base64Image.isEmpty()) {
            try {
                if (base64Image.startsWith("data:")) {
                    base64Image = base64Image.split(",", 2)[1];
                }
                byte[] decodedBytes = Base64.getDecoder().decode(base64Image);
                user.setFaceImage(decodedBytes);
            } catch (IllegalArgumentException e) {
                System.err.println("Error decoding face image during registration: " + e.getMessage());
            }
        }
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);

        return userRepo.save(user);
    }

    @Override
    public String generateOtp(User user) {
        try {
            int randomPIN = (int) (Math.random() * 9000) + 1000;
            user.setOtp(randomPIN);
            user.setOtpVerified(false);

            userRepo.save(user);
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom("chintalakalyansai119@gmail.com");// input the senders email ID
            msg.setTo(user.getEmail());

            msg.setSubject("Welcome To My Channel");
            msg.setText("Hello \n\n" +"Your Login OTP :" + randomPIN + ".Please Verify. \n\n"+"Regards \n"+"ABC");

            javaMailSender.send(msg);

            return "success";
        }catch (Exception e) {
            System.err.println("FATAL ERROR DURING USER SAVE: " + e.getMessage());
            e.printStackTrace();
            return "error";
        }
    }

}