package com.threelambda.btsearch;

import com.threelambda.btsearch.hello.Customer;
import com.threelambda.btsearch.hello.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class BtsearchApplication {

	private static final Logger log = LoggerFactory.getLogger(BtsearchApplication.class);


	public static void main(String[] args) {
		SpringApplication.run(BtsearchApplication.class, args);
	}

}
