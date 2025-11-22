package com.skycoin.wallet.encryption;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;

public class EncryptionManager {

    private static final String TAG = EncryptionManager.class.getName();

    private static final String AndroidKeyStore = "AndroidKeyStore";
    private static final String AES_MODE = "AES/GCM/NoPadding";
    private static final String KEY_ALIAS = "skycoin.key.aes.gcm";
    private static final String TOKEN_SEPARATOR = ",";
    private static final int GCM_TAG_LENGTH_BITS = 128;

    /**
     * Call this at least once to create a key which is then saved for
     * all future uses (calling it multiple times till not overwrite
     * the first key)
     */
    public static void generateKey() throws CryptoException {

                try {
                    KeyStore keyStore = KeyStore.getInstance(AndroidKeyStore);
                    keyStore.load(null);
                    Log.d(TAG, "getting key from " + AndroidKeyStore);
                    if (!keyStore.containsAlias(KEY_ALIAS)) {
                        Log.d(TAG, "creating new encryption key");
                        KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, AndroidKeyStore);
                        keyGenerator.init(
                                new KeyGenParameterSpec.Builder(KEY_ALIAS,
                                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                                        .setRandomizedEncryptionRequired(true) // Cipher will generate IV, remember to store it when encrypting!
                                        .build());
                        keyGenerator.generateKey();
                    } else {
                        Log.d(TAG, "encryption key already exists");
                    }
                } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException | NoSuchProviderException | InvalidAlgorithmParameterException ex) {
                    CryptoException cx = new CryptoException("Could not generate key",ex);
                    throw cx;
                }
    }

    private static Key getSecretKey() throws CryptoException {
        try {
            KeyStore keyStore = KeyStore.getInstance(AndroidKeyStore);
            keyStore.load(null);
            return keyStore.getKey(KEY_ALIAS, null);
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException | UnrecoverableKeyException ex) {
            throw new CryptoException("Could not get secret key", ex);
        }
    }

    /**
     * Encrypts the given data with our AndroidKeyStore key
     *
     * @param plaintextBytes byte array to be encrypted
     * @return base64-encoded result string with the base64-encoded IV concatenated on the end
     * after a comma symbol. Split the string on "," to
     * get first part as encrypted data, second part as IV
     */
    public static String encrypt(byte[] plaintextBytes) throws CryptoException {

        try {
            Cipher c = Cipher.getInstance(AES_MODE);
            c.init(Cipher.ENCRYPT_MODE, getSecretKey());
            byte[] encryptedBytes = c.doFinal(plaintextBytes);

            String encryptedBase64Encoded = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP);
            String ivStrBase64 = Base64.encodeToString(c.getIV(), Base64.NO_WRAP);
            String finalResultStr = encryptedBase64Encoded + TOKEN_SEPARATOR + ivStrBase64;
            Log.d(TAG, "encrypt result: " + finalResultStr);
            return finalResultStr;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
            throw new CryptoException("Could not encrypt",ex);
        }
    }

    /**
     * Decrypts the given base64-encoded string into a byte-array.
     * The string must have the used IV separated
     * by a Comma at the end
     *
     * @param encryptedBase64 String on the format "<base64encryptedstring>,<base64encodedIV>"
     * @return the decrypted plaintext byte array
     */
    public static byte[] decrypt(String encryptedBase64) throws CryptoException {

        String[] parts = encryptedBase64.split(",");
        if (parts == null || parts.length != 2) {
            Log.e(TAG, "could not split param into 'encrypted' and 'iv'");
            throw new CryptoException("Input string seems badly formatted, should be split on a Comma:" + encryptedBase64);
        }

        byte[] encrypted = Base64.decode(parts[0], Base64.NO_WRAP);
        byte[] iv = Base64.decode(parts[1], Base64.NO_WRAP);

        try {
            Cipher c = Cipher.getInstance(AES_MODE);
            c.init(Cipher.DECRYPT_MODE, getSecretKey(), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] decryptedBytes = c.doFinal(encrypted);
            return decryptedBytes;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex) {
            throw new CryptoException("Could not decrypt", ex);
        }
    }

}
