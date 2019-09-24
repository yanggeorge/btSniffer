package com.threelambda.btsearch.bt;

/**
 * Created by ym on 2019-09-25
 */

public enum KRpcType {
    PING("ping"),
    FIND_NODE("find_node"),
    GET_PEERS("get_peers"),
    ANNOUNCE_PEER("announce_peer");

    private String code;

    KRpcType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    @Override
    public String toString() {
        return code;
    }
}
