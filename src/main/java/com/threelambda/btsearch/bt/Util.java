package com.threelambda.btsearch.bt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ym on 2019-04-22
 */
public class Util {

    private static final Logger logger = LoggerFactory.getLogger(Util.class);

    public static Map<String, Object> parse(ByteBuf buf) {
        BDecode bDecode = new BDecode(buf);
        return bDecode.parse();
    }

    public static Map<String, Object> parse(byte[] arr) {
        BDecode bDecode = new BDecode(Unpooled.copiedBuffer(arr));
        return bDecode.parse();
    }

    public static void encode(ByteBuf buf, Map<String, Object> map) {
        BEncode.encodeToDic(buf, map);
    }

    public static byte[] encode(Map<String, Object> map) {
        return BEncode.encodeToBin(map);
    }


    public static ByteBuf getHandshake(String infoHash) {
        ByteBuf handshake = Unpooled.buffer();
        handshake.writeByte(19);
        handshake.writeBytes("BitTorrent protocol".getBytes());
        handshake.writeByte((byte) 0x00);
        handshake.writeByte((byte) 0x00);
        handshake.writeByte((byte) 0x00);
        handshake.writeByte((byte) 0x00);
        handshake.writeByte((byte) 0x00);
        handshake.writeByte((byte) 0x10);
        handshake.writeByte((byte) 0x00);
        handshake.writeByte((byte) 0x01);
        handshake.writeBytes(ByteBufUtil.decodeHexDump(infoHash));
        handshake.writeBytes(Util.getPeerId().getBytes());
        return handshake;
    }

    public static ByteBuf getExtHandshake() {
        Map<String, Object> dic = new HashMap<>();
        Map<String, Object> metaDic = new HashMap<>();
        dic.put("m", metaDic);
        metaDic.put("ut_metadata", 1);
        byte[] bytes = Util.encode(dic);
        return pack((byte) 0, bytes);
    }

    public static ByteBuf getMetadataPieceRequest(int piece, int extId) {
        Map<String, Object> dic = new HashMap<>();
        dic.put("msg_type", 0);
        dic.put("piece", piece);
        byte[] bytes = Util.encode(dic);
        return pack((byte) extId, bytes);
    }

    private static ByteBuf pack(byte extId, byte[] bytes) {
        ByteBuf buf = Unpooled.buffer();
        buf.writeInt(bytes.length + 2);
        buf.writeByte((byte) 20);
        buf.writeByte(extId);
        buf.writeBytes(bytes);
        return buf;
    }


    /**
     * 获取outbound ip ，not work on mac
     *
     * @return
     */
    public static String getCurrentIp() {
        String ip = null;
        try (final DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 80);
            ip = socket.getLocalAddress().getHostAddress();
        } catch (Exception e) {
            logger.error("", e);
        }
        return ip;
    }


    public static String getCurrentIpOnMac() {
        String interfaceName = "en0";
        String ip = null;
        try {
            Enumeration e = NetworkInterface.getNetworkInterfaces();
            while (e.hasMoreElements()) {
                NetworkInterface n = (NetworkInterface) e.nextElement();
                if (interfaceName.equals(n.getName())) {
                    Enumeration ee = n.getInetAddresses();
                    while (ee.hasMoreElements()) {
                        InetAddress i = (InetAddress) ee.nextElement();
                        if (i instanceof Inet4Address) {
                            ip = i.getHostAddress();
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("", e);
        }
        return ip;
    }

    public static String getPeerId() {
        return getAlphaNumericString(20);
    }

    private static String getAlphaNumericString(int n) {

        // chose a Character random from this String
        String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                + "0123456789"
                + "abcdefghijklmnopqrstuvxyz";

        // create StringBuffer size of AlphaNumericString
        StringBuilder sb = new StringBuilder(n);

        for (int i = 0; i < n; i++) {

            // generate a random number between
            // 0 to AlphaNumericString variable READ_MSG_LENGTH
            int index
                    = (int) (AlphaNumericString.length()
                    * Math.random());

            // add Character one by one in end of sb
            sb.append(AlphaNumericString
                    .charAt(index));
        }

        return sb.toString();
    }

}
