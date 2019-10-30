package com.threelambda.btsearch.bt;

import com.google.common.base.Charsets;
import com.threelambda.btsearch.bt.exception.BtSearchException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Created by ym on 2019-04-22
 */
public class Util {

    private static final Logger logger = LoggerFactory.getLogger(Util.class);

    public static Map<String, Object> decode(ByteBuf buf) throws BtSearchException {
        BDecode bDecode = new BDecode(buf);
        return bDecode.parse();
    }

    public static Map<String, Object> decode(byte[] arr) {
        BDecode bDecode = new BDecode(Unpooled.copiedBuffer(arr));
        return bDecode.parse();
    }

    public static void encode(ByteBuf buf, Map<String, Object> map) {
        BEncode.encodeDic(buf, map);
    }

    public static byte[] encode(Map<String, Object> map) {
        return BEncode.encodeToBin(map);
    }


    public static ByteBuf getHandshake(String infoHashHex) {
        ByteBuf handshake = Unpooled.buffer()
                .writeByte(19)
                .writeBytes("BitTorrent protocol".getBytes())
                .writeByte((byte) 0x00)
                .writeByte((byte) 0x00)
                .writeByte((byte) 0x00)
                .writeByte((byte) 0x00)
                .writeByte((byte) 0x00)
                .writeByte((byte) 0x10)
                .writeByte((byte) 0x00)
                .writeByte((byte) 0x01)
                .writeBytes(ByteBufUtil.decodeHexDump(infoHashHex))
                .writeBytes(Util.createPeerId().getBytes());
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
        ByteBuf buf = Unpooled.buffer()
                .writeInt(bytes.length + 2)
                .writeByte((byte) 20)
                .writeByte(extId)
                .writeBytes(bytes);
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

    public static String createPeerId() {
        return getAlphaNumericString(20);
    }

    public static String randomString(int n) {
        return getAlphaNumericString(n);
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

    public static String getAddr(byte[] addr) {
        return String.format("%d.%d.%d.%d", addr[0] & 0xFF, addr[1] & 0xFF, addr[2] & 0xFF, addr[3] & 0xFF);
    }

    public static Integer getPort(byte[] port) {
        return port[0] << 8 | (port[1] & 0xFF);
    }


    /**
     * 如果prefix = 0101
     * 那么childId = 0101... (160 bit)
     *
     * @param prefix 前缀
     * @return
     */
    public static BitMap randomChildId(BitMap prefix) {
        String peerId = Util.createPeerId();
        byte[] data = peerId.getBytes(Charset.forName("ISO-8859-1"));
        //设置data与prefix的相同
        byte[] prefixData = prefix.getData();
        int prefixSize = prefix.getSize();
        int div = prefixSize / 8;
        int mod = prefixSize % 8;
        for (int i = 0; i < div; i++) {
            data[i] = prefixData[i];
        }
        BitMap id = new BitMap(data.length * 8);
        id.setData(data);
        for (int i = div * 8; i < div * 8 + mod; i++) {
            if (prefix.bit(i) > 0) {
                id.set(i);
            } else {
                id.unset(i);
            }
        }
        return id;
    }

    /**
     * 与stringDecodeToInt可以相互转化
     *
     * @param v
     * @return
     */
    public static String intEncodeToString(Integer v) {
        ByteBuf buf = Unpooled.buffer(4);
        buf.writeInt(v);
        return new String(buf.array(), Charsets.ISO_8859_1);
    }

    /**
     * 与intEncodeToString可以相互转化
     *
     * @param intEncodeString
     * @return
     */
    public static Optional<Integer> stringDecodeToInt(String intEncodeString) {
        try {
            byte[] bytes = intEncodeString.getBytes(Charsets.ISO_8859_1);
            ByteBuf byteBuf = Unpooled.copiedBuffer(bytes);
            int val = byteBuf.readInt();
            byteBuf.release();
            return Optional.of(val);
        } catch (Exception e) {
            logger.warn("error|" + e.getMessage());
            return Optional.empty();
        }
    }


    public static String toString(byte[] bytes) {
        return new String(bytes, Charsets.ISO_8859_1);
    }

    public static byte[] getBytes(String s) {
        return s.getBytes(Charsets.ISO_8859_1);
    }

    public static String hex(String s){
        return ByteBufUtil.hexDump(getBytes(s));
    }

    public static void main(String[] args) {
        BitMap prefix = new BitMap(4);
        prefix.set(1);
        prefix.set(3);
        System.out.println(prefix.toString());
        BitMap randomChildId = Util.randomChildId(prefix);
        System.out.println(randomChildId.toString());
    }
}
