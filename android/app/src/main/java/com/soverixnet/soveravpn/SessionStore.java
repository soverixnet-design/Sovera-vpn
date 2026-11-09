package com.soverixnet.soveravpn;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Keeps bearer tokens encrypted at rest with an Android Keystore-backed key. */
final class SessionStore {
    private static final String ALIAS = "sovera_session_aes_v1";
    private static final String PREFS = "sovera_secure_session";
    private static final String TOKEN = "token_v1";
    private final SharedPreferences preferences;

    SessionStore(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (store.containsAlias(ALIAS)) return (SecretKey) store.getKey(ALIAS, null);
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS,
            KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256).build());
        return generator.generateKey();
    }

    void save(String token) throws Exception {
        if (token == null || !token.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("Invalid session token");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key());
        String iv = Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP);
        String encrypted = Base64.encodeToString(cipher.doFinal(token.getBytes(StandardCharsets.UTF_8)), Base64.NO_WRAP);
        if (!preferences.edit().putString(TOKEN, iv + ":" + encrypted).commit()) {
            throw new IllegalStateException("Could not store session");
        }
    }

    String load() {
        String value = preferences.getString(TOKEN, null);
        if (value == null) return null;
        try {
            String[] parts = value.split(":", -1);
            if (parts.length != 2) throw new IllegalArgumentException("Invalid session storage");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(),
                new GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)));
            String token = new String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), StandardCharsets.UTF_8);
            if (!token.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("Invalid saved token");
            return token;
        } catch (Exception failedToDecrypt) {
            clear();
            return null;
        }
    }

    void clear() {
        preferences.edit().remove(TOKEN).commit();
    }
}
