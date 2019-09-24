package com.threelambda.btsearch.bt;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ym on 2019-09-24
 */
public class TokenManager {

    private ConcurrentHashMap<String, Token> map = new ConcurrentHashMap<>();
    private final Duration expireAfter;

    TokenManager(Duration expireAfter) {
        this.expireAfter = expireAfter;
    }

    public Token getToken(String ip) {
        Token token = map.getOrDefault(ip, new Token());
        if (DateTime.now().isAfter(token.getCreateTime().plus(expireAfter))) {
            token = new Token();
            map.put(ip, token);
        }
        return token;
    }

    /**
     * 判断tokenString是否有效
     * @param ip
     * @param tokenString
     * @return
     */
    public boolean check(String ip, String tokenString) {
        Token token = map.getOrDefault(ip, null);
        if (token != null) {
            map.remove(ip);
        }
        return token != null && token.getData().equals(tokenString);
    }

    public void clear() {
        //todo 每三分钟清楚过期的token
    }
}
