package com.threelambda.btsearch.bt.token;

import lombok.Data;
import org.joda.time.DateTime;

import java.io.Serializable;

/**
 * @author ym
 * @date 2019/10/21
 */
@Data
public class Token implements Serializable {
    String token;
    DateTime createTime;

    public Token(String token) {
        this.token = token;
        createTime = DateTime.now();
    }
}
