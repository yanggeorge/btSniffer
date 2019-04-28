package com.threelambda.btsearch.bt;

import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Created by ym on 2019-04-27
 */
public class BitMap {

    private int size;
    private byte[] data;

    BitMap(int size) {
        this.size = size;
        int div = size / 8;
        if ((size % 8) > 0) {
            div += 1;
        }
        data = new byte[div];
        for (int i = 0; i < div; i++) {
            data[i] = (byte) 0;
        }
    }

    public int bit(int index) {
        if (index >= this.size) {
            throw new RuntimeException("out of range");
        }

        int div = index / 8;
        int mod = index % 8;
        return (data[div] & (1 << (7 - mod))) >> (7 - mod);
    }

    public void set(int index) {
        if (index >= this.size) {
            throw new RuntimeException("out of range");
        }
        int div = index / 8;
        int mod = index % 8;
        int shift = 1 << (7 - mod);
        data[div] |= shift;
    }


    public void unset(int index) {
        if (index >= this.size) {
            throw new RuntimeException("out of range");
        }
        int div = index / 8;
        int mod = index % 8;
        int shift = 1 << (7 - mod);
        data[div] &= ~shift;
    }

    public static BitMap fromBytes(byte[] data) {
        BitMap bitMap = new BitMap(8 * data.length);
        bitMap.data = Arrays.copyOf(data, data.length);
        return bitMap;
    }

    public static BitMap fromString(String s) {
        return fromBytes(s.getBytes());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.size; i++) {
            if (this.bit(i) > 0) {
                sb.append(1);
            } else {
                sb.append(0);
            }
        }
        return sb.toString();
    }

    public String rawString() {
        return new String(this.data, Charset.forName("ISO-8859-1"));
    }

    public byte[] getData() {
        return data;
    }

    public int compare(BitMap other, int prefixLen) {
        if (prefixLen > this.size || prefixLen > other.size) {
            throw new RuntimeException("index out of range");
        }

        int div = prefixLen / 8;
        int mod = prefixLen % 8;

        for (int i = 0; i < div; i++) {
            if (this.data[i] > other.data[i]) {
                return 1;
            } else if (this.data[i] < other.data[i]) {
                return -1;
            }
        }

        for (int i = div * 8; i < prefixLen + mod; i++) {
            int bit1 = this.bit(i);
            int bit2 = other.bit(i);
            if (bit1 > bit2) {
                return 1;
            } else if (bit1 < bit2) {
                return -1;
            }
        }
        return 0;
    }

    public BitMap xor(BitMap other) {
        BitMap distance = new BitMap(this.size);

        int div = distance.size / 8;
        int mod = distance.size % 8;
        for (int i = 0; i < div; i++) {
            distance.data[i] = (byte) (this.data[i] ^ other.data[i]);
        }

        for (int i = 8 * div; i < 8 * div + mod; i++) {
            if ((this.bit(i) ^ other.bit(i)) > 0) {
                distance.set(i);
            }
        }
        return distance;
    }

    public static void main(String[] args) {
        BitMap bitMap = new BitMap(10);
        bitMap.set(2);
        System.out.println(bitMap.bit(2));
        System.out.println(bitMap.bit(1));
        System.out.println(bitMap.toString());
        bitMap.unset(2);
        System.out.println(bitMap.toString());
        bitMap.set(2);

        BitMap bitMap2 = new BitMap(10);
        bitMap2.set(1);
        bitMap2.set(0);
        System.out.println(bitMap2.toString());
        System.out.println(bitMap2.compare(bitMap, 10));

        System.out.println(bitMap.xor(bitMap2).toString());

        byte[] data = new byte[4];
        data[0] = 0;
        data[1] = (byte) 255;
        data[2] = 0;
        data[3] = (byte) 255;
        BitMap bitMap3 = BitMap.fromBytes(data);
        System.out.println(bitMap3.toString());
    }
}
