package com.merchant.ordering.util;

import java.security.MessageDigest;

/**
 * MD5 加密工具类
 */
public class Md5Utils {

    private static final char[] HEX_DIGITS = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    /**
     * 对字符串进行 MD5 加密（32位小写）
     */
    public static String md5(String input) {
        if (input == null) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(input.getBytes("UTF-8"));
            return toHexString(bytes);
        } catch (Exception e) {
            throw new RuntimeException("MD5加密失败", e);
        }
    }

    private static String toHexString(byte[] bytes) {
        char[] chars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            chars[i * 2] = HEX_DIGITS[(bytes[i] >> 4) & 0x0f];
            chars[i * 2 + 1] = HEX_DIGITS[bytes[i] & 0x0f];
        }
        return new String(chars);
    }
}
