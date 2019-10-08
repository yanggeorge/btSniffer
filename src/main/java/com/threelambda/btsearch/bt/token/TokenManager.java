package com.threelambda.btsearch.bt.token;

import com.threelambda.btsearch.bt.Util;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ym
 * @date 2019/10/21
 */
public class TokenManager {

    /**
     *  ip string -> token
     */
    private ConcurrentHashMap<String,Token> map = new ConcurrentHashMap<>();
    private Duration timeExpireAfter = Duration.standardMinutes(10);


    public Token token(String addr) {
        Token token = map.getOrDefault(addr, null);
        if (token == null || !token.createTime.plus(timeExpireAfter).isAfter(DateTime.now())) {
            token = new Token(Util.randomString(5));
            map.put(addr, token);
        }
        return token;
    }

    public boolean check(String addr, String tokenString) {
        Token token = map.getOrDefault(addr, null);
        if (token == null) {
            map.remove(addr);
            return false;
        }
        return token.getToken().equals(tokenString);
    }

    public void clear(){
        //todo 每三分钟清除
    }

}
