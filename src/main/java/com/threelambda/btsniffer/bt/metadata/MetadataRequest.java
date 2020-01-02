package com.threelambda.btsniffer.bt.metadata;

import lombok.Data;

import java.io.Serializable;

/**
 * @author ym
 * @date 2019/12/05
 */
@Data
public class MetadataRequest implements Serializable {
    String ip;
    int port;
    String infoHashHex;
}
