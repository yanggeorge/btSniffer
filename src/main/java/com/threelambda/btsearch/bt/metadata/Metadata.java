package com.threelambda.btsearch.bt.metadata;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * Created by ym on 2019-04-26
 */
@Data
@AllArgsConstructor
public class Metadata implements Serializable {

    private String infoHash;
    private int metadataSize;
    private byte[] metadata;
    private String addr;
    private int port;

    public Metadata() {
    }
}
