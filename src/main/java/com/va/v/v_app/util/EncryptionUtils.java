package com.va.v.v_app.util;

import lombok.extern.slf4j.Slf4j;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Utility class for AES encryption and decryption.
 * Uses a fixed key for development as requested.
 */
@Slf4j
public class EncryptionUtils {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String SECRET_KEY = "v-app-dev-secret"; // 16 bytes
    private static final String IV = "v-app-dev-iv-12"; // 16 bytes (padded below)

    private static final SecretKeySpec keySpec;
    private static final IvParameterSpec ivSpec;

    static {
        // Ensure key is 16 bytes
        byte[] keyBytes = new byte[16];
        byte[] originalKeyBytes = "v-app-dev".getBytes(StandardCharsets.UTF_8);
        System.arraycopy(originalKeyBytes, 0, keyBytes, 0, Math.min(originalKeyBytes.length, 16));
        keySpec = new SecretKeySpec(keyBytes, "AES");

        // Ensure IV is 16 bytes
        byte[] ivBytes = new byte[16];
        byte[] originalIvBytes = "v-app-iv-salt-12".getBytes(StandardCharsets.UTF_8);
        System.arraycopy(originalIvBytes, 0, ivBytes, 0, Math.min(originalIvBytes.length, 16));
        ivSpec = new IvParameterSpec(ivBytes);
    }

    /**
     * Encrypts the given plain text.
     */
    public static String encrypt(String plainText) {
        if (plainText == null) return null;
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            log.error("Encryption error: {}", e.getMessage());
            throw new RuntimeException("Error during encryption", e);
        }
    }

    /**
     * Decrypts the given encrypted text.
     */
    public static String decrypt(String encryptedText) {
        if (encryptedText == null) return null;
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Decryption error: {}", e.getMessage());
            throw new RuntimeException("Error during decryption", e);
        }
    }
}
