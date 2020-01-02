package com.threelambda.btsniffer.bt;


import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author ym
 * @date 2019/10/14
 */
@Slf4j
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

    public static KRpcType getByCode(String code) {
        try {
            Preconditions.checkNotNull(code, "code was null");
            return KRpcType.valueOf(code.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("{}", e.getMessage());
        }
        return null;
    }

    @Override
    public String toString() {
        return code;
    }

    public static void main(String[] args) {
        System.out.println(KRpcType.valueOf("PING"));
    }
}
