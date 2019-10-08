package com.threelambda.btsearch.bt;

import io.netty.buffer.ByteBufUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * @author ym
 * @date 2019/10/30
 */
@Slf4j
public class BDecodeTest {
    @Test
    public void test(){
        String rawHex = "64 31 3a 61 64 32 3a 69 64 32 30 3a 3b ac e4 eb\n" +
                "b3 a6 db 3c 87 0c 3e 99 24 5e 0d 1c 06 b7 47 bb\n" +
                "31 32 3a 69 6d 70 6c 69 65 64 5f 70 6f 72 74 69\n" +
                "31 65 39 3a 69 6e 66 6f 5f 68 61 73 68 32 30 3a\n" +
                "4a 02 1d dd 65 11 e7 5d 12 97 cc b4 88 5a 83 c5\n" +
                "4b 14 7d c4 34 3a 6e 61 6d 65 33 33 3a 43 7a 65\n" +
                "63 68 20 66 69 72 73 74 20 76 69 64 65 6f 20 37\n" +
                "20 6d 6f 6e 69 6b 61 20 50 49 52 40 54 45 34 3a\n" +
                "70 6f 72 74 69 35 34 38 34 37 65 35 3a 74 6f 6b\n" +
                "65 6e 35 3a 41 48 68 50 6e 65 31 3a 71 31 33 3a\n" +
                "61 6e 6e 6f 75 6e 63 65 5f 70 65 65 72 31 3a 74\n" +
                "34 3a c1 43 00 00 31 3a 76 34 3a 55 54 b1 00 31\n" +
                "3a 79 31 3a 71 65";
        String hexDump = StringUtils.deleteAny(rawHex, " \n");
        byte[] bytes = ByteBufUtil.decodeHexDump(hexDump);
        Map<String, Object> map = Util.decode(bytes);
        assert ((Map<String, Object>) map.get("a")).get("port") instanceof  Long ;
    }
}
