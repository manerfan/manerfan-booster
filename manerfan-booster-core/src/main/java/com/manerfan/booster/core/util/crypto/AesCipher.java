package com.manerfan.booster.core.util.crypto;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.StringUtils;

/**
 * AesCipher
 *
 * <pre>
 *      AES/ECB/PKCS5Padding 加解密
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/7
 */
public class AesCipher implements BaseCipher {
    private static final String KEY_ALGORITHM = "AES";
    private static final String AES_CIPHER = "AES/ECB/PKCS5Padding";

    /**
     * 秘钥长度
     */
    private final int passwordSize;

    /**
     * 秘钥
     */
    private final SecretKeySpec keySpec;

    private AesCipher(int passwordSize, String password) {
        this.passwordSize = passwordSize;
        this.keySpec = new SecretKeySpec(
            StringUtils.leftPad(password, passwordSize, '0').getBytes(StandardCharsets.UTF_8),
            KEY_ALGORITHM
        );
    }

    /**
     * 构建AesCipher
     *
     * @param passwordSize 密码长度
     * @param password     密码
     * @return {@link AesCipher}
     */
    public static AesCipher build(int passwordSize, String password) {
        return new AesCipher(passwordSize, password);
    }

    /**
     * 构建AesCipher
     *
     * @param password 密码
     * @return {@link AesCipher}
     */
    public static AesCipher build(String password) {
        return build(16, password);
    }

    @Override
    public byte[] encrypt(byte[] data)
        throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException,
        IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance(AES_CIPHER);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        return cipher.doFinal(data);
    }

    @Override
    public byte[] decrypt(byte[] data)
        throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException,
        IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance(AES_CIPHER);
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        return cipher.doFinal(data);
    }
}
