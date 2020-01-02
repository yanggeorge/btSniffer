package com.threelambda.btsniffer.bt.debug;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author ym
 * @date 2019/10/29
 */
@Data
public class DebugInfo implements Serializable {
    String localId;
    String insertNodeId;
    List<DebugNode> nodes;

    public static class DebugNode implements Serializable{
        String ip;
        Integer port;
        String nodeId;

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public Integer getPort() {
            return port;
        }

        public void setPort(Integer port) {
            this.port = port;
        }

        public String getNodeId() {
            return nodeId;
        }

        public void setNodeId(String nodeId) {
            this.nodeId = nodeId;
        }
    }
}
