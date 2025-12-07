package com.tsv.implementation.service;

import org.springframework.stereotype.Service;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

@Service
public class CryptoService {

    private static final String RSA_ALGORITHM = "RSA";
    private static final int RSA_KEY_SIZE = 2048;
    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;

    // Generate RSA Key Pair
    public KeyPair generateRSAKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(RSA_ALGORITHM);
        keyGen.initialize(RSA_KEY_SIZE);
        return keyGen.generateKeyPair();
    }

    // Encrypt private key with password-derived AES key
    public byte[] encryptPrivateKey(PrivateKey privateKey, String password) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = digest.digest(password.getBytes("UTF-8"));
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");

        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

        byte[] privateKeyBytes = privateKey.getEncoded();
        byte[] encryptedKey = cipher.doFinal(privateKeyBytes);

        byte[] combined = new byte[iv.length + encryptedKey.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encryptedKey, 0, combined, iv.length, encryptedKey.length);

        return combined;
    }

    // Generate random AES-256 key
    public SecretKey generateAESKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        return keyGen.generateKey();
    }

    // Wrap AES key with RSA public key
    public byte[] wrapAESKeyWithRSA(SecretKey aesKey, byte[] rsaPublicKeyBytes) throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(rsaPublicKeyBytes);
        PublicKey publicKey = keyFactory.generatePublic(keySpec);

        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.WRAP_MODE, publicKey);
        return cipher.wrap(aesKey);
    }

    // Encrypt file with AES-GCM
    public byte[] encryptFile(byte[] fileData, SecretKey aesKey, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, parameterSpec);
        return cipher.doFinal(fileData);
    }

    public PrivateKey decryptPrivateKey(byte[] encryptedPrivateKey, String password) throws Exception {
        // 1. Derive AES key from password (same way as encryption)
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = digest.digest(password.getBytes("UTF-8"));
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");

        // 2. Extract IV (first 12 bytes) and encrypted data (rest)
        byte[] iv = Arrays.copyOfRange(encryptedPrivateKey, 0, GCM_IV_LENGTH);
        byte[] encryptedData = Arrays.copyOfRange(encryptedPrivateKey, GCM_IV_LENGTH,
                encryptedPrivateKey.length);

        // 3. Decrypt using AES-GCM
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

        byte[] privateKeyBytes = cipher.doFinal(encryptedData);

        // 4. Reconstruct PrivateKey object
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        return keyFactory.generatePrivate(keySpec);
    }

    /**
     * Unwrap (decrypt) an AES key that was wrapped with RSA public key.
     * Used by file owners and share recipients to get the file's AES key.
     *
     * @param wrappedKey The encrypted AES key (from FileMetadata or FileShare)
     * @param privateKey User's RSA private key (after decryption)
     * @return SecretKey for AES file decryption
     */
    public SecretKey unwrapAESKey(byte[] wrappedKey, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.UNWRAP_MODE, privateKey);
        return (SecretKey) cipher.unwrap(wrappedKey, "AES", Cipher.SECRET_KEY);
    }

    /**
     * Decrypt a file's encrypted data using AES-GCM.
     *
     * @param encryptedData The encrypted file bytes
     * @param aesKey The unwrapped AES key
     * @param iv The GCM IV from FileMetadata.gcmIV
     * @return Decrypted file bytes
     */
    public byte[] decryptFile(byte[] encryptedData, SecretKey aesKey, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, aesKey, parameterSpec);
        return cipher.doFinal(encryptedData);
    }
}
