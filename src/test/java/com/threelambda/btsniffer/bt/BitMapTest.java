package com.threelambda.btsniffer.bt;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

/**
 * @author ym
 * @date 2019/10/29
 */
@Slf4j
public class BitMapTest {
    @Test
    public void test() {
        BitMap prefix = new BitMap(1);
        assert prefix.toString().equals("0");
        log.info(prefix.toString());
        BitMap left = BitMap.newBitMapFrom(prefix, prefix.getSize() + 1);
        assert left.toString().equals("00");
        BitMap right = BitMap.newBitMapFrom(prefix, prefix.getSize() + 1);
        right.set(prefix.getSize());
        assert right.toString().equals("01");
    }
}
