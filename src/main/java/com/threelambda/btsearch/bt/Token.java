package com.threelambda.btsearch.bt;

import lombok.Data;
import org.joda.time.DateTime;

import java.io.Serializable;

/**
 * Created by ym on 2019-09-24
 */
@Data
public class Token implements Serializable {
    String data;
    DateTime createTime;

    public Token() {
        data = Util.randomString(5);
        createTime = DateTime.now();
    }
}
