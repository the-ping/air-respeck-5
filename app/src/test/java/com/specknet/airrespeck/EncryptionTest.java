package com.specknet.airrespeck;

import android.util.Base64;

import com.specknet.airrespeck.utils.Utils;

import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.junit.Test;

/**
 * Class to test encryption algorithm
 */

public class EncryptionTest {
    @Test
    public void encrypt() {
        // Example key in same format as key stored on device
        String passphrase = "012";

        String plainText = "Hello";

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] key = md.digest(passphrase.getBytes());

            System.out.println("Key: " + Arrays.toString(key));

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecureRandom random = new SecureRandom();
            byte[] ivBytes = new byte[16];
            random.nextBytes(ivBytes);

            IvParameterSpec iv = new IvParameterSpec(ivBytes);
            SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
            byte[] cipherText = Utils.concatenate(iv.getIV(), cipher.doFinal(plainText.getBytes("UTF8")));
            System.out.println("Ciphertext: " + Arrays.toString(cipherText));
            System.out.println("b64 encoded: " + new String(Base64.encode(cipherText, Base64.NO_WRAP), StandardCharsets.UTF_8));

            // Decryption
            cipher.init(Cipher.DECRYPT_MODE, secretKey,
                    new IvParameterSpec(Arrays.copyOfRange(cipherText, 0, 16)));
            byte[] decryptedText = cipher.doFinal(Arrays.copyOfRange(cipherText, 16, cipherText.length));
            System.out.println("Decrypted text: " + new String(decryptedText, StandardCharsets.UTF_8));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
