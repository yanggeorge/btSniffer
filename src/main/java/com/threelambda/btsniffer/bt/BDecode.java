package com.threelambda.btsniffer.bt;

import com.google.common.base.Charsets;
import com.threelambda.btsniffer.bt.exception.BtSnifferException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.joda.time.DateTime;

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
            throw new BtSnifferException("error");
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

            //对pieces的值: string -> bytes -> hex string
            //相应的编码的时候: hex string -> bytes -> string
            if ("pieces".equals(key)) {
                val = ByteBufUtil.hexDump(Util.getBytes((String) val));
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
            throw new BtSnifferException("error");
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
            throw new BtSnifferException("not recognized.");
        }
    }

    private Object list() {
        List<Object> lis = new ArrayList<>();

        byte c = this.next();
        if (c != 'l') {
            throw new BtSnifferException("error");
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
            throw new BtSnifferException("error");
        }
        long num = this.num();
        c = this.next();
        if (c != 'e') {
            throw new BtSnifferException("error");
        }
        return num;
    }

    private String string() {
        long num = this.num();
        byte c = this.next();
        if (c != ':') {
            throw new BtSnifferException("not equal ':' ");
        }
        byte[] bytes = new byte[(int) num];
        this.buf.readBytes(bytes);
        //String s = new String(bytes) ; 默认使用的可能是UTF-8编码
        //使用ISO-8859-1则在java下保证可以bytes->string->bytes可以还原
        String s = new String(bytes, Charsets.ISO_8859_1);
        this.i += num;
        return s;
    }

    private long num() {
        byte c = this.peek();
        boolean negative = false;
        if(c == '-'){
            negative = true;
            next();
            c = this.peek();
        }
        long num = 0;
        while (Character.isDigit(c)) {
            num = num * 10 + c - '0';
            this.next();
            c = this.peek();
        }
        if(negative) {
            return -num;
        }
        return num;
    }

    private byte peek() {
        if (this.i < this.n) {
            return this.buf.getByte(this.i);
        }
        throw new BtSnifferException("out of index");
    }

    private byte next() {
        if (this.i < this.n) {
            byte c = this.buf.readByte();
            this.i += 1;
            return c;
        }
        throw new BtSnifferException("out of index");
    }
}
