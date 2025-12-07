package com.tsv.implementation.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.*;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Entity
@Table(name = "user")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    private String name;

    @Column(unique = true)
    private String email;

    private String password;
    private String mobileNumber;

    private LocalDate dateOfBirth;

    // --- FACE IMAGE TEMPLATE FOR BIOMETRIC VERIFICATION ---
    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] faceImage;

    @Lob
    @Column(name = "rsa_public_key", columnDefinition = "LONGBLOB")
    private byte[] rsaPublicKey;

    @Lob
    @Column(name = "rsa_private_key_encrypted", columnDefinition = "LONGBLOB")
    private byte[] rsaPrivateKeyEncrypted;


    private LocalDateTime keyGeneratedAt;
    // --- END RSA ENCRYPTION KEYS ---

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "users_role",
            joinColumns = @JoinColumn(name = "cust_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"))
    Set<Role> roles = new HashSet<Role>();

    private int otp;

    // Three-factor authentication flags
    private boolean isActive = false;      // Final verification status (Password + OTP + Face)
    private boolean otpVerified = false;   // OTP verification status

    // --- CONSTRUCTORS ---
    public User() {
    }

    // --- GETTERS AND SETTERS ---

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public void addRole(Role role) {
        this.roles.add(role);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public int getOtp() {
        return otp;
    }

    public void setOtp(int otp) {
        this.otp = otp;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    public boolean isOtpVerified() {
        return otpVerified;
    }

    public void setOtpVerified(boolean otpVerified) {
        this.otpVerified = otpVerified;
    }

    // --- FACE IMAGE GETTER/SETTER ---
    public byte[] getFaceImage() {
        return faceImage;
    }

    public void setFaceImage(byte[] faceImage) {
        this.faceImage = faceImage;
    }

    // --- RSA KEY GETTERS/SETTERS ---
    public byte[] getRsaPublicKey() {
        return rsaPublicKey;
    }

    public void setRsaPublicKey(byte[] rsaPublicKey) {
        this.rsaPublicKey = rsaPublicKey;
    }

    public byte[] getRsaPrivateKeyEncrypted() {
        return rsaPrivateKeyEncrypted;
    }

    public void setRsaPrivateKeyEncrypted(byte[] rsaPrivateKeyEncrypted) {
        this.rsaPrivateKeyEncrypted = rsaPrivateKeyEncrypted;
    }

    public LocalDateTime getKeyGeneratedAt() {
        return keyGeneratedAt;
    }

    public void setKeyGeneratedAt(LocalDateTime keyGeneratedAt) {
        this.keyGeneratedAt = keyGeneratedAt;
    }
    // --- END RSA KEY GETTERS/SETTERS ---

    // --- UserDetails Implementation Methods ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return new HashSet<>();
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
