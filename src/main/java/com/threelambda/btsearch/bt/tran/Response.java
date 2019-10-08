package com.threelambda.btsearch.bt.tran;

import lombok.Data;

import java.io.Serializable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * @author ym
 * @date 2019/10/14
 */
@Data
public class Response implements Serializable {
    BlockingQueue<Object> queue = new LinkedBlockingDeque<>(1);
}
