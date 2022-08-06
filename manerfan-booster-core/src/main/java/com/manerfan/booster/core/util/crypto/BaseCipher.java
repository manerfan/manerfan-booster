package com.manerfan.booster.core.util.crypto;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * BaseCipher
 *
 * <pre>
 *      加解密
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/7
 */
public interface BaseCipher {
    Base64.Encoder BASE64_ENCODER = Base64.getEncoder();
    Base64.Decoder BASE64_DECODER = Base64.getDecoder();

    /**
     * 加密
     *
     * @param data 待加密数据
     * @return 加密后数据
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     */
    byte[] encrypt(byte[] data)
        throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException,
        IllegalBlockSizeException;

    /**
     * 加密为base64
     *
     * @param data 待加密数据
     * @return 加密后base64
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     */
    default String encryptToBase64(byte[] data)
        throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException,
        IllegalBlockSizeException {
        return BASE64_ENCODER.encodeToString(encrypt(data));
    }

    /**
     * 加密字符串为base64
     *
     * @param data 待加密字符串
     * @return 加密后base64
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     */
    default String encryptToBase64(String data)
        throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException,
        IllegalBlockSizeException {
        return encryptToBase64(data.getBytes());
    }

    /**
     * 解密
     *
     * @param data 待解密数据
     * @return 解密后数据
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     */
    byte[] decrypt(byte[] data)
        throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException,
        IllegalBlockSizeException;

    /**
     * 解密
     *
     * @param base64Data 带解密base64
     * @return 解密后数据
     * @throws IllegalBlockSizeException
     * @throws InvalidKeyException
     * @throws BadPaddingException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     */
    default byte[] decryptFromBase64(String base64Data)
        throws IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException,
        NoSuchPaddingException {
        return decrypt(BASE64_DECODER.decode(base64Data));
    }

    /**
     * 解密为字符串
     *
     * @param data 待解密字节流
     * @return 解密后字符串
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     */
    default String decryptToString(byte[] data)
        throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException,
        IllegalBlockSizeException {
        return new String(decrypt(data));
    }


    /**
     * 解密为字符串
     *
     * @param data 待解密字节流
     * @return 解密后字符串
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     */
    default String decryptFromBase64ToString(String data)
        throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException,
        IllegalBlockSizeException {
        return new String(decryptFromBase64(data));
    }
}
