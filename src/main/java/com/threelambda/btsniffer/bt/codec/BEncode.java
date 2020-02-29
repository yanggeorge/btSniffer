package com.threelambda.btsniffer.bt.codec;

import com.threelambda.btsniffer.bt.util.Util;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.threelambda.btsniffer.bt.util.Util.getBytes;

/**
 * Created by ym on 2019-04-19
 */
public class BEncode {

    public static byte[] encodeToBin(Map<String, Object> map) {
        ByteBuf buf = Unpooled.buffer();
        encodeDic(buf, map);
        byte[] arr = new byte[buf.readableBytes()];
        buf.getBytes(buf.readerIndex(), arr);
        buf.release();
        return arr;
    }

    public static void encodeDic(ByteBuf buf, Map<String, Object> map) {
        buf.writeByte('d');

        List<String> keys = new ArrayList<>(map.keySet());
        Collections.sort(keys);

        for (String key : keys) {
            if("info_hash".equalsIgnoreCase(key)){
                continue;
            }
            BEncode.encodeString(buf, key);
            Object val = map.get(key);
            if("pieces".equalsIgnoreCase(key)){
                val = Util.toString(ByteBufUtil.decodeHexDump((String)val));
            }
            if("creation date".equalsIgnoreCase(key)){
                val = DateTime.parse((String) val, DateTimeFormat.forPattern("yyyy-MM-dd HH:hh:ss")).getMillis()/1000;
            }
            BEncode.encodeElement(buf, val);
        }

        buf.writeByte('e');
    }


    private static void encodeString(ByteBuf buf, String key) {
        byte[] bytes = getBytes(key);
        int length = bytes.length;
        buf.writeBytes(getBytes(String.valueOf(length)));
        buf.writeByte(':');
        buf.writeBytes(bytes);
    }

    private static void encodeElement(ByteBuf buf, Object o) {
        if (o instanceof String) {
            BEncode.encodeString(buf, (String) o);
        } else if (o instanceof Integer) {
            BEncode.encodeInt(buf, (Integer) o);
        } else if (o instanceof Long){
            BEncode.encodeLong(buf, (Long) o);
        } else if (o instanceof List) {
            BEncode.encodeList(buf, (List) o);
        } else if (o instanceof Map) {
            BEncode.encodeDic(buf, (Map<String, Object>) o);
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
        buf.writeBytes(getBytes(o.toString()));
        buf.writeByte('e');
    }

    private static void encodeLong(ByteBuf buf, Long o) {
        buf.writeByte('i');
        buf.writeBytes(getBytes(o.toString()));
        buf.writeByte('e');
    }

}
