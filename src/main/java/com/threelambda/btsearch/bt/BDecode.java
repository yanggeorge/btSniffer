package com.threelambda.btsearch.bt;

import com.google.common.io.Files;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.commons.codec.digest.DigestUtils;
import org.joda.time.DateTime;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 解析bencoding
 * Created by ym on 2019-04-19
 */

class BDecode {
    private ByteBuf buf;
    private int i;
    private int n;

    public static void main(String[] args) throws Exception {
        ByteBuf buf = Unpooled.copiedBuffer("d4:spaml1:a1:bee".getBytes());
        BDecode bDecode = new BDecode(buf);
        Map<String, Object> parse = bDecode.parse();
        System.out.println(parse);

        String path = "/Users/ym/tmp/venom.torrent";
        buf = Unpooled.copiedBuffer(Files.toByteArray(new File(path)));
        bDecode = new BDecode(buf);
        parse = bDecode.parse();
        System.out.println(parse);
    }

    BDecode(ByteBuf buf) {
        this.buf = buf;
        this.i = 0;
        this.n = buf.readableBytes();
    }

    public Map<String, Object> parse() {
        return this.dic();
    }

    private Map<String, Object> dic() {
        byte c = this.peek();
        if (c != 'd') {
            throw new RuntimeException("error");
        }
        this.next();
        HashMap<String, Object> dic = new HashMap<String, Object>();
        String key = "";
        Object val = null;
        c = this.peek();

        int infoStart = 0, infoEnd = 0;
        while (c != 'e') {
            key = this.string();
            if ("info".equalsIgnoreCase(key)) {
                infoStart = this.i;
            }

            val = this.element();

            if ("info".equalsIgnoreCase(key)) {
                infoEnd = this.i;
                byte[] bytes = new byte[infoEnd - infoStart];
                this.buf.getBytes(infoStart, bytes);
                String infoHash = DigestUtils.sha1Hex(bytes);
                dic.put("info_hash", infoHash);
            }

            //暂时不保存。
            if ("pieces".equals(key)) {
                val = "...";
            }

            if ("creation date".equals(key)) {
                //因为是unix time 所以*1000
                val = new DateTime((long) val * 1000).toString("yyyy-MM-dd HH:hh:ss");
            }

            dic.put(key, val);

            c = this.peek();
        }

        c = this.next();
        if (c != 'e') {
            throw new RuntimeException("error");
        }

        return dic;
    }

    private Object element() {
        byte c = this.peek();
        if (c == 'i') {
            return this.integer();
        } else if (c == 'd') {
            return this.dic();
        } else if (c == 'l') {
            return this.list();
        } else {
            if (Character.isDigit(c)) {
                return this.string();
            }
            throw new RuntimeException("not recognized.");
        }
    }

    private Object list() {
        List<Object> lis = new ArrayList<>();

        byte c = this.next();
        if (c != 'l') {
            throw new RuntimeException("error");
        }

        c = this.peek();

        while (c != 'e') {
            Object ele = this.element();

            lis.add(ele);
            c = this.peek();
        }
        this.next();
        return lis;
    }

    private Long integer() {
        byte c = this.next();
        if (c != 'i') {
            throw new RuntimeException("error");
        }
        long num = this.num();
        c = this.next();
        if (c != 'e') {
            throw new RuntimeException("error");
        }
        return num;
    }

    private String string() {
        long num = this.num();
        byte c = this.next();
        if (c != ':') {
            throw new RuntimeException("not equal ':' ");
        }
        byte[] bytes = new byte[(int) num];
        this.buf.readBytes(bytes);
        String s = new String(bytes);
        this.i += num;
        return s;
    }

    private long num() {
        byte c = this.peek();
        long num = 0;
        while (Character.isDigit(c)) {
            num = num * 10 + c - '0';
            this.next();
            c = this.peek();
        }
        return num;
    }

    private byte peek() {
        if (this.i < this.n) {
            return this.buf.getByte(this.i);
        }
        throw new RuntimeException("out of index");
    }

    private byte next() {
        if (this.i < this.n) {
            byte c = this.buf.readByte();
            this.i += 1;
            return c;
        }
        throw new RuntimeException("out of index");
    }
}
