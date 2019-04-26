package com.threelambda.btsearch.bt;

/**
 * Created by ym on 2019-04-26
 */
public class Metadata {

    private final String infoHash;
    private final int metadataSize;
    private final byte[] metadata;
    private final String addr;
    private final int port;

    public Metadata(String infoHash, int metadataSize, byte[] metadata, String addr, int port) {
        this.infoHash = infoHash;
        this.metadataSize = metadataSize;
        this.metadata = metadata;
        this.addr = addr;
        this.port = port;
    }

    public String getInfoHash() {
        return infoHash;
    }

    public int getMetadataSize() {
        return metadataSize;
    }

    public byte[] getMetadata() {
        return metadata;
    }

    public String getAddr() {
        return addr;
    }

    public int getPort() {
        return port;
    }
}
