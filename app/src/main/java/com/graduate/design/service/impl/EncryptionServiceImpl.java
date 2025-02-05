package com.graduate.design.service.impl;

import android.os.Build;

import com.graduate.design.entity.BiIndex;
import com.graduate.design.proto.FileUpload;
import com.graduate.design.proto.SearchFile;
import com.graduate.design.proto.SendSearchToken;
import com.graduate.design.service.EncryptionService;
import com.graduate.design.utils.ByteUtils;
import com.graduate.design.utils.FileUtils;
import com.graduate.design.utils.GraduateDesignApplication;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionServiceImpl implements EncryptionService {
    @Override
    public byte[] getSecretKey(String username, String password) {
        MessageDigest messageDigest;
        byte[] mainSecret;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update((username+password).getBytes(StandardCharsets.UTF_8));
            mainSecret = messageDigest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return mainSecret;
    }

    @Override
    public byte[] encryptByAES128(String plaintext, byte[] secretKey) {
        byte[] plainBytes = plaintext.getBytes(StandardCharsets.UTF_8);
        return encryptByAES128(plainBytes, secretKey);
    }

    @Override
    public byte[] encryptByAES128(byte[] plaintext, byte[] secretKey) {
        byte[] encryptRes;
        try {
            SecretKeySpec spec = new SecretKeySpec(secretKey, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");// "算法/加密/填充"
            IvParameterSpec iv = new IvParameterSpec(GraduateDesignApplication.IV.getBytes(StandardCharsets.UTF_8));
            cipher.init(Cipher.ENCRYPT_MODE, spec, iv);
            encryptRes = cipher.doFinal(plaintext);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException(e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        }
        return encryptRes;
    }

    @Override
    public byte[] decryptByAES128(String ciphertext, byte[] secretKey) {
        byte[] cipherBytes = FileUtils.Base64ToBytes(ciphertext);
        return decryptByAES128(cipherBytes, secretKey);
    }

    @Override
    public byte[] decryptByAES128(byte[] ciphertext, byte[] secretKey) {
        byte[] decryptRes;
        try {
            SecretKeySpec spec = new SecretKeySpec(secretKey, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); // "算法/加密/填充"
            IvParameterSpec iv = new IvParameterSpec(GraduateDesignApplication.IV.getBytes(StandardCharsets.UTF_8));
            cipher.init(Cipher.DECRYPT_MODE, spec, iv);
            decryptRes = cipher.doFinal(ciphertext);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException(e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        }
        return decryptRes;
    }

    public byte[] HmacSha256(byte[] key, byte[] data){
        byte[] res;
        try {
            SecretKeySpec secret = new SecretKeySpec(key, "HmacSha256");
            Mac mac = Mac.getInstance("HmacSha256");
            mac.init(secret);
            res = mac.doFinal(data);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        }
        return res;
    }

    // 上传加密搜索索引
    @Override
    public FileUpload.indexToken uploadIndex(Long id, String word) {
        BiIndex biIndex = GraduateDesignApplication.getBiIndex();
        Map<Long, String> lastW = biIndex.getLastW();
        Map<String, Long> lastID = biIndex.getLastID();
        byte[] key1 = GraduateDesignApplication.getKey1();
        byte[] key2 = GraduateDesignApplication.getKey2();

        String oldWord = lastW.get(id);
        Long oldID = lastID.get(word);

        byte[] idBytes = String.valueOf(id).getBytes(StandardCharsets.UTF_8);
        byte[] wordBytes = word.getBytes(StandardCharsets.UTF_8);

        byte[] word_id = ByteUtils.mergeBytes(wordBytes, idBytes);
        byte[] id_word = ByteUtils.mergeBytes(idBytes, wordBytes);

        byte[] L = cutOffTo128(HmacSha256(key1, word_id));
        byte[] Rw = ByteUtils.getRandomBytes(16);
        byte[] Rid = ByteUtils.getRandomBytes(16);

        byte[] kw = cutOffTo128(HmacSha256(key2, wordBytes));
        byte[] kid = cutOffTo128(HmacSha256(key2, idBytes));

        byte[] Cw = encryptByAES128(idBytes, kw);
        byte[] Cid = encryptByAES128(word, kid);
        byte[] Iw;

        if(oldID == null) {
            byte[] Jw = cutOffTo128(HmacSha256(key2, word_id));
            Iw = HmacSha256(Jw, Rw);
        }
        else {
            byte[] oldIDBytes = String.valueOf(oldID).getBytes(StandardCharsets.UTF_8);
            byte[] oldL = cutOffTo128(HmacSha256(key1, ByteUtils.mergeBytes(wordBytes, oldIDBytes)));
            byte[] oldJw = cutOffTo128(HmacSha256(key2, ByteUtils.mergeBytes(wordBytes, oldIDBytes)));
            byte[] Jw = cutOffTo128(HmacSha256(key2, word_id));
            Iw = ByteUtils.xor(HmacSha256(Jw, Rw), ByteUtils.mergeBytes(oldL, oldJw));
        }
        byte[] Iid;
        if(oldWord==null) {
            byte[] Jid = cutOffTo128(HmacSha256(key2, id_word));
            Iid = HmacSha256(Jid, Rid);
        }
        else {
            byte[] oldWordBytes = oldWord.getBytes(StandardCharsets.UTF_8);
            byte[] oldL = cutOffTo128(HmacSha256(key1, ByteUtils.mergeBytes(oldWordBytes, idBytes)));
            byte[] oldJid = cutOffTo128(HmacSha256(key2, ByteUtils.mergeBytes(idBytes, oldWordBytes)));
            byte[] Jid = cutOffTo128(HmacSha256(key2, id_word));
            Iid = ByteUtils.xor(HmacSha256(Jid, Rid), ByteUtils.mergeBytes(oldL, oldJid));
        }
        lastW.put(id, word);
        lastID.put(word, id);

        biIndex.setLastID(lastID);
        biIndex.setLastW(lastW);

        return FileUpload.indexToken.newBuilder().setL(FileUtils.bytes2Base64(L))
                .setIw(FileUtils.bytes2Base64(Iw))
                .setRw(FileUtils.bytes2Base64(Rw))
                .setCw(FileUtils.bytes2Base64(Cw))
                .setIid(FileUtils.bytes2Base64(Iid))
                .setRid(FileUtils.bytes2Base64(Rid))
                .setCid(FileUtils.bytes2Base64(Cid))
                .build();
    }

    @Override
    public SendSearchToken.SearchToken getSearchToken(String word) {
        BiIndex biIndex = GraduateDesignApplication.getBiIndex();
        Map<String, Long> lastID = biIndex.getLastID();
        byte[] key1 = GraduateDesignApplication.getKey1();
        byte[] key2 = GraduateDesignApplication.getKey2();

        Long id = lastID.get(word);
        if(id==null) return null;

        byte[] idBytes = String.valueOf(id).getBytes(StandardCharsets.UTF_8);
        byte[] wordBytes = word.getBytes(StandardCharsets.UTF_8);
        byte[] word_id = ByteUtils.mergeBytes(wordBytes, idBytes);
        byte[] L = cutOffTo128(HmacSha256(key1, word_id));
        byte[] Jw = cutOffTo128(HmacSha256(key2, word_id));

        return SendSearchToken.SearchToken.newBuilder()
                .setL(FileUtils.bytes2Base64(L))
                .setJw(FileUtils.bytes2Base64(Jw))
                .build();
    }

    @Override
    public List<Long> getNodeIdByCw(List<String> Cw, String word) {
        byte[] key2 = GraduateDesignApplication.getKey2();

        byte[] kw = cutOffTo128(HmacSha256(key2, word.getBytes(StandardCharsets.UTF_8)));
        List<Long> res = new ArrayList<>();
        for(int i=0;i<Cw.size();i++){
            String cw = Cw.get(i);
            byte[] cwBytes = FileUtils.Base64ToBytes(cw);
            byte[] idBytes = decryptByAES128(cwBytes, kw);
            Long id = Long.parseLong(new String(idBytes));
            res.add(id);
        }
        return res;
    }

    public byte[] cutOffTo128(byte[] data){
        byte[] res = new byte[16];
        for(int i=0;i<16;i++){
            res[i] = data[i];
        }
        return res;
    }
}
