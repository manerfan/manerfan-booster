package com.manerfan.booster.core.util.crypto;

import java.io.IOException;
import java.io.InputStreamReader;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Objects;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

/**
 * RsaCipher
 *
 * <pre>
 *      RSA/ECB/PKCS1Padding 加解密
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/7
 */
public class RsaCipher implements BaseCipher {
    private static final String KEY_ALGORITHM = "RSA";
    private static final String RSA_CIPHER = "RSA/ECB/PKCS1Padding";

    /**
     * 私钥
     */
    private final RSAPrivateKey privateKey;

    /**
     * 公钥
     */
    private final RSAPublicKey publicKey;

    private RsaCipher(RSAPrivateKey privateKey, RSAPublicKey publicKey) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    /**
     * 通过公私钥构建
     *
     * @param privateKey 私钥
     * @param publicKey  公钥
     * @return {@link RsaCipher}
     */
    public static RsaCipher build(RSAPrivateKey privateKey, RSAPublicKey publicKey) {
        return new RsaCipher(privateKey, publicKey);
    }

    /**
     * 通过公私钥base64构建
     *
     * <pre>
     *        KeyPairGenerator keyGenJ = KeyPairGenerator.getInstance("RSA");
     *
     *        keyGen.initialize(1024);
     *        KeyPair pair = keyGen.generateKeyPair();
     *
     *        privateKeyBytes = pair.getPrivate().getEncoded();
     *        privateKeyBase64 = BASE64_ENCODER.encodeToString(privateKeyBytes);
     *
     *        publicKeyBytes = pair.getPublic().getEncoded();
     *        publicKeyBase64 = BASE64_ENCODER.encodeToString(publicKeyBytes);
     * </pre>
     *
     * @param privateKeyBase64 私钥base64
     * @param publicKeyBase64  公钥base64
     * @return {@link RsaCipher}
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    public static RsaCipher build(String privateKeyBase64, String publicKeyBase64)
        throws NoSuchAlgorithmException, InvalidKeySpecException {
        val keyTuple = generateRsaKey(privateKeyBase64, publicKeyBase64);
        return RsaCipher.build(keyTuple._1, keyTuple._2);
    }

    /**
     * 通过公私钥文件构建（文件中存储公私钥base64）
     *
     * @param privateKeyRes 私钥文件
     * @param publicKeyRes  公钥文件
     * @return {@link RsaCipher}
     * @throws IOException
     * @throws InvalidKeySpecException
     * @throws NoSuchAlgorithmException
     * @see #build(String, String)
     */
    public static RsaCipher build(Resource privateKeyRes, Resource publicKeyRes)
        throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        String privateKeyBase64 = null;
        String publicKeyBase64 = null;

        if (Objects.nonNull(privateKeyRes) && privateKeyRes.exists()) {
            privateKeyBase64 = FileCopyUtils.copyToString(new InputStreamReader(privateKeyRes.getInputStream()));
        }

        if (Objects.nonNull(publicKeyRes) && publicKeyRes.exists()) {
            publicKeyBase64 = FileCopyUtils.copyToString(new InputStreamReader(publicKeyRes.getInputStream()));
        }

        val keyTuple = generateRsaKey(privateKeyBase64, publicKeyBase64);
        return RsaCipher.build(keyTuple._1, keyTuple._2);
    }

    @Override
    public byte[] encrypt(byte[] data)
        throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException,
        IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance(RSA_CIPHER);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(data);
    }

    @Override
    public byte[] decrypt(byte[] data)
        throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException,
        IllegalBlockSizeException {
        if (Objects.isNull(data) || data.length < 1) {
            return null;
        }
        Cipher cipher = Cipher.getInstance(RSA_CIPHER);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(data);
    }

    private static Tuple2<RSAPrivateKey, RSAPublicKey> generateRsaKey(String privateKeyBase64, String publicKeyBase64)
        throws NoSuchAlgorithmException, InvalidKeySpecException {
        RSAPrivateKey privateKey = null;
        RSAPublicKey publicKey = null;

        if (StringUtils.isNotBlank(privateKeyBase64)) {
            val privateBuffer = BASE64_DECODER.decode(privateKeyBase64);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateBuffer);
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            privateKey = (RSAPrivateKey)keyFactory.generatePrivate(keySpec);
        }

        if (StringUtils.isNotBlank(publicKeyBase64)) {
            val publicBuffer = BASE64_DECODER.decode(publicKeyBase64);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBuffer);
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            publicKey = (RSAPublicKey)keyFactory.generatePublic(keySpec);
        }

        return Tuple.of(privateKey, publicKey);
    }
}
