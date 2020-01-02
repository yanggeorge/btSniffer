package com.threelambda.btsniffer.bt;

import com.google.common.base.Charsets;
import com.threelambda.btsniffer.bt.exception.BtSnifferException;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

/**
 * Created by ym on 2019-04-27
 */
@Slf4j
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
            throw new BtSnifferException("out of range");
        }

        int div = index / 8;
        int mod = index % 8;
        return (data[div] & (1 << (7 - mod))) >> (7 - mod);
    }

    public BitMap set(int index) {
        if (index >= this.size) {
            throw new BtSnifferException("out of range");
        }
        int div = index / 8;
        int mod = index % 8;
        int shift = 1 << (7 - mod);
        data[div] |= shift;
        return this;
    }


    public void unset(int index) {
        if (index >= this.size) {
            throw new BtSnifferException("out of range");
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

    public static BitMap fromRawString(String s) {
        return fromBytes(s.getBytes(Charsets.ISO_8859_1));
    }

    /**
     * @return "1001101001..."
     */
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

    /**
     * @param s  "1001101001..."
     * @return
     */
    public static BitMap fromString(String s) {
        try {
            BitMap bitMap = new BitMap(s.length());
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if(c == '1'){
                    bitMap.set(i);
                }
            }
            return bitMap;
        } catch (Exception e) {
        }
        return null;

    }

    public String rawString() {
        return new String(this.data, Charsets.ISO_8859_1);
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int getSize() {
        return size;
    }

    public int compare(BitMap other, int prefixLen) {
        if (prefixLen > this.size || prefixLen > other.size) {
            throw new BtSnifferException("index out of range");
        }

        int div = prefixLen / 8;
        int mod = prefixLen % 8;

        for (int i = 0; i < div; i++) {
            int a = Byte.toUnsignedInt(this.data[i]);
            int b = Byte.toUnsignedInt(other.data[i]);
            if (a > b) {
                return 1;
            } else if (a < b) {
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
        try {
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
        } catch (ArrayIndexOutOfBoundsException e) {
            log.error("ArrayIndexOutOfBoundsException|this.id={}|this.size={}|other.id={}|other.size={}", this.toString(),this.size
                    , other.toString(), other.size);
            throw e;
        }
    }

    public static BitMap newBitMapFrom(BitMap other, int size) {
        BitMap bitMap = new BitMap(size);
        if (size > other.getSize()) {
            size = other.getSize();
        }

        int div = size / 8;
        if (div >= 0) System.arraycopy(other.data, 0, bitMap.data, 0, div);

        for (int i = div * 8; i < size; i++) {
            if (other.bit(i) == 1) {
                bitMap.set(i);
            }
        }
        return bitMap;
    }

    public static Integer getCommonPrefixLength(BitMap a, BitMap b) {

        int size = a.getSize();
        if (size > b.getSize()) {
            size = b.getSize();
        }

        int i = 0;
        while (i < size && a.bit(i) == b.bit(i)) i++;

        return i;
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

        System.out.println("xor----");
        System.out.println(bitMap.toString());
        System.out.println(bitMap2.toString());
        System.out.println(bitMap.xor(bitMap2).toString());
        System.out.println();
        byte[] data = new byte[4];
        data[0] = 0;
        data[1] = (byte) 255;
        data[2] = 0;
        data[3] = (byte) 255;
        BitMap bitMap3 = BitMap.fromBytes(data);
        System.out.println(bitMap3.toString());

        BitMap id1 = new BitMap(160).set(0).set(2).set(3);
        BitMap id2 = new BitMap(160).set(1).set(2).set(3);
        System.out.println("----");
        System.out.println(id1);
        System.out.println(id2);
        assert  id1.compare(id2, 160) == 1;
        System.out.println("----");

        System.out.println(BitMap.fromString("0101").toString());
    }


}
