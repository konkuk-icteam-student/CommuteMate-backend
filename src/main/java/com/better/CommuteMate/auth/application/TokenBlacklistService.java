package com.better.CommuteMate.auth.application;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {

    private final Map<String, Boolean> blacklist = new ConcurrentHashMap<>();

    public void blacklist(String token) {
        blacklist.put(token, Boolean.TRUE);
    }

    public boolean isBlacklisted(String token) {
        return blacklist.containsKey(token);
    }
}
