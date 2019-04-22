package com.threelambda.btsearch.bt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

import java.util.*;

/**
 * Created by ym on 2019-04-19
 */
class BEncode {


    public static void main(String[] args) {
        Map<String, Object> map = new HashMap<>();
        map.put("ym", 12);
        List<Object> list = new ArrayList<>();
        map.put("list", list);
        list.add("abc");
        list.add(1234);
        byte[] bytes = encodeToBin(map);
        System.out.println(ByteBufUtil.hexDump(bytes));

        System.out.println(Util.parse(bytes));

    }

    public static byte[] encodeToBin(Map<String, Object> map) {
        ByteBuf buf = Unpooled.buffer();
        encodeToDic(buf, map);
        byte[] arr = new byte[buf.readableBytes()];
        buf.getBytes(buf.readerIndex(), arr);
        return arr;
    }

    public static void encodeToDic(ByteBuf buf, Map<String, Object> map) {
        buf.writeByte('d');

        List<String> keys = new ArrayList<>();
        keys.addAll(map.keySet());
        Collections.sort(keys);

        for (String key : keys) {
            BEncode.encodeToString(buf, key);
            BEncode.encodeElement(buf, map.get(key));
        }

        buf.writeByte('e');
    }


    private static void encodeToString(ByteBuf buf, String key) {
        byte[] bytes = key.getBytes();
        int length = bytes.length;
        buf.writeBytes(String.valueOf(length).getBytes());
        buf.writeByte(':');
        buf.writeBytes(bytes);
    }

    private static void encodeElement(ByteBuf buf, Object o) {
        if (o instanceof String) {
            BEncode.encodeToString(buf, (String) o);
        } else if (o instanceof Integer) {
            BEncode.encodeInt(buf, (Integer) o);
        } else if (o instanceof List) {
            BEncode.encodeList(buf, (List) o);
        } else if (o instanceof Map) {
            BEncode.encodeToDic(buf, (Map<String, Object>) o);
        }
    }

    private static void encodeList(ByteBuf buf, List list) {
        buf.writeByte('l');
        for (Object o : list) {
            BEncode.encodeElement(buf, o);
        }
        buf.writeByte('e');
    }

    private static void encodeInt(ByteBuf buf, Integer o) {
        buf.writeByte('i');
        buf.writeBytes(o.toString().getBytes());
        buf.writeByte('e');
    }


}
