package com.threelambda.btsearch.bt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.Map;

/**
 * Created by ym on 2019-04-22
 */
public class Util {

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
}
