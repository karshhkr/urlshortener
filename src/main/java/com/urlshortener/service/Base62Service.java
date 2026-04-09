package com.urlshortener.service;

import org.springframework.stereotype.Service;

@Service
public class Base62Service {

    private static final String CHARACTERS =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int BASE = CHARACTERS.length();

    public String encode(long value) {
        if (value == 0) return String.valueOf(CHARACTERS.charAt(0));
        StringBuilder sb = new StringBuilder();
        while (value > 0) {
            sb.insert(0, CHARACTERS.charAt((int)(value % BASE)));
            value /= BASE;
        }
        return sb.toString();
    }

    public long decode(String encoded) {
        long result = 0;
        for (char c : encoded.toCharArray()) {
            result = result * BASE + CHARACTERS.indexOf(c);
        }
        return result;
    }
}