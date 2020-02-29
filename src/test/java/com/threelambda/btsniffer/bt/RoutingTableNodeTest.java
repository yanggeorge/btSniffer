package com.threelambda.btsniffer.bt;

import com.threelambda.btsniffer.bt.util.BitMap;
import com.threelambda.btsniffer.bt.routingtable.RoutingTableNode;
import org.junit.Test;

/**
 * @author ym
 */
public class RoutingTableNodeTest {

    @Test
    public void test(){
        BitMap prefix = new BitMap(2);
        prefix.set(0);
        System.out.println(prefix.toString());
        RoutingTableNode node = new RoutingTableNode(prefix);
        System.out.println(node.child(0));
        System.out.println(node);
    }
}
