package com.ranqiyun.service.web.util;

import java.util.Random;

/**
 * Created by daijingjing on 2018/4/19.
 */
public class NumbeCodec {

    private static final char[] default_table = {'o', 'q', 'j', '1', 'h', 'w', 'g', '8', 't', 'i', 'l', 'p', 'n', 'k', 'c', 'd',
        'f', 'b', 'x', '6', 'r', '9', 'v', '3', '4', 'u', 'a', '2', 'e', '7', 'y', 'm', '0', 's', 'z', '5'};

    private static final char[] readable_table = {'q', 'j', 'h', 'w', 'g', '8', 't', 'p', 'n', 'k', 'c', 'd', 'f', 'b', 'x', '6',
        'r', '9', 'v', '3', '4', 'u', 'a', '2', 'e', '7', 'y', 'm', 's', 'z', '5'};

    public static String encode(long num) {
        return encode(num, readable_table);
    }

    public static String encode(long num, final char[] table) {
        StringBuilder result = new StringBuilder();
        long base = table.length;
        do {
            int mod = (int) (num % base);
            num /= base;

            result.insert(0, table[mod]);
        } while (num > 0);
        return result.toString();
    }

    public static long decode(String value) {
        return decode(value, readable_table);
    }

    private static int indexOf(final char[] arrays, char c) {
        for (int i = 0; i < arrays.length; i++) {
            if (arrays[i] == c) return i;
        }

        return -1;
    }

    public static long decode(String value, final char[] table) {
        char[] array = value.toLowerCase().toCharArray();

        long result = 0;
        long base = table.length;
        for (int i = array.length - 1, j = 0; i >= 0; i--, j++) {
            result += indexOf(table, array[i]) * (long) (Math.pow(base, j));
        }

        return result;
    }

    public static void main(String[] args) {
        Random rad = new Random();
        for (int i = 0; i < 1000000; i++) {
            long num = Math.abs(rad.nextInt(3000000));
            String encodeStr = encode(i);
            long test = decode(encodeStr);
            if (test != i) {
                try {
                    throw new Exception("算法错误");
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }

            System.out.println(String.format("Numbe: %d, encode: %s, decode: %d", i, encodeStr, test));
        }
    }
}
