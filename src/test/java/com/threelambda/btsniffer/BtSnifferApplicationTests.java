package com.threelambda.btsniffer;

import com.google.common.collect.Lists;
import com.threelambda.btsniffer.bt.RoutingTable;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

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
