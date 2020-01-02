package com.threelambda.btsniffer.bt.tran;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.Map;

/**
 * @author ym
 * @date 2019/10/14
 */
@Data
@AllArgsConstructor
public class Query implements Serializable {
    InetSocketAddress addr;
    Map<String, Object> dataMap;

}
