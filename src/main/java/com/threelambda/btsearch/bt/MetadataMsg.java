package com.threelambda.btsearch.bt;

/**
 * Created by ym on 2019-04-25
 */
public class MetadataMsg extends HandshakeMsg {

    private final int metadataSize;
    private final int utMetadata;

    MetadataMsg(int metadataSize, int utMetadata) {
        this.metadataSize = metadataSize;
        this.utMetadata = utMetadata;
    }

    public int getMetadataSize() {
        return metadataSize;
    }

    public int getUtMetadata() {
        return utMetadata;
    }
}
