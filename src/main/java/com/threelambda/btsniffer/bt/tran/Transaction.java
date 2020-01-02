package com.threelambda.btsniffer.bt.tran;

import lombok.Data;

import java.io.Serializable;

/**
 *
 * @author ym
 * @date 2019/10/14
 */
@Data
public class Transaction implements Serializable {
    Query query;
    String tranId;
    Response response;
    /**
     * 重试次数
     */
    Integer retryTimes;

    public Transaction(){}

    public Transaction(Query query, String tranId) {
        this.query = query;
        this.tranId = tranId;
        this.response = new Response();
        this.retryTimes = 3;
    }

}
