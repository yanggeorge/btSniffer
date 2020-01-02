package com.threelambda.btsearch;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.threelambda.btsearch.bt.BitMap;
import com.threelambda.btsearch.bt.Node;
import com.threelambda.btsearch.bt.RoutingTable;
import com.threelambda.btsearch.bt.debug.DebugInfo;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class BtSnifferApplicationTests {
	@Autowired
	private RoutingTable routingTable;

	@Test
	public void contextLoads() {
		List<String> list = Lists.newLinkedList();
	}

	@Test
	public void test(){
		log.info("routingTable.size={}", routingTable.size());
	}

}
